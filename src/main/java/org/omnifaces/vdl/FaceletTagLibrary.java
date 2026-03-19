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
package org.omnifaces.vdl;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.vdlgen.FaceletTagLibraryProcessor;

/**
 * <p>
 * Declares a Facelet tag library for which a {@code taglib.xml} file will be generated during compilation.
 *
 * <h2>Usage</h2>
 * <ol>
 * <li>Include {@code org.omnifaces:vdlgen} as compile-only dependency (thus not runtime!).</li>
 * <li>Put {@link FaceletTagLibrary} on any class in the project (e.g. a config/startup class) to declare the taglib
 * metadata (id, namespace, description, version).</li>
 * <li>{@link UIComponent} classes with {@link FacesComponent} whose
 * {@link FacesComponent#namespace() namespace} matches this taglib's {@link #namespace()} will be included as
 * component tags. Their attributes are auto-detected from public setter methods in the class hierarchy.</li>
 * <li>{@link TagHandler} classes annotated with {@link FacesTagHandler} whose
 * {@link FacesTagHandler#namespace() namespace} matches this taglib's {@link #namespace()} will be included as tag
 * handler tags. Their attributes are declared via {@link FacesAttribute} on
 * {@link jakarta.faces.view.facelets.TagAttribute TagAttribute} fields.</li>
 * <li>Converter/validator classes annotated with {@link FacesTag} whose {@link FacesTag#namespace() namespace} matches
 * this taglib's {@link #namespace()} will be included as converter/validator tags.</li>
 * <li>Methods annotated with {@link FacesFunction} whose {@link FacesFunction#namespace() namespace} matches this
 * taglib's {@link #namespace()} or classes annotated with {@link FacesFunctions} whose
 * {@link FacesFunctions#namespace() namespace} matches this taglib's {@link #namespace()} will be included
 * as EL functions.</li>
 * <li>The {@link FaceletTagLibraryProcessor} will automatically generate {@code META-INF/*.taglib.xml} files.</li>
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FacesTag
 * @see FacesTagHandler
 * @see FacesAttribute
 * @see FacesFunction
 * @see FacesFunctions
 * @see FacesComponentConfig
 * @see FaceletTagLibraryProcessor
 */
@Target(TYPE)
@Repeatable(FaceletTagLibrary.List.class)
public @interface FaceletTagLibrary {

	/**
	 * Makes {@link FaceletTagLibrary} {@link Repeatable}.
	 */
	@Target(TYPE)
	@interface List {

		/**
		 * Returns {@link FaceletTagLibrary} as array.
		 * @return {@link FaceletTagLibrary} as array.
		 */
		FaceletTagLibrary[] value();
	}

	/**
	 * Available {@code taglib.xml} versions.
	 *
	 * @author Bauke Scholtz
	 * @since 1.0
	 */
	enum Version {

	    /** Version 4.1 */
	    V_4_1("https://jakarta.ee/xml/ns/jakartaee", "https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_1.xsd"),

		/** Version 4.0 */
		V_4_0("https://jakarta.ee/xml/ns/jakartaee", "https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd");

		private final String value;
		private final String xmlNamespace;
		private final String schemaLocation;

		Version(String xmlNamespace, String schemaLocation) {
			this.value = name().split("V_", 2)[1].replace('_', '.');
			this.xmlNamespace = xmlNamespace;
			this.schemaLocation = xmlNamespace + " " + schemaLocation;
		}

		/**
		 * Returns the version's string value, e.g. "4.1", "4.0", etc.
		 * @return The version's string value.
		 */
		public String value() {
			return value;
		}

		/**
		 * Returns the XML namespace associated with this version.
		 * @return The XML namespace.
		 */
		public String xmlNamespace() {
			return xmlNamespace;
		}

		/**
		 * Returns the XML schema location associated with this version.
		 * @return The XML schema location.
		 */
		public String schemaLocation() {
			return schemaLocation;
		}

		/**
		 * Returns {@link #value()}.
		 * @return {@link #value()}.
		 */
		@Override
		public String toString() {
			return value();
		}

	}

	/**
	 * Required: the unique taglib identifier. This is used as the base filename (e.g. {@code "omnifaces"} generates
	 * {@code META-INF/omnifaces.taglib.xml}) and as the {@code <short-name>} in the generated {@code taglib.xml}
	 * when {@link #shortName()} is not specified.
	 * @return The taglib identifier.
	 */
	String id();

	/**
	 * Optional: the taglib short name. Defaults to {@link #id()} when empty. This is used as the
	 * {@code <short-name>} in the generated {@code taglib.xml}.
	 * @return The taglib short name.
	 */
	String shortName() default "";

	/**
	 * Required: the unique taglib namespace URI. This is used in the {@code <namespace>} element and serves as the
	 * matching key for annotations like {@link FacesTagHandler#namespace()}, {@link FacesFunction#namespace()}, etc.
	 * <p>
	 * For {@link FacesComponent}-annotated classes, the {@link FacesComponent#namespace()} is matched against this
	 * value.
	 * @return The taglib namespace URI.
	 */
	String namespace();

	/**
	 * Optional: the taglib description. Defaults to empty. This is used in the {@code <description>} element.
	 * @return The taglib description.
	 */
	String description() default "";

	/**
	 * Optional: the taglib version. Defaults to {@link Version#V_4_1}. This controls the XML namespace, schema
	 * location, and version attribute in the generated {@code taglib.xml}.
	 * @return The taglib version.
	 */
	Version version() default Version.V_4_1;

}
