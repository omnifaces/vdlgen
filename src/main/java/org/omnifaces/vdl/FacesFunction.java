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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.omnifaces.vdlgen.FaceletTagLibraryProcessor;

/**
 * Registers a <strong>public static non-void</strong> method as an EL function in the generated {@code taglib.xml}.
 * <p>
 * The function description is taken from the method's Javadoc. The function signature is derived from the method's
 * return type and parameter types.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FacesFunctions
 * @see FaceletTagLibraryProcessor
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface FacesFunction {

	/**
	 * The Facelet tag library namespace. This must match the {@link FaceletTagLibrary#namespace()} of a declared
	 * {@link FaceletTagLibrary}.
	 * @return The Facelet tag library namespace.
	 */
	String namespace();

	/**
	 * Optional function name override. Defaults to the method name.
	 * @return The function name.
	 */
	String name() default "";

}
