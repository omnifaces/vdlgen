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
 * Registers all <strong>public static non-void</strong> methods of a class as EL functions in the generated
 * {@code taglib.xml}. The function name defaults to the method name and the function description defaults to the
 * method's Javadoc.
 * <p>
 * Individual methods can be excluded by making them non-public, non-static, or void.
 * Individual methods can be further customized by also annotating them with {@link FacesFunction}.
 *
 * @author Bauke Scholtz
 * @since 1.0
 * @see FacesFunction
 * @see FaceletTagLibraryProcessor
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface FacesFunctions {

	/**
	 * The Facelet tag library namespace. This must match the {@link FaceletTagLibrary#namespace()} of a declared
	 * {@link FaceletTagLibrary}.
	 * @return The Facelet tag library namespace.
	 */
	String namespace();

}
