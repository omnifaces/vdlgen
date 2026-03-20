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

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.junit.jupiter.api.Test;

import com.google.testing.compile.JavaFileObjects;

class FaceletTagLibraryProcessorTest {

	private static final String NAMESPACE = "http://test.example.com";
	private static final String TAGLIB_XML = "META-INF/test.taglib.xml";

	private static final JavaFileObject CONFIG = JavaFileObjects.forSourceString("test.Config", """
		package test;

		import org.omnifaces.vdl.FaceletTagLibrary;

		@FaceletTagLibrary(id = "test", namespace = "%s")
		public class Config {}
		""".formatted(NAMESPACE));

	// Empty taglib ---

	@Test
	void emptyTaglib() {
		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG);

		assertThat(compilation).succeeded();
		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.contains("<short-name>test</short-name>");
	}

	@Test
	void emptyTaglibHasNamespace() {
		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG);

		assertThat(compilation).succeeded();
		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.contains("<namespace>" + NAMESPACE + "</namespace>");
	}

	// Component tags ---

	@Test
	void componentTag() {
		var component = JavaFileObjects.forSourceString("test.MyComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;

			@FacesComponent(value = "test.MyComponent", namespace = "%s")
			public class MyComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<component-type>test.MyComponent</component-type>");
		xml.contains("<tag-name>myComponent</tag-name>");
	}

	@Test
	void componentWithSetterAttributes() {
		var component = JavaFileObjects.forSourceString("test.MyInput", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;

			/**
			 * A test input component.
			 */
			@FacesComponent(value = "test.MyInput", namespace = "%s")
			public class MyInput extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}

				/**
				 * Sets the label.
				 */
				public void setLabel(String label) {}

				/**
				 * Sets whether this component is required.
				 */
				public void setRequired(boolean required) {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<name>label</name>");
		xml.contains("<type>java.lang.String</type>");
		xml.contains("<name>required</name>");
		xml.contains("<type>boolean</type>");
	}

	@Test
	void componentWithCustomTagName() {
		var component = JavaFileObjects.forSourceString("test.MyComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;

			@FacesComponent(value = "test.MyComponent", namespace = "%s", tagName = "custom")
			public class MyComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();
		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.contains("<tag-name>custom</tag-name>");
	}

	@Test
	void componentWithoutMatchingNamespaceIsSkipped() {
		var component = JavaFileObjects.forSourceString("test.OtherComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;

			@FacesComponent(value = "test.OtherComponent", namespace = "http://other.example.com")
			public class OtherComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}
			}
			""");

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();
		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.doesNotContain("<tag-name>otherComponent</tag-name>");
	}

	// Converter/Validator tags ---

	@Test
	void converterTag() {
		var converter = JavaFileObjects.forSourceString("test.MyConverter", """
			package test;

			import jakarta.faces.convert.FacesConverter;
			import jakarta.faces.convert.Converter;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesConverterTag;

			/**
			 * A test converter.
			 */
			@FacesConverter("test.MyConverter")
			@FacesConverterTag(namespace = "%s")
			public class MyConverter implements Converter<Object> {
				@Override
				public Object getAsObject(FacesContext context, UIComponent component, String value) {
					return null;
				}

				@Override
				public String getAsString(FacesContext context, UIComponent component, Object value) {
					return null;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, converter);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<converter-id>test.MyConverter</converter-id>");
		xml.contains("<tag-name>myConverter</tag-name>");
	}

	@Test
	void validatorTag() {
		var validator = JavaFileObjects.forSourceString("test.MyValidator", """
			package test;

			import jakarta.faces.validator.FacesValidator;
			import jakarta.faces.validator.Validator;
			import jakarta.faces.validator.ValidatorException;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesValidatorTag;

			@FacesValidator("test.MyValidator")
			@FacesValidatorTag(namespace = "%s")
			public class MyValidator implements Validator<Object> {
				@Override
				public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, validator);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<validator-id>test.MyValidator</validator-id>");
		xml.contains("<tag-name>myValidator</tag-name>");
	}

	// Behavior tags ---

	@Test
	void behaviorTag() {
		var behavior = JavaFileObjects.forSourceString("test.MyBehavior", """
			package test;

			import jakarta.faces.component.behavior.FacesBehavior;
			import jakarta.faces.component.behavior.ClientBehaviorBase;
			import org.omnifaces.vdl.FacesBehaviorTag;

			/**
			 * A test behavior.
			 */
			@FacesBehavior("test.MyBehavior")
			@FacesBehaviorTag(namespace = "%s")
			public class MyBehavior extends ClientBehaviorBase {
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, behavior);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<behavior-id>test.MyBehavior</behavior-id>");
		xml.contains("<tag-name>myBehavior</tag-name>");
	}

	@Test
	void facesBehaviorTagWithoutFacesBehavior() {
		var clazz = JavaFileObjects.forSourceString("test.NotABehavior", """
			package test;

			import org.omnifaces.vdl.FacesBehaviorTag;

			@FacesBehaviorTag(namespace = "%s")
			public class NotABehavior {}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesBehaviorTag is only supported on a @FacesBehavior class");
	}

	@Test
	void facesBehaviorTagOnFacesConverterClass() {
		var clazz = JavaFileObjects.forSourceString("test.NotABehavior", """
			package test;

			import jakarta.faces.convert.FacesConverter;
			import jakarta.faces.convert.Converter;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesBehaviorTag;

			@FacesConverter("test.NotABehavior")
			@FacesBehaviorTag(namespace = "%s")
			public class NotABehavior implements Converter<Object> {
				@Override
				public Object getAsObject(FacesContext context, UIComponent component, String value) {
					return null;
				}

				@Override
				public String getAsString(FacesContext context, UIComponent component, Object value) {
					return null;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesBehaviorTag is only supported on a @FacesBehavior class");
	}

	@Test
	void facesBehaviorTagWithEmptyBehaviorId() {
		var clazz = JavaFileObjects.forSourceString("test.BadBehavior", """
			package test;

			import jakarta.faces.component.behavior.FacesBehavior;
			import jakarta.faces.component.behavior.ClientBehaviorBase;
			import org.omnifaces.vdl.FacesBehaviorTag;

			@FacesBehavior("")
			@FacesBehaviorTag(namespace = "%s")
			public class BadBehavior extends ClientBehaviorBase {
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesBehaviorTag on a @FacesBehavior requires a non-empty behavior ID");
	}

	// Tag handler ---

	@Test
	void tagHandler() {
		var handler = JavaFileObjects.forSourceString("test.MyTagHandler", """
			package test;

			import jakarta.faces.view.facelets.TagHandler;
			import jakarta.faces.view.facelets.TagConfig;
			import jakarta.faces.view.facelets.TagAttribute;
			import jakarta.faces.view.facelets.FaceletContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;
			import java.io.IOException;

			@FacesTagHandler(namespace = "%s")
			public class MyTagHandler extends TagHandler {
				@FacesAttribute
				private TagAttribute value;

				@FacesAttribute(required = true)
				private TagAttribute name;

				public MyTagHandler(TagConfig config) {
					super(config);
				}

				@Override
				public void apply(FaceletContext ctx, UIComponent parent) throws IOException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<tag-name>myTagHandler</tag-name>");
		xml.contains("<handler-class>test.MyTagHandler</handler-class>");
		xml.contains("<name>value</name>");
		xml.contains("<name>name</name>");
	}

	@Test
	void tagHandlerAttributeWithNameOverride() {
		var handler = JavaFileObjects.forSourceString("test.MyHandler", """
			package test;

			import jakarta.faces.view.facelets.TagHandler;
			import jakarta.faces.view.facelets.TagConfig;
			import jakarta.faces.view.facelets.TagAttribute;
			import jakarta.faces.view.facelets.FaceletContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;
			import java.io.IOException;

			@FacesTagHandler(namespace = "%s")
			public class MyHandler extends TagHandler {
				@FacesAttribute(name = "for")
				private TagAttribute forValue;

				public MyHandler(TagConfig config) {
					super(config);
				}

				@Override
				public void apply(FaceletContext ctx, UIComponent parent) throws IOException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<name>for</name>");
		xml.doesNotContain("<name>forValue</name>");
	}

	@Test
	void tagHandlerAttributeWithDescription() {
		var handler = JavaFileObjects.forSourceString("test.MyHandler", """
			package test;

			import jakarta.faces.view.facelets.TagHandler;
			import jakarta.faces.view.facelets.TagConfig;
			import jakarta.faces.view.facelets.TagAttribute;
			import jakarta.faces.view.facelets.FaceletContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;
			import java.io.IOException;

			@FacesTagHandler(namespace = "%s")
			public class MyHandler extends TagHandler {
				@FacesAttribute(description = "The target value.")
				private TagAttribute value;

				public MyHandler(TagConfig config) {
					super(config);
				}

				@Override
				public void apply(FaceletContext ctx, UIComponent parent) throws IOException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).succeeded();

		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.contains("The target value.");
	}

	@Test
	void tagHandlerWithConverterId() {
		var handler = JavaFileObjects.forSourceString("test.MyConverterHandler", """
			package test;

			import jakarta.faces.view.facelets.ConverterHandler;
			import jakarta.faces.view.facelets.ConverterConfig;
			import jakarta.faces.view.facelets.TagAttribute;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;

			@FacesTagHandler(namespace = "%s", converterId = "test.MyConverter")
			public class MyConverterHandler extends ConverterHandler {
				@FacesAttribute(description = "The converter binding.")
				private TagAttribute binding;

				public MyConverterHandler(ConverterConfig config) {
					super(config);
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<converter-id>test.MyConverter</converter-id>");
		xml.contains("<handler-class>test.MyConverterHandler</handler-class>");
		xml.contains("<name>binding</name>");
		xml.doesNotContain("<component-type>");
		xml.doesNotContain("<validator-id>");
	}

	@Test
	void tagHandlerWithValidatorId() {
		var handler = JavaFileObjects.forSourceString("test.MyValidatorHandler", """
			package test;

			import jakarta.faces.view.facelets.ValidatorHandler;
			import jakarta.faces.view.facelets.ValidatorConfig;
			import jakarta.faces.view.facelets.TagAttribute;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;

			@FacesTagHandler(namespace = "%s", validatorId = "test.MyValidator")
			public class MyValidatorHandler extends ValidatorHandler {
				@FacesAttribute(description = "Whether this validator is disabled.")
				private TagAttribute disabled;

				public MyValidatorHandler(ValidatorConfig config) {
					super(config);
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<validator-id>test.MyValidator</validator-id>");
		xml.contains("<handler-class>test.MyValidatorHandler</handler-class>");
		xml.contains("<name>disabled</name>");
		xml.doesNotContain("<component-type>");
		xml.doesNotContain("<converter-id>");
	}

	@Test
	void tagHandlerWithBothConverterIdAndValidatorIdFails() {
		var handler = JavaFileObjects.forSourceString("test.BadHandler", """
			package test;

			import jakarta.faces.view.facelets.TagHandler;
			import jakarta.faces.view.facelets.TagConfig;
			import jakarta.faces.view.facelets.FaceletContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesTagHandler;
			import java.io.IOException;

			@FacesTagHandler(namespace = "%s", converterId = "test.Converter", validatorId = "test.Validator")
			public class BadHandler extends TagHandler {
				public BadHandler(TagConfig config) {
					super(config);
				}

				@Override
				public void apply(FaceletContext ctx, UIComponent parent) throws IOException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("converterId and validatorId are mutually exclusive");
	}

	// Functions ---

	@Test
	void functionsClass() {
		var functions = JavaFileObjects.forSourceString("test.MyFunctions", """
			package test;

			import org.omnifaces.vdl.FacesFunctions;

			@FacesFunctions(namespace = "%s")
			public class MyFunctions {
				/**
				 * Capitalizes a string.
				 */
				public static String capitalize(String input) {
					return input;
				}

				public static int add(int a, int b) {
					return a + b;
				}

				// Non-public, should be excluded.
				static String internal(String s) {
					return s;
				}

				// Void return, should be excluded.
				public static void doNothing() {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, functions);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<function-name>capitalize</function-name>");
		xml.contains("<function-class>test.MyFunctions</function-class>");
		xml.contains("<function-signature>java.lang.String capitalize(java.lang.String)</function-signature>");
		xml.contains("<function-name>add</function-name>");
		xml.contains("<function-signature>int add(int, int)</function-signature>");
		xml.doesNotContain("<function-name>internal</function-name>");
		xml.doesNotContain("<function-name>doNothing</function-name>");
	}

	@Test
	void individualFunction() {
		var clazz = JavaFileObjects.forSourceString("test.Utils", """
			package test;

			import org.omnifaces.vdl.FacesFunction;

			public class Utils {
				@FacesFunction(namespace = "%s")
				public static String greet(String name) {
					return "Hello " + name;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).succeeded();
		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.contains("<function-name>greet</function-name>");
	}

	@Test
	void individualFunctionWithNameOverride() {
		var clazz = JavaFileObjects.forSourceString("test.Utils", """
			package test;

			import org.omnifaces.vdl.FacesFunction;

			public class Utils {
				@FacesFunction(namespace = "%s", name = "hello")
				public static String greet(String name) {
					return "Hello " + name;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).succeeded();
		assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String()
			.contains("<function-name>hello</function-name>");
	}

	// Error cases ---

	@Test
	void facesConverterTagWithoutFacesConverter() {
		var clazz = JavaFileObjects.forSourceString("test.NotAConverter", """
			package test;

			import org.omnifaces.vdl.FacesConverterTag;

			@FacesConverterTag(namespace = "%s")
			public class NotAConverter {}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesConverterTag is only supported on a @FacesConverter class");
	}

	@Test
	void facesConverterTagOnFacesValidatorClass() {
		var clazz = JavaFileObjects.forSourceString("test.NotAConverter", """
			package test;

			import jakarta.faces.validator.FacesValidator;
			import jakarta.faces.validator.Validator;
			import jakarta.faces.validator.ValidatorException;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesConverterTag;

			@FacesValidator("test.NotAConverter")
			@FacesConverterTag(namespace = "%s")
			public class NotAConverter implements Validator<Object> {
				@Override
				public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesConverterTag is only supported on a @FacesConverter class");
	}

	@Test
	void facesConverterTagWithEmptyConverterId() {
		var clazz = JavaFileObjects.forSourceString("test.BadConverter", """
			package test;

			import jakarta.faces.convert.FacesConverter;
			import jakarta.faces.convert.Converter;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesConverterTag;

			@FacesConverter
			@FacesConverterTag(namespace = "%s")
			public class BadConverter implements Converter<Object> {
				@Override
				public Object getAsObject(FacesContext context, UIComponent component, String value) {
					return null;
				}

				@Override
				public String getAsString(FacesContext context, UIComponent component, Object value) {
					return null;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesConverterTag on a @FacesConverter requires a non-empty converter ID");
	}

	@Test
	void facesValidatorTagWithoutFacesValidator() {
		var clazz = JavaFileObjects.forSourceString("test.NotAValidator", """
			package test;

			import org.omnifaces.vdl.FacesValidatorTag;

			@FacesValidatorTag(namespace = "%s")
			public class NotAValidator {}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesValidatorTag is only supported on a @FacesValidator class");
	}

	@Test
	void facesValidatorTagOnFacesConverterClass() {
		var clazz = JavaFileObjects.forSourceString("test.NotAValidator", """
			package test;

			import jakarta.faces.convert.FacesConverter;
			import jakarta.faces.convert.Converter;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesValidatorTag;

			@FacesConverter("test.NotAValidator")
			@FacesValidatorTag(namespace = "%s")
			public class NotAValidator implements Converter<Object> {
				@Override
				public Object getAsObject(FacesContext context, UIComponent component, String value) {
					return null;
				}

				@Override
				public String getAsString(FacesContext context, UIComponent component, Object value) {
					return null;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesValidatorTag is only supported on a @FacesValidator class");
	}

	@Test
	void facesValidatorTagWithEmptyValidatorId() {
		var clazz = JavaFileObjects.forSourceString("test.BadValidator", """
			package test;

			import jakarta.faces.validator.FacesValidator;
			import jakarta.faces.validator.Validator;
			import jakarta.faces.validator.ValidatorException;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesValidatorTag;

			@FacesValidator
			@FacesValidatorTag(namespace = "%s")
			public class BadValidator implements Validator<Object> {
				@Override
				public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesValidatorTag on a @FacesValidator requires a non-empty validator ID");
	}

	@Test
	void facesFunctionOnVoidMethod() {
		var clazz = JavaFileObjects.forSourceString("test.BadFunctions", """
			package test;

			import org.omnifaces.vdl.FacesFunction;

			public class BadFunctions {
				@FacesFunction(namespace = "%s")
				public static void doNothing() {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("public static non-void");
	}

	@Test
	void facesFunctionOnNonStaticMethod() {
		var clazz = JavaFileObjects.forSourceString("test.BadFunctions", """
			package test;

			import org.omnifaces.vdl.FacesFunction;

			public class BadFunctions {
				@FacesFunction(namespace = "%s")
				public String greet(String name) {
					return "Hello " + name;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("public static non-void");
	}

	@Test
	void unknownNamespaceOnFacesConverterTag() {
		var clazz = JavaFileObjects.forSourceString("test.MyConverter", """
			package test;

			import jakarta.faces.convert.FacesConverter;
			import jakarta.faces.convert.Converter;
			import jakarta.faces.context.FacesContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesConverterTag;

			@FacesConverter("test.MyConverter")
			@FacesConverterTag(namespace = "http://unknown.example.com")
			public class MyConverter implements Converter<Object> {
				@Override
				public Object getAsObject(FacesContext context, UIComponent component, String value) {
					return null;
				}

				@Override
				public String getAsString(FacesContext context, UIComponent component, Object value) {
					return null;
				}
			}
			""");

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("No @FaceletTagLibrary declared for namespace");
	}

	@Test
	void inheritedAttributesHaveDescriptions() {
		var component = JavaFileObjects.forSourceString("test.MyComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;

			@FacesComponent(value = "test.MyComponent", namespace = "%s")
			public class MyComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<name>id</name>");
		xml.contains("component identifier");
		xml.contains("<name>rendered</name>");
		xml.contains("whether or not this component should be rendered");
	}

	@Test
	void setterJavadocWithSetPrefix() {
		var component = JavaFileObjects.forSourceString("test.MyInput", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;

			@FacesComponent(value = "test.MyInput", namespace = "%s")
			public class MyInput extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}

				/**
				 * Set the value of the label.
				 */
				public void setLabel(String label) {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<name>label</name>");
		xml.contains("The value of the label.");
	}

	@Test
	void componentWithRendererType() {
		var component = JavaFileObjects.forSourceString("test.MyMessages", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;
			import org.omnifaces.vdl.FacesComponentConfig;

			@FacesComponent(value = "test.MyMessages", namespace = "%s")
			@FacesComponentConfig(rendererType = "test.MessagesRenderer")
			public class MyMessages extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}

				public void setVar(String var) {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<renderer-type>test.MessagesRenderer</renderer-type>");
		xml.contains("<component-type>test.MyMessages</component-type>");
		xml.doesNotContain("<handler-class>");
	}

	@Test
	void componentWithComponentHandler() {
		var handler = JavaFileObjects.forSourceString("test.MyHandler", """
			package test;

			import jakarta.faces.view.facelets.ComponentHandler;
			import jakarta.faces.view.facelets.ComponentConfig;

			public class MyHandler extends ComponentHandler {
				public MyHandler(ComponentConfig config) {
					super(config);
				}
			}
			""");

		var component = JavaFileObjects.forSourceString("test.MyComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;
			import org.omnifaces.vdl.FacesComponentConfig;

			@FacesComponent(value = "test.MyComponent", namespace = "%s")
			@FacesComponentConfig(componentHandler = MyHandler.class)
			public class MyComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<handler-class>test.MyHandler</handler-class>");
		xml.contains("<component-type>test.MyComponent</component-type>");
		xml.doesNotContain("<renderer-type>");
	}

	@Test
	void componentWithComponentHandlerAndRendererType() {
		var handler = JavaFileObjects.forSourceString("test.MyHandler", """
			package test;

			import jakarta.faces.view.facelets.ComponentHandler;
			import jakarta.faces.view.facelets.ComponentConfig;

			public class MyHandler extends ComponentHandler {
				public MyHandler(ComponentConfig config) {
					super(config);
				}
			}
			""");

		var component = JavaFileObjects.forSourceString("test.MyComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;
			import org.omnifaces.vdl.FacesComponentConfig;

			@FacesComponent(value = "test.MyComponent", namespace = "%s")
			@FacesComponentConfig(componentHandler = MyHandler.class, rendererType = "test.MyRenderer")
			public class MyComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<handler-class>test.MyHandler</handler-class>");
		xml.contains("<renderer-type>test.MyRenderer</renderer-type>");
		xml.contains("<component-type>test.MyComponent</component-type>");
	}

	@Test
	void tagHandlerAttributeWithTypeOverride() {
		var handler = JavaFileObjects.forSourceString("test.MyHandler", """
			package test;

			import jakarta.faces.view.facelets.TagHandler;
			import jakarta.faces.view.facelets.TagConfig;
			import jakarta.faces.view.facelets.TagAttribute;
			import jakarta.faces.view.facelets.FaceletContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;
			import java.io.IOException;

			@FacesTagHandler(namespace = "%s")
			public class MyHandler extends TagHandler {
				@FacesAttribute(type = boolean.class)
				private TagAttribute disabled;

				@FacesAttribute(type = Object.class)
				private TagAttribute validator;

				@FacesAttribute
				private TagAttribute value;

				public MyHandler(TagConfig config) {
					super(config);
				}

				@Override
				public void apply(FaceletContext ctx, UIComponent parent) throws IOException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<name>disabled</name>");
		xml.contains("<type>boolean</type>");
		xml.contains("<name>validator</name>");
		xml.contains("<type>java.lang.Object</type>");
		xml.contains("<name>value</name>");
		xml.contains("<type>java.lang.String</type>");
	}

	@Test
	void facesAttributeAtClassLevel() {
		var handler = JavaFileObjects.forSourceString("test.BadHandler", """
			package test;

			import jakarta.faces.view.facelets.TagHandler;
			import jakarta.faces.view.facelets.TagConfig;
			import jakarta.faces.view.facelets.FaceletContext;
			import jakarta.faces.component.UIComponent;
			import org.omnifaces.vdl.FacesTagHandler;
			import org.omnifaces.vdl.FacesAttribute;
			import java.io.IOException;

			@FacesTagHandler(namespace = "%s")
			@FacesAttribute(name = "value", description = "The value.")
			public class BadHandler extends TagHandler {
				public BadHandler(TagConfig config) {
					super(config);
				}

				@Override
				public void apply(FaceletContext ctx, UIComponent parent) throws IOException {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, handler);

		assertThat(compilation).failed();
	}

	@Test
	void componentWithMethodExpressionAttribute() {
		var component = JavaFileObjects.forSourceString("test.MyAction", """
			package test;

			import jakarta.el.MethodExpression;
			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;
			import org.omnifaces.vdl.FacesAttribute;

			@FacesComponent(value = "test.MyAction", namespace = "%s")
			public class MyAction extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}

				/**
				 * Sets the action to invoke when this component is activated.
				 */
				@FacesAttribute(methodSignature = "java.lang.Object action()")
				public void setAction(MethodExpression action) {}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).succeeded();

		var xml = assertThat(compilation)
			.generatedFile(StandardLocation.CLASS_OUTPUT, TAGLIB_XML)
			.contentsAsUtf8String();

		xml.contains("<name>action</name>");
		xml.contains("<method-signature>java.lang.Object action()</method-signature>");
		xml.doesNotContain("<type>jakarta.el.MethodExpression</type>");
	}

	@Test
	void facesComponentConfigWithoutFacesComponent() {
		var clazz = JavaFileObjects.forSourceString("test.NotAComponent", """
			package test;

			import org.omnifaces.vdl.FacesComponentConfig;

			@FacesComponentConfig(rendererType = "test.Renderer")
			public class NotAComponent {}
			""");

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, clazz);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesComponentConfig is only supported on a @FacesComponent class");
	}

	@Test
	void facesAttributeOnGetterMethod() {
		var component = JavaFileObjects.forSourceString("test.BadComponent", """
			package test;

			import jakarta.faces.component.FacesComponent;
			import jakarta.faces.component.UIComponentBase;
			import org.omnifaces.vdl.FacesAttribute;

			@FacesComponent(value = "test.BadComponent", namespace = "%s")
			public class BadComponent extends UIComponentBase {
				@Override
				public String getFamily() {
					return "test";
				}

				@FacesAttribute
				public String getFoo() {
					return null;
				}
			}
			""".formatted(NAMESPACE));

		var compilation = javac()
			.withProcessors(new FaceletTagLibraryProcessor())
			.compile(CONFIG, component);

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining("@FacesAttribute on a method is only supported on setter methods");
	}

}
