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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.vdlgen.FaceletTagLibraryProcessor;

/**
 * Declares a Facelet tag attribute.
 * <p>
 * Can be placed on:
 * <ul>
 * <li>A setter method on a {@link UIComponent} subclass — the attribute name is derived from the setter name
 * ({@code setFoo} becomes {@code foo}), the type from the parameter type, and the description from Javadoc.</li>
 * <li>A {@link jakarta.faces.view.facelets.TagAttribute TagAttribute} field on a {@link TagHandler} subclass — the
 * attribute name defaults to the field name (or {@link #name()} override), the type defaults to
 * {@code java.lang.String} (or {@link #type()} override), and the description is taken from the field's Javadoc
 * or {@link #description()}.</li>
 * </ul>
 * <p>
 * This annotation is optional on component setters; all public setters in the class hierarchy are auto-detected.
 * Use it only when you need to override the default {@link #required()}, specify a {@link #methodSignature()},
 * or provide a {@link #description()} override.
 * On tag handler fields, this annotation is required to opt in a field as a tag attribute.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FaceletTagLibraryProcessor
 */
@Target({ METHOD, FIELD })
@Retention(SOURCE)
public @interface FacesAttribute {

	/**
	 * Optional attribute name override. Defaults to the field or setter-derived name.
	 * @return The attribute name.
	 */
	String name() default "";

	/**
	 * Optional description override. When specified, this takes precedence over Javadoc on the field or setter method.
	 * Useful for fields/setters without Javadoc, or when the Javadoc is not suitable as a taglib description.
	 * @return The attribute description.
	 */
	String description() default "";

	/**
	 * Whether this attribute is required. Defaults to {@code false}.
	 * @return Whether this attribute is required.
	 */
	boolean required() default false;

	/**
	 * The method signature for {@code MethodExpression}-typed attributes.
	 * When specified, the generated taglib will use a {@code <method-signature>} element instead of a {@code <type>}
	 * element. For example: {@code "void actionListener(jakarta.faces.event.ActionEvent)"}.
	 * <p>
	 * Only applicable to component setter methods; ignored on tag handler fields.
	 * @return The method signature, or empty string if this is a value attribute.
	 */
	String methodSignature() default "";

	/**
	 * The type for a tag handler attribute. When specified on a {@link TagHandler} field, the generated taglib
	 * will use this type instead of the default {@code java.lang.String}. For example:
	 * {@code @FacesAttribute(type = Converter.class)}.
	 * <p>
	 * Only applicable to tag handler fields; ignored on component setter methods.
	 * @return The attribute type, or {@code void.class} if the default should be used.
	 */
	Class<?> type() default void.class;

}
