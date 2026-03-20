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

import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.persistence.oxm.annotations.XmlCDATA;
import org.omnifaces.vdl.FaceletTagLibrary.Version;
import org.omnifaces.vdlgen.FaceletTaglib.Tag.Behavior;
import org.omnifaces.vdlgen.FaceletTaglib.Tag.Component;
import org.omnifaces.vdlgen.FaceletTaglib.Tag.Converter;
import org.omnifaces.vdlgen.FaceletTaglib.Tag.Validator;

/**
 * INTERNAL ONLY
 * <p>
 * JAXB mapping of {@code <facelet-taglib>}. The {@link FaceletTagLibraryProcessor} converts annotations found in
 * source code to instances of this class which are then marshalled to {@code *.taglib.xml} files via JAXB.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FaceletTagLibraryProcessor
 */
@XmlRootElement(name = "facelet-taglib")
@XmlAccessorType(XmlAccessType.FIELD)
class FaceletTaglib implements Comparable<FaceletTaglib> {

	@XmlAttribute @XmlJavaTypeAdapter(XmlEnumToStringAdapter.class)
	private Version version;

	@XmlElement
	private String description;

	@XmlElement
	private String namespace;

	@XmlElement(name = "short-name")
	private String shortName;

	@XmlElement(name = "tag")
	private SortedSet<Tag> tags = new TreeSet<>();

	@XmlElement(name = "function")
	private SortedSet<Function> functions = new TreeSet<>();

	private transient String id;

	public FaceletTaglib() {
		// Keep default c'tor alive for JAXB.
	}

	FaceletTaglib(Version version, String description, String namespace, String id, String shortName) {
		this.version = version;
		this.description = description;
		this.namespace = namespace;
		this.id = id;
		this.shortName = shortName;
	}

	Version getVersion() {
		return version;
	}

	String getId() {
		return id;
	}

	String getShortName() {
		return shortName;
	}

	Tag addComponent(String description, String tagName, String componentType, String handlerClass, String rendererType) {
		var component = new Component();
		component.componentType = componentType;
		component.rendererType = rendererType;
		component.handlerClass = handlerClass;
		var tag = new Tag(description, tagName);
		tag.component = component;

		if (!tags.add(tag)) {
			throw new IllegalStateException("Duplicate tag " + tag.tagName);
		}

		return tag;
	}

	Tag addConverter(String description, String tagName, String converterId, String handlerClass) {
		var converter = new Converter();
		converter.converterId = converterId;
		converter.handlerClass = handlerClass;
		var tag = new Tag(description, tagName);
		tag.converter = converter;

		if (!tags.add(tag)) {
			throw new IllegalStateException("Duplicate tag " + tag.tagName);
		}

		return tag;
	}

	Tag addValidator(String description, String tagName, String validatorId, String handlerClass) {
		var validator = new Validator();
		validator.validatorId = validatorId;
		validator.handlerClass = handlerClass;
		var tag = new Tag(description, tagName);
		tag.validator = validator;

		if (!tags.add(tag)) {
			throw new IllegalStateException("Duplicate tag " + tag.tagName);
		}

		return tag;
	}

	Tag addBehavior(String description, String tagName, String behaviorId, String handlerClass) {
		var behavior = new Behavior();
		behavior.behaviorId = behaviorId;
		behavior.handlerClass = handlerClass;
		var tag = new Tag(description, tagName);
		tag.behavior = behavior;

		if (!tags.add(tag)) {
			throw new IllegalStateException("Duplicate tag " + tag.tagName);
		}

		return tag;
	}

	Tag addTagHandler(String description, String tagName, String handlerClass) {
		var tag = new Tag(description, tagName);
		tag.handlerClass = handlerClass;

		if (!tags.add(tag)) {
			throw new IllegalStateException("Duplicate tag " + tag.tagName);
		}

		return tag;
	}

	void addFunction(String description, String functionName, String functionClass, String functionSignature) {
		var function = new Function(description, functionName, functionClass, functionSignature);

		if (!functions.add(function)) {
			throw new IllegalStateException("Duplicate function " + function.functionName);
		}
	}

	@Override
	public int compareTo(FaceletTaglib other) {
		return this.id.compareTo(other.id);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	@XmlAccessorType(XmlAccessType.FIELD)
	static class Tag implements Comparable<Tag> {

		@XmlElement @XmlCDATA
		private String description;

		@XmlElement(name = "tag-name")
		private String tagName;

		@XmlElement
		private Component component;

		@XmlElement
		private Converter converter;

		@XmlElement
		private Validator validator;

		@XmlElement
		private Behavior behavior;

		@XmlElement(name = "handler-class")
		private String handlerClass;

		@XmlElement(name = "attribute")
		private SortedSet<Attribute> attributes = new TreeSet<>();

		public Tag() {
			// Keep default c'tor alive for JAXB.
		}

		private Tag(String description, String tagName) {
			this.description = description;
			this.tagName = tagName;
		}

		void addAttribute(String description, String name, boolean required, String type, String methodSignature) {
			var attribute = new Attribute(description, name, required, type, methodSignature);

			if (!attributes.add(attribute)) {
				// Skip duplicate (e.g. extra attribute already added for same name).
			}
		}

		boolean hasAttribute(String name) {
			return attributes.stream().anyMatch(a -> a.name.equals(name));
		}

		@Override
		public int compareTo(Tag other) {
			return tagName.compareTo(other.tagName);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + tagName + "]";
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		static class Component {

			@XmlElement(name = "component-type")
			private String componentType;

			@XmlElement(name = "renderer-type")
			private String rendererType;

			@XmlElement(name = "handler-class")
			private String handlerClass;
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		static class Converter {

			@XmlElement(name = "converter-id")
			private String converterId;

			@XmlElement(name = "handler-class")
			private String handlerClass;
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		static class Validator {

			@XmlElement(name = "validator-id")
			private String validatorId;

			@XmlElement(name = "handler-class")
			private String handlerClass;
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		static class Behavior {

			@XmlElement(name = "behavior-id")
			private String behaviorId;

			@XmlElement(name = "handler-class")
			private String handlerClass;
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		static class Attribute implements Comparable<Attribute> {

			@XmlElement @XmlCDATA
			private String description;

			@XmlElement
			private String name;

			@XmlElement
			private boolean required;

			@XmlElement
			private String type;

			@XmlElement(name = "method-signature")
			private String methodSignature;

			public Attribute() {
				// Keep default c'tor alive for JAXB.
			}

			private Attribute(String description, String name, boolean required, String type, String methodSignature) {
				this.description = description;
				this.name = name;
				this.required = required;
				this.type = type;
				this.methodSignature = methodSignature;
			}

			@Override
			public int compareTo(Attribute other) {
				return name.compareTo(other.name);
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "[" + name + "]";
			}
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	static class Function implements Comparable<Function> {

		@XmlElement @XmlCDATA
		private String description;

		@XmlElement(name = "function-name")
		private String functionName;

		@XmlElement(name = "function-class")
		private String functionClass;

		@XmlElement(name = "function-signature")
		private String functionSignature;

		public Function() {
			// Keep default c'tor alive for JAXB.
		}

		private Function(String description, String functionName, String functionClass, String functionSignature) {
			this.description = description;
			this.functionName = functionName;
			this.functionClass = functionClass;
			this.functionSignature = functionSignature;
		}

		@Override
		public int compareTo(Function other) {
			return functionName.compareTo(other.functionName);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + functionName + "]";
		}
	}

}
