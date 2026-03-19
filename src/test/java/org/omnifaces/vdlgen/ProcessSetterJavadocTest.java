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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.omnifaces.vdlgen.FaceletTagLibraryProcessor.processSetterJavadoc;

import org.junit.jupiter.api.Test;

class ProcessSetterJavadocTest {

	@Test
	void nullInput() {
		assertNull(processSetterJavadoc(null));
	}

	@Test
	void emptyInput() {
		assertNull(processSetterJavadoc(""));
	}

	@Test
	void blankInput() {
		assertNull(processSetterJavadoc("   "));
	}

	@Test
	void stripsSetsPrefix() {
		assertEquals("The value.", processSetterJavadoc("Sets the value."));
	}

	@Test
	void stripsSetsLowercasePrefix() {
		assertEquals("The value.", processSetterJavadoc("sets the value."));
	}

	@Test
	void capitalizesAfterStripping() {
		assertEquals("Whether this is enabled.", processSetterJavadoc("Sets whether this is enabled."));
	}

	@Test
	void capitalizesLowercaseAfterStripping() {
		assertEquals("A new converter.", processSetterJavadoc("Sets a new converter."));
	}

	@Test
	void stripsSetPrefix() {
		assertEquals("The value.", processSetterJavadoc("Set the value."));
	}

	@Test
	void stripsSetLowercasePrefix() {
		assertEquals("The value.", processSetterJavadoc("set the value."));
	}

	@Test
	void capitalizesAfterStrippingSetPrefix() {
		assertEquals("Whether this is enabled.", processSetterJavadoc("Set whether this is enabled."));
	}

	@Test
	void noSetsPrefixPassesThrough() {
		assertEquals("The value.", processSetterJavadoc("The value."));
	}

	@Test
	void stripsSetsPrefixWithLeadingWhitespace() {
		assertEquals("The value.", processSetterJavadoc("  Sets the value."));
	}

	@Test
	void stripsSetsPrefixAndBlockTags() {
		assertEquals("The value.", processSetterJavadoc("Sets the value.\n @param value the value"));
	}

	@Test
	void stripsSetsPrefixAndConvertsInlineTags() {
		assertEquals("The <code>Converter</code> instance.", processSetterJavadoc("Sets the {@code Converter} instance."));
	}

	@Test
	void exactlySetsSpacePassesThrough() {
		assertEquals("Sets", processSetterJavadoc("Sets "));
	}

	@Test
	void setsWithoutSpacePassesThrough() {
		assertEquals("SetsFoo", processSetterJavadoc("SetsFoo"));
	}

}
