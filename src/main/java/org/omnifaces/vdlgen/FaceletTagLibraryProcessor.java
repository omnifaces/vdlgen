/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.vdlgen;

import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.VOID;

import java.beans.Introspector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.validator.FacesValidator;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.jaxb.metadata.MetadataSourceAdapter;
import org.eclipse.persistence.jaxb.xmlmodel.XmlBindings;
import org.eclipse.persistence.jaxb.xmlmodel.XmlNsForm;
import org.eclipse.persistence.jaxb.xmlmodel.XmlSchema;
import org.omnifaces.vdl.FaceletTagLibrary;
import org.omnifaces.vdl.FaceletTagLibrary.Version;
import org.omnifaces.vdl.FacesAttribute;
import org.omnifaces.vdl.FacesComponentConfig;
import org.omnifaces.vdl.FacesFunction;
import org.omnifaces.vdl.FacesFunctions;
import org.omnifaces.vdl.FacesBehaviorTag;
import org.omnifaces.vdl.FacesConverterTag;
import org.omnifaces.vdl.FacesTagHandler;
import org.omnifaces.vdl.FacesValidatorTag;
import org.omnifaces.vdlgen.FaceletTaglib.Tag;

/**
 * Annotation processor that generates {@code *.taglib.xml} files from annotated source code.
 * <p>
 * See {@link FaceletTagLibrary} for usage instructions.
 * <p>
 * Implementation notice: we explicitly use Eclipse MOXy as JAXB implementation because it is so far the only JAXB
 * implementation with proper built-in support for generating CDATA-flavored entries. This is primarily used for
 * {@code <description>} entries which are basically copied from the javadocs.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FaceletTagLibrary
 */
public class FaceletTagLibraryProcessor extends AbstractProcessor {

	private static final String SETTER_PREFIX = "set";
	private static final int SETTER_PREFIX_LENGTH = SETTER_PREFIX.length();
	private static final String[] SETTER_JAVADOC_PREFIXES = { "Sets ", "Set " };

