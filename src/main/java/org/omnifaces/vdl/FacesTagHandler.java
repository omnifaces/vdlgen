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
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.omnifaces.vdlgen.FaceletTagLibraryProcessor;

/**
 * Registers a {@link jakarta.faces.view.facelets.TagHandler TagHandler} subclass as a Facelet tag in the generated
 * {@code taglib.xml}.
 * <p>
 * The tag description is taken from the class Javadoc. The tag name defaults to the decapitalized simple class name.
 * Tag attributes are declared by annotating {@link jakarta.faces.view.facelets.TagAttribute TagAttribute} fields
 * with {@link FacesAttribute}.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FacesAttribute
 * @see FaceletTagLibraryProcessor
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface FacesTagHandler {

	/**
	 * The Facelet tag library namespace. This must match the {@link FaceletTagLibrary#namespace()} of a declared
	 * {@link FaceletTagLibrary}.
	 * @return The Facelet tag library namespace.
	 */
	String namespace();

	/**
	 * Optional tag name override. Defaults to the decapitalized simple class name as per JavaBeans spec.
	 * @return The tag name.
	 */
	String tagName() default "";

	/**
	 * Optional converter ID. When specified, the tag will be generated with a {@code <converter>} element containing
	 * the given converter ID instead of a plain {@code <handler-class>}. This is mutually exclusive with
	 * {@link #validatorId()}.
	 * @return The converter ID.
	 */
	String converterId() default "";

	/**
	 * Optional validator ID. When specified, the tag will be generated with a {@code <validator>} element containing
	 * the given validator ID instead of a plain {@code <handler-class>}. This is mutually exclusive with
	 * {@link #converterId()}.
	 * @return The validator ID.
	 */
	String validatorId() default "";

}
