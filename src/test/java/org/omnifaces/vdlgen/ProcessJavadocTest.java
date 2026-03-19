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
import static org.omnifaces.vdlgen.FaceletTagLibraryProcessor.processJavadoc;

import org.junit.jupiter.api.Test;

class ProcessJavadocTest {

	@Test
	void nullInput() {
		assertNull(processJavadoc(null));
	}

	@Test
	void emptyInput() {
		assertNull(processJavadoc(""));
	}

	@Test
	void blankInput() {
		assertNull(processJavadoc("   "));
	}

	@Test
	void plainText() {
		assertEquals("Some description.", processJavadoc("Some description."));
	}

	@Test
	void plainTextWithSurroundingWhitespace() {
		assertEquals("Some description.", processJavadoc("  Some description.  "));
	}

	// Block tag stripping ---

	@Test
	void stripsParamTag() {
		assertEquals("Description.", processJavadoc("Description.\n @param foo the foo"));
	}

	@Test
	void stripsReturnTag() {
		assertEquals("Description.", processJavadoc("Description.\n @return the value"));
	}

	@Test
	void stripsAuthorTag() {
		assertEquals("Description.", processJavadoc("Description.\n @author John"));
	}

	@Test
	void stripsSinceTag() {
		assertEquals("Description.", processJavadoc("Description.\n @since 1.0"));
	}

	@Test
	void stripsSeeTag() {
		assertEquals("Description.", processJavadoc("Description.\n @see Other"));
	}

	@Test
	void stripsThrowsTag() {
		assertEquals("Description.", processJavadoc("Description.\n @throws Exception if error"));
	}

	@Test
	void stripsMultipleBlockTags() {
		assertEquals("Description.", processJavadoc("Description.\n @param foo bar\n @return baz\n @since 1.0"));
	}

	@Test
	void returnsNullWhenOnlyBlockTags() {
		assertNull(processJavadoc(" @param foo bar"));
	}

	// Inline {@code} ---

	@Test
	void convertsInlineCode() {
		assertEquals("The <code>foo</code> value.", processJavadoc("The {@code foo} value."));
	}

	@Test
	void convertsMultipleInlineCodes() {
		assertEquals("Use <code>foo</code> or <code>bar</code>.", processJavadoc("Use {@code foo} or {@code bar}."));
	}

	// Inline {@literal} ---

	@Test
	void convertsInlineLiteral() {
		assertEquals("The foo value.", processJavadoc("The {@literal foo} value."));
	}

	// Inline {@link} ---

	@Test
	void convertsSimpleLink() {
		assertEquals("See <code>Foo</code>.", processJavadoc("See {@link Foo}."));
	}

	@Test
	void convertsLinkWithHash() {
		assertEquals("See <code>Foo#bar</code>.", processJavadoc("See {@link Foo#bar}."));
	}

	@Test
	void convertsLinkWithHashAndParens() {
		assertEquals("See <code>Foo#bar()</code>.", processJavadoc("See {@link Foo#bar()}."));
	}

	@Test
	void convertsLinkWithHashOnly() {
		assertEquals("See <code>#bar</code>.", processJavadoc("See {@link #bar}."));
	}

	@Test
	void convertsLinkWithHashAndParensOnly() {
		assertEquals("See <code>#bar()</code>.", processJavadoc("See {@link #bar()}."));
	}

	@Test
	void convertsLinkplain() {
		assertEquals("See <code>Foo</code>.", processJavadoc("See {@linkplain Foo}."));
	}

	// Inline {@value}, {@inheritDoc}, etc. ---

	@Test
	void stripsInlineValue() {
		assertEquals("The  value.", processJavadoc("The {@value DEFAULT} value."));
	}

	@Test
	void stripsInlineInheritDoc() {
		assertEquals("The  value.", processJavadoc("The {@inheritDoc} value."));
	}

	// Combination ---

	@Test
	void handlesMultipleInlineTagTypes() {
		assertEquals("Use <code>foo</code> with <code>Bar</code>.",
			processJavadoc("Use {@code foo} with {@link Bar}."));
	}

	@Test
	void handlesInlineTagsBeforeBlockTags() {
		assertEquals("Use <code>foo</code>.",
			processJavadoc("Use {@code foo}.\n @param bar baz"));
	}

	// Multiline ---

	@Test
	void preservesMultilineDescription() {
		assertEquals("First line.\n Second line.", processJavadoc(" First line.\n Second line."));
	}

	@Test
	void preservesHtmlInDescription() {
		assertEquals("The <p>\n description.", processJavadoc(" The <p>\n description.\n @author John"));
	}

}