	private static final Pattern JAVADOC_BLOCK_TAG = Pattern.compile("^\\s*@\\w+", Pattern.MULTILINE);
	private static final Pattern INLINE_LINK = Pattern.compile("\\{@link(?:plain)?\\s+([^}]+)\\}");
	private static final Pattern INLINE_CODE = Pattern.compile("\\{@code\\s+([^}]+)\\}");
	private static final Pattern INLINE_LITERAL = Pattern.compile("\\{@literal\\s+([^}]+)\\}");
	private static final Pattern INLINE_OTHER = Pattern.compile("\\{@\\w+(?:\\s+[^}]*)?\\}");

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(
			FaceletTagLibrary.class.getCanonicalName(),
			FaceletTagLibrary.List.class.getCanonicalName(),
			FacesBehaviorTag.class.getCanonicalName(),
			FacesConverterTag.class.getCanonicalName(),
			FacesValidatorTag.class.getCanonicalName(),
			FacesTagHandler.class.getCanonicalName(),
			FacesComponentConfig.class.getCanonicalName(),
			FacesAttribute.class.getCanonicalName(),
			FacesFunction.class.getCanonicalName(),
			FacesFunctions.class.getCanonicalName()
		);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_17;
	}

	private TaglibDescriptionReader taglibReader;

	private TaglibDescriptionReader getTaglibReader() {
		if (taglibReader == null) {
			taglibReader = new TaglibDescriptionReader(getClass().getClassLoader());
		}

		return taglibReader;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
		if (annotations.isEmpty()) {
			return false;
		}

		var taglibs = collectFaceletTaglibs(env);
		validateAnnotations(env);

		if (!taglibs.isEmpty()) {
			collectComponents(env, taglibs);
			collectConvertersAndValidators(env, taglibs);
			collectBehaviors(env, taglibs);
			collectTagHandlers(env, taglibs);
			collectFunctions(env, taglibs);
			generateTaglibXmlFiles(taglibs);
		}

		return true;
	}

	// Tag library collection ------------------------------------------------------------------------------------------

	private static Map<String, FaceletTaglib> collectFaceletTaglibs(RoundEnvironment env) {
		SortedMap<String, FaceletTaglib> taglibs = new TreeMap<>();

		Consumer<FaceletTagLibrary[]> add = libraries -> {
			for (var library : libraries) {
				var shortName = library.shortName().isEmpty() ? library.id() : library.shortName();
				var taglib = new FaceletTaglib(library.version(), library.description(), library.namespace(), library.id(), shortName);

				if (taglibs.put(library.namespace(), taglib) != null) {
					throw new IllegalStateException("Duplicate @FaceletTagLibrary namespace: " + library.namespace());
				}
			}
		};

		for (var element : env.getElementsAnnotatedWith(FaceletTagLibrary.class)) {
			add.accept(element.getAnnotationsByType(FaceletTagLibrary.class));
		}

		for (var element : env.getElementsAnnotatedWith(FaceletTagLibrary.List.class)) {
			for (var list : element.getAnnotationsByType(FaceletTagLibrary.List.class)) {
				add.accept(list.value());
			}
		}

		return taglibs;
	}

	private void collectComponents(RoundEnvironment env, Map<String, FaceletTaglib> taglibs) {
		for (var element : env.getElementsAnnotatedWith(FacesComponent.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				continue;
			}

			var typeElement = (TypeElement) element;
			var facesComponent = typeElement.getAnnotation(FacesComponent.class);
			var namespace = facesComponent.namespace();

			if (namespace.isEmpty()) {
				continue;
			}

			var taglib = taglibs.get(namespace);

			if (taglib == null) {
				continue;
			}

			var description = processJavadoc(processingEnv.getElementUtils().getDocComment(typeElement));
			var tagName = facesComponent.tagName().isEmpty()
				? Introspector.decapitalize(typeElement.getSimpleName().toString())
				: facesComponent.tagName();
			var componentType = facesComponent.value().isEmpty()
				? typeElement.getQualifiedName().toString()
				: facesComponent.value();
			var handlerClass = getComponentHandlerClass(typeElement);
			var rendererType = findRendererType(typeElement);

			var tag = taglib.addComponent(description, tagName, componentType, handlerClass, rendererType);
			collectAttributes(tag, typeElement, collectParentComponentTypes(typeElement));
		}
	}

	private void collectConvertersAndValidators(RoundEnvironment env, Map<String, FaceletTaglib> taglibs) {
		for (var element : env.getElementsAnnotatedWith(FacesConverterTag.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				error("@FacesConverterTag is only supported on a class", element);
				continue;
			}

			var typeElement = (TypeElement) element;
			var facesConverterTag = typeElement.getAnnotation(FacesConverterTag.class);
			var taglib = getTaglib(taglibs, facesConverterTag.namespace(), element);

			if (taglib == null) {
				continue;
			}

			var facesConverter = typeElement.getAnnotation(FacesConverter.class);

			if (facesConverter == null) {
				error("@FacesConverterTag is only supported on a @FacesConverter class; '"
					+ typeElement.getQualifiedName() + "' is not annotated with @FacesConverter", element);
				continue;
			}

			var converterId = facesConverter.value();

			if (converterId.isEmpty()) {
				error("@FacesConverterTag on a @FacesConverter requires a non-empty converter ID in @FacesConverter.value()", element);
				continue;
			}

			var description = processJavadoc(processingEnv.getElementUtils().getDocComment(typeElement));
			var tagName = facesConverterTag.tagName().isEmpty()
				? Introspector.decapitalize(typeElement.getSimpleName().toString())
				: facesConverterTag.tagName();
			var handlerClass = extractHandlerClass(
				() -> facesConverterTag.handlerClass(), "jakarta.faces.view.facelets.ConverterHandler");

			var tag = taglib.addConverter(description, tagName, converterId, handlerClass);
			collectAttributes(tag, typeElement, collectParentConverterIds(typeElement));
		}

		for (var element : env.getElementsAnnotatedWith(FacesValidatorTag.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				error("@FacesValidatorTag is only supported on a class", element);
				continue;
			}

			var typeElement = (TypeElement) element;
			var facesValidatorTag = typeElement.getAnnotation(FacesValidatorTag.class);
			var taglib = getTaglib(taglibs, facesValidatorTag.namespace(), element);

			if (taglib == null) {
				continue;
			}

			var facesValidator = typeElement.getAnnotation(FacesValidator.class);

			if (facesValidator == null) {
				error("@FacesValidatorTag is only supported on a @FacesValidator class; '"
					+ typeElement.getQualifiedName() + "' is not annotated with @FacesValidator", element);
				continue;
			}

			var validatorId = facesValidator.value();

			if (validatorId.isEmpty()) {
				error("@FacesValidatorTag on a @FacesValidator requires a non-empty validator ID in @FacesValidator.value()", element);
				continue;
			}

			var description = processJavadoc(processingEnv.getElementUtils().getDocComment(typeElement));
			var tagName = facesValidatorTag.tagName().isEmpty()
				? Introspector.decapitalize(typeElement.getSimpleName().toString())
				: facesValidatorTag.tagName();
			var handlerClass = extractHandlerClass(
				() -> facesValidatorTag.handlerClass(), "jakarta.faces.view.facelets.ValidatorHandler");

			var tag = taglib.addValidator(description, tagName, validatorId, handlerClass);
			collectAttributes(tag, typeElement, collectParentValidatorIds(typeElement));
		}
	}

	private void collectBehaviors(RoundEnvironment env, Map<String, FaceletTaglib> taglibs) {
		for (var element : env.getElementsAnnotatedWith(FacesBehaviorTag.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				error("@FacesBehaviorTag is only supported on a class", element);
				continue;
			}

			var typeElement = (TypeElement) element;
			var facesBehaviorTag = typeElement.getAnnotation(FacesBehaviorTag.class);
			var taglib = getTaglib(taglibs, facesBehaviorTag.namespace(), element);

			if (taglib == null) {
				continue;
			}

			var facesBehavior = typeElement.getAnnotation(FacesBehavior.class);

			if (facesBehavior == null) {
				error("@FacesBehaviorTag is only supported on a @FacesBehavior class; '"
					+ typeElement.getQualifiedName() + "' is not annotated with @FacesBehavior", element);
				continue;
			}

			var behaviorId = facesBehavior.value();

			if (behaviorId.isEmpty()) {
				error("@FacesBehaviorTag on a @FacesBehavior requires a non-empty behavior ID in @FacesBehavior.value()", element);
				continue;
			}

			var description = processJavadoc(processingEnv.getElementUtils().getDocComment(typeElement));
			var tagName = facesBehaviorTag.tagName().isEmpty()
				? Introspector.decapitalize(typeElement.getSimpleName().toString())
				: facesBehaviorTag.tagName();
			var handlerClass = extractHandlerClass(
				() -> facesBehaviorTag.handlerClass(), "jakarta.faces.view.facelets.BehaviorHandler");

			var tag = taglib.addBehavior(description, tagName, behaviorId, handlerClass);
			collectAttributes(tag, typeElement, List.of());
		}
	}

	private void collectTagHandlers(RoundEnvironment env, Map<String, FaceletTaglib> taglibs) {
		for (var element : env.getElementsAnnotatedWith(FacesTagHandler.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				error("@FacesTagHandler is only supported on a class", element);
				continue;
			}

			var typeElement = (TypeElement) element;
			var facesTagHandler = typeElement.getAnnotation(FacesTagHandler.class);
			var taglib = getTaglib(taglibs, facesTagHandler.namespace(), element);

			if (taglib == null) {
				continue;
			}

			var description = processJavadoc(processingEnv.getElementUtils().getDocComment(typeElement));
			var tagName = facesTagHandler.tagName().isEmpty()
				? Introspector.decapitalize(typeElement.getSimpleName().toString())
				: facesTagHandler.tagName();
			var handlerClassFqn = typeElement.getQualifiedName().toString();
			var converterId = facesTagHandler.converterId();
			var validatorId = facesTagHandler.validatorId();

			if (!converterId.isEmpty() && !validatorId.isEmpty()) {
				error("@FacesTagHandler converterId and validatorId are mutually exclusive", element);
				continue;
			}

			Tag tag;

			if (!converterId.isEmpty()) {
				tag = taglib.addConverter(description, tagName, converterId, handlerClassFqn);
			}
			else if (!validatorId.isEmpty()) {
				tag = taglib.addValidator(description, tagName, validatorId, handlerClassFqn);
			}
			else {
				tag = taglib.addTagHandler(description, tagName, handlerClassFqn);
			}

			collectTagHandlerAttributes(tag, typeElement);
		}
	}

	private void collectFunctions(RoundEnvironment env, Map<String, FaceletTaglib> taglibs) {
		for (var element : env.getElementsAnnotatedWith(FacesFunctions.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				error("@FacesFunctions is only supported on a class", element);
				continue;
			}

			var typeElement = (TypeElement) element;
			var facesFunctions = typeElement.getAnnotation(FacesFunctions.class);
			var taglib = getTaglib(taglibs, facesFunctions.namespace(), element);

			if (taglib == null) {
				continue;
			}

			var functionClass = typeElement.getQualifiedName().toString();

			for (var enclosed : typeElement.getEnclosedElements()) {
				if (enclosed.getKind() == METHOD) {
					var method = (ExecutableElement) enclosed;
					var modifiers = method.getModifiers();

					if (modifiers.contains(PUBLIC) && modifiers.contains(STATIC) && method.getReturnType().getKind() != VOID) {
						var facesFunction = method.getAnnotation(FacesFunction.class);
						var functionName = (facesFunction != null && !facesFunction.name().isEmpty())
							? facesFunction.name()
							: method.getSimpleName().toString();
						addFunction(taglib, functionClass, functionName, method);
					}
				}
			}
		}

		for (var element : env.getElementsAnnotatedWith(FacesFunction.class)) {
			var method = (ExecutableElement) element;

			if (method.getEnclosingElement().getAnnotation(FacesFunctions.class) != null) {
				continue;
			}

			var modifiers = method.getModifiers();

			if (!modifiers.contains(PUBLIC) || !modifiers.contains(STATIC) || method.getReturnType().getKind() == VOID) {
				error("@FacesFunction is only supported on public static non-void methods; '" + method + "' does not meet these requirements", element);
				continue;
			}

			var facesFunction = method.getAnnotation(FacesFunction.class);
			var taglib = getTaglib(taglibs, facesFunction.namespace(), element);

			if (taglib == null) {
				continue;
			}

			var functionClass = ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString();
			var functionName = facesFunction.name().isEmpty() ? method.getSimpleName().toString() : facesFunction.name();
			addFunction(taglib, functionClass, functionName, method);
		}
	}

	private void addFunction(FaceletTaglib taglib, String functionClass, String functionName, ExecutableElement method) {
		var description = processJavadoc(processingEnv.getElementUtils().getDocComment(method));
		var returnType = getErasedTypeName(method.getReturnType());
		var params = method.getParameters().stream()
			.map(VariableElement::asType)
			.map(this::getErasedTypeName)
			.collect(Collectors.joining(", "));
		var signature = returnType + " " + functionName + "(" + params + ")";

		taglib.addFunction(description, functionName, functionClass, signature);
	}

	/**
	 * Collects attributes for component, converter, and validator tags by walking the class hierarchy for setter
	 * methods, looking up descriptions from taglib XML by parent type IDs (component types, converter IDs, or
	 * validator IDs), and adding extra attributes (binding, validator, action, etc.) from
	 * {@link StandardFacesAttributes}.
	 */
	private void collectAttributes(Tag tag, TypeElement typeElement, List<String> parentTypeIds) {
		var processedAttributes = new HashSet<String>();

		for (var member : processingEnv.getElementUtils().getAllMembers(typeElement)) {
			if (member.getKind() != METHOD) {
				continue;
			}

			var method = (ExecutableElement) member;

			if (!isSetterMethod(method)) {
				continue;
			}

			var attrName = Introspector.decapitalize(method.getSimpleName().toString().substring(SETTER_PREFIX_LENGTH));

			if (!processedAttributes.add(attrName)) {
				continue;
			}

			if (isExcludedSetter((TypeElement) method.getEnclosingElement(), attrName)) {
				continue;
			}

			var description = processSetterJavadoc(processingEnv.getElementUtils().getDocComment(method));

			if (description == null) {
				description = lookupTaglibDescription(parentTypeIds, attrName);
			}

			if (description == null) {
				description = lookupStandardDescription((TypeElement) method.getEnclosingElement(), attrName);
			}

			var facesAttribute = method.getAnnotation(FacesAttribute.class);

			if (facesAttribute != null && !facesAttribute.description().isEmpty()) {
				description = facesAttribute.description();
			}

			var required = facesAttribute != null && facesAttribute.required();
			var methodSignature = facesAttribute != null && !facesAttribute.methodSignature().isEmpty()
				? facesAttribute.methodSignature()
				: null;
			var type = methodSignature != null ? null : getErasedTypeName(method.getParameters().get(0).asType());

			tag.addAttribute(description, attrName, required, type, methodSignature);
		}

		addExtraAttributes(tag, typeElement);
	}

	/**
	 * Adds non-setter attributes (binding, validator, action, actionListener, valueChangeListener) by walking the
	 * class hierarchy and checking {@link StandardFacesAttributes#EXTRA_ATTRIBUTES}. These attributes are handled
	 * through special mechanisms in Jakarta Faces (value expression binding, method expression evaluation) and are
	 * not discoverable from setter methods. They must be scoped to the correct class hierarchy level to avoid
	 * injecting attributes into unrelated tag types.
	 */
	private void addExtraAttributes(Tag tag, TypeElement typeElement) {
		var current = typeElement;

		while (current != null) {
			var fqn = current.getQualifiedName().toString();
			var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get(fqn);

			if (extras != null) {
				for (var extra : extras) {
					tag.addAttribute(extra.description(), extra.name(), extra.required(), extra.type(), extra.methodSignature());
				}
			}

			var superMirror = current.getSuperclass();

			if (superMirror.getKind() == TypeKind.NONE) {
				break;
			}

			current = (TypeElement) processingEnv.getTypeUtils().asElement(superMirror);
		}
	}

	/**
	 * Collects {@code COMPONENT_TYPE} constant values from the class hierarchy of the given type element.
	 * These are used for looking up attribute descriptions in the taglib reader.
	 */
	private List<String> collectParentComponentTypes(TypeElement typeElement) {
		return collectParentConstants(typeElement, "COMPONENT_TYPE");
	}

	/**
	 * Collects converter IDs from the class hierarchy by reading {@code CONVERTER_ID} constants.
	 */
	private List<String> collectParentConverterIds(TypeElement typeElement) {
		return collectParentConstants(typeElement, "CONVERTER_ID");
	}

	/**
	 * Collects validator IDs from the class hierarchy by reading {@code VALIDATOR_ID} constants.
	 */
	private List<String> collectParentValidatorIds(TypeElement typeElement) {
		return collectParentConstants(typeElement, "VALIDATOR_ID");
	}

	private List<String> collectParentConstants(TypeElement typeElement, String constantName) {
		var ids = new ArrayList<String>();
		var current = typeElement;

		while (current != null) {
			for (var enclosed : current.getEnclosedElements()) {
				if (enclosed.getKind() == FIELD
					&& enclosed.getSimpleName().toString().equals(constantName)
					&& enclosed instanceof VariableElement variable) {
					var value = variable.getConstantValue();

					if (value instanceof String id) {
						ids.add(id);
					}
				}
			}

			var superMirror = current.getSuperclass();

			if (superMirror.getKind() == TypeKind.NONE) {
				break;
			}

			current = (TypeElement) processingEnv.getTypeUtils().asElement(superMirror);
		}

		return ids;
	}

	/**
	 * Looks up the description for an attribute by trying each parent type ID in the taglib reader.
	 */
	private String lookupTaglibDescription(List<String> parentTypeIds, String attrName) {
		for (var typeId : parentTypeIds) {
			var description = getTaglibReader().getDescription(typeId, attrName);

			if (description != null) {
				return description;
			}
		}

		return null;
	}

	/**
	 * Looks up the description for a setter-based attribute by walking the superclass hierarchy
	 * and checking {@link StandardFacesAttributes}.
	 */
	private String lookupStandardDescription(TypeElement typeElement, String attrName) {
		var current = typeElement;

		while (current != null) {
			var description = StandardFacesAttributes.getDescription(current.getQualifiedName().toString(), attrName);

			if (description != null) {
				return description;
			}

			var superMirror = current.getSuperclass();

			if (superMirror.getKind() == TypeKind.NONE) {
				break;
			}

			current = (TypeElement) processingEnv.getTypeUtils().asElement(superMirror);
		}

		return null;
	}

	/**
	 * Checks whether a setter is excluded by walking the class hierarchy from the declaring class upward and
	 * checking {@link StandardFacesAttributes#EXCLUDED_SETTERS}. This handles cases where a subclass overrides
	 * a setter that is excluded on a parent class.
	 */
	private boolean isExcludedSetter(TypeElement declaringClass, String attrName) {
		var current = declaringClass;

		while (current != null) {
			if (StandardFacesAttributes.EXCLUDED_SETTERS.getOrDefault(current.getQualifiedName().toString(), Set.of()).contains(attrName)) {
				return true;
			}

			var superMirror = current.getSuperclass();

			if (superMirror.getKind() == TypeKind.NONE) {
				break;
			}

			current = (TypeElement) processingEnv.getTypeUtils().asElement(superMirror);
		}

		return false;
	}

	/**
	 * Collects attributes for tag handler tags from {@link FacesAttribute}-annotated fields.
	 */
	private void collectTagHandlerAttributes(Tag tag, TypeElement typeElement) {
		for (var member : processingEnv.getElementUtils().getAllMembers(typeElement)) {
			if (member.getKind() != FIELD || member.getAnnotation(FacesAttribute.class) == null) {
				continue;
			}

			var field = (VariableElement) member;
			var facesAttribute = field.getAnnotation(FacesAttribute.class);
			var description = facesAttribute.description().isEmpty()
				? processJavadoc(processingEnv.getElementUtils().getDocComment(field))
				: facesAttribute.description();
			var attrName = facesAttribute.name().isEmpty() ? field.getSimpleName().toString() : facesAttribute.name();
			var required = facesAttribute.required();
			var type = getAttributeType(facesAttribute);

			tag.addAttribute(description, attrName, required, type, null);
		}

	}

	private void validateAnnotations(RoundEnvironment env) {
		for (var element : env.getElementsAnnotatedWith(FacesComponentConfig.class)) {
			if (element.getAnnotation(FacesComponent.class) == null) {
				error("@FacesComponentConfig is only supported on a @FacesComponent class; '"
					+ ((TypeElement) element).getQualifiedName() + "' is not annotated with @FacesComponent", element);
			}
		}

		for (var element : env.getElementsAnnotatedWith(FacesAttribute.class)) {
			if (element.getKind() == METHOD && !isSetterMethod((ExecutableElement) element)) {
				error("@FacesAttribute on a method is only supported on setter methods; '"
					+ element.getEnclosingElement().getSimpleName() + "." + element + "' is not a setter", element);
			}
		}
	}

	private static boolean isSetterMethod(ExecutableElement method) {
		var name = method.getSimpleName().toString();
		return name.length() > SETTER_PREFIX_LENGTH
			&& name.startsWith(SETTER_PREFIX)
			&& Character.isUpperCase(name.charAt(SETTER_PREFIX_LENGTH))
			&& method.getModifiers().contains(PUBLIC)
			&& !method.getModifiers().contains(STATIC)
			&& method.getReturnType().getKind() == VOID
			&& method.getParameters().size() == 1;
	}

	/**
	 * Returns the erased type name for a given type mirror. Type variables are mapped to {@code java.lang.Object},
	 * generic types are stripped of their type parameters, and arrays are handled recursively.
	 */
	private String getErasedTypeName(TypeMirror typeMirror) {
		if (typeMirror.getKind() == TypeKind.TYPEVAR) {
			return "java.lang.Object";
		}
		else if (typeMirror.getKind() == TypeKind.DECLARED) {
			return ((TypeElement) processingEnv.getTypeUtils().asElement(typeMirror)).getQualifiedName().toString();
		}
		else if (typeMirror.getKind() == TypeKind.ARRAY) {
			return getErasedTypeName(((ArrayType) typeMirror).getComponentType()) + "[]";
		}
		else {
			return typeMirror.toString();
		}
	}

	/**
	 * Returns the attribute type from a {@link FacesAttribute#type()} annotation element.
	 * Defaults to {@code java.lang.String} when unspecified ({@code void.class}).
	 */
	private String getAttributeType(FacesAttribute facesAttribute) {
		try {
			facesAttribute.type();
			return "java.lang.String"; // unreachable at compile time, but needed for the compiler
		}
		catch (MirroredTypeException e) {
			var typeMirror = e.getTypeMirror();

			if (typeMirror.getKind() == TypeKind.VOID) {
				return "java.lang.String";
			}

			return getErasedTypeName(typeMirror);
		}
	}

	/**
	 * Returns the renderer type from a {@link FacesComponentConfig#rendererType()} annotation element.
	 */
	private static String findRendererType(TypeElement typeElement) {
		var annotation = typeElement.getAnnotation(FacesComponentConfig.class);
		return annotation != null && !annotation.rendererType().isEmpty() ? annotation.rendererType() : null;
	}

	/**
	 * Returns the component handler class from a {@link FacesComponentConfig#componentHandler()} annotation element.
	 */
	private String getComponentHandlerClass(TypeElement typeElement) {
		var annotation = typeElement.getAnnotation(FacesComponentConfig.class);

		if (annotation == null) {
			return null;
		}

		return extractHandlerClass(
			() -> annotation.componentHandler(), "jakarta.faces.view.facelets.ComponentHandler");
	}

	/**
	 * Extracts a handler class FQN from an annotation attribute that holds a {@code Class<?>} value.
	 * The attribute accessor must be a lambda that reads the annotation element (which throws
	 * {@link MirroredTypeException} at compile time). Returns {@code null} when the value equals the
	 * given default FQN (meaning "no custom handler").
	 */
	private String extractHandlerClass(Runnable classAccessor, String defaultFqn) {
		try {
			classAccessor.run();
			return null; // unreachable at compile time, but needed for the compiler
		}
		catch (MirroredTypeException e) {
			var typeMirror = e.getTypeMirror();
			var fqn = ((TypeElement) processingEnv.getTypeUtils().asElement(typeMirror)).getQualifiedName().toString();
			return fqn.equals(defaultFqn) ? null : fqn;
		}
	}

	private FaceletTaglib getTaglib(Map<String, FaceletTaglib> taglibs, String namespace, Element element) {
		var taglib = taglibs.get(namespace);

		if (taglib == null) {
			error("No @FaceletTagLibrary declared for namespace '" + namespace + "'", element);
		}

		return taglib;
	}

	// Javadoc processing ----------------------------------------------------------------------------------------------

	/**
	 * Processes raw javadoc: strips block tags, converts inline tags, and trims.
	 */
	static String processJavadoc(String javadoc) {
		if (javadoc == null || javadoc.isBlank()) {
			return null;
		}

		var processed = javadoc;
		var blockTagMatch = JAVADOC_BLOCK_TAG.matcher(processed);

		if (blockTagMatch.find()) {
		    processed = processed.substring(0, blockTagMatch.start());
		}

		processed = INLINE_LINK.matcher(processed).replaceAll("<code>$1</code>");
		processed = INLINE_CODE.matcher(processed).replaceAll("<code>$1</code>");
		processed = INLINE_LITERAL.matcher(processed).replaceAll("$1");
		processed = INLINE_OTHER.matcher(processed).replaceAll("");
		processed = processed.strip();

		return processed.isEmpty() ? null : processed;
	}

	/**
	 * Processes setter javadoc: strips "Sets " or "Set " prefix, then delegates to {@link #processJavadoc(String)}.
	 */
	static String processSetterJavadoc(String javadoc) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

		var processed = javadoc.stripLeading();

		for (var prefix : SETTER_JAVADOC_PREFIXES) {
			if (processed.length() > prefix.length() && (processed.startsWith(prefix) || processed.startsWith(prefix.toLowerCase()))) {
				processed = Character.toUpperCase(processed.charAt(prefix.length())) + processed.substring(prefix.length() + 1);
				break;
			}
		}

		return processJavadoc(processed);
	}

	// XML generation --------------------------------------------------------------------------------------------------

	private void generateTaglibXmlFiles(Map<String, FaceletTaglib> taglibs) {
		for (var taglib : taglibs.values()) {
			try {
				var resource = processingEnv.getFiler().createResource(
					StandardLocation.CLASS_OUTPUT, "", "META-INF/" + taglib.getId() + ".taglib.xml");

				try (var output = resource.openOutputStream()) {
					createMarshaller(taglib.getVersion()).marshal(taglib, output);
				}

				note("Generated " + resource.toUri());
			}
			catch (JAXBException e) {
				throw new IllegalStateException(e);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static Marshaller createMarshaller(Version version) throws JAXBException {
		var jaxbProperties = new HashMap<String, Object>();

		jaxbProperties.put(JAXBContextProperties.OXM_METADATA_SOURCE, new MetadataSourceAdapter() {
			@Override
			public XmlBindings getXmlBindings(Map<String, ?> properties, ClassLoader classLoader) {
				var xmlSchema = new XmlSchema();
				xmlSchema.setElementFormDefault(XmlNsForm.QUALIFIED);
				xmlSchema.setNamespace(version.xmlNamespace());
				var xmlBindings = new XmlBindings();
				xmlBindings.setXmlSchema(xmlSchema);
				xmlBindings.setPackageName(FaceletTaglib.class.getPackageName());
				return xmlBindings;
			}
		});

		var jaxb = JAXBContextFactory.createContext(new Class[] { FaceletTaglib.class }, jaxbProperties);
		var marshaller = jaxb.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, version.schemaLocation());

		return marshaller;
	}

	// Logging ---------------------------------------------------------------------------------------------------------

	private void error(String message, Element element) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
	}

	private void note(Object message) {
		processingEnv.getMessager().printMessage(Kind.NOTE, String.valueOf(message));
	}

}
