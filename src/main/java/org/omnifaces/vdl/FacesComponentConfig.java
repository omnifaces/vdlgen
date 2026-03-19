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

import jakarta.faces.component.FacesComponent;
import jakarta.faces.view.facelets.ComponentHandler;

import org.omnifaces.vdlgen.FaceletTagLibraryProcessor;

/**
 * Specifies additional taglib configuration for a {@link FacesComponent}-annotated class.
 * <ul>
 * <li>{@link #componentHandler()} generates a {@code <handler-class>} element inside the {@code <component>} element.</li>
 * <li>{@link #rendererType()} generates a {@code <renderer-type>} element inside the {@code <component>} element.</li>
 * </ul>
 * <p>
 * The {@link #componentHandler()} can also be used on a {@link FacesTag}-annotated converter or validator class.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FaceletTagLibraryProcessor
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface FacesComponentConfig {

	/**
	 * The custom component handler class. Defaults to {@link ComponentHandler} (no custom handler).
	 * @return The component handler class.
	 */
	Class<? extends ComponentHandler> componentHandler() default ComponentHandler.class;

	/**
	 * The renderer type for the component. When specified, the generated taglib will include a
	 * {@code <renderer-type>} element inside the {@code <component>} element.
	 * @return The renderer type, or empty string if none.
	 */
	String rendererType() default "";

}
