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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StandardFacesAttributesTest {

	// EXCLUDED_SETTERS ---

	@Test
	void excludedSettersContainsParentForUIComponent() {
		assertTrue(StandardFacesAttributes.EXCLUDED_SETTERS.get("jakarta.faces.component.UIComponent").contains("parent"));
	}

	@Test
	void excludedSettersContainsTransientForUIComponentBase() {
		assertTrue(StandardFacesAttributes.EXCLUDED_SETTERS.get("jakarta.faces.component.UIComponentBase").contains("transient"));
	}

	@Test
	void excludedSettersContainsSubmittedValueForUIInput() {
		assertTrue(StandardFacesAttributes.EXCLUDED_SETTERS.get("jakarta.faces.component.UIInput").contains("submittedValue"));
	}

	@Test
	void excludedSettersDoesNotContainIdForUIComponent() {
		assertFalse(StandardFacesAttributes.EXCLUDED_SETTERS.get("jakarta.faces.component.UIComponent").contains("id"));
	}

	@Test
	void excludedSettersDoesNotContainValueForUIInput() {
		assertFalse(StandardFacesAttributes.EXCLUDED_SETTERS.get("jakarta.faces.component.UIInput").contains("value"));
	}

	// getDescription ---

	@Test
	void returnsDescriptionForUIComponentId() {
		var description = StandardFacesAttributes.getDescription("jakarta.faces.component.UIComponent", "id");
		assertNotNull(description);
		assertTrue(description.contains("component identifier"));
	}

	@Test
	void returnsDescriptionForUIComponentRendered() {
		var description = StandardFacesAttributes.getDescription("jakarta.faces.component.UIComponent", "rendered");
		assertNotNull(description);
		assertTrue(description.contains("rendered"));
	}

	@Test
	void returnsNullForUnknownClass() {
		assertNull(StandardFacesAttributes.getDescription("com.example.Unknown", "id"));
	}

	@Test
	void returnsNullForUnknownAttribute() {
		assertNull(StandardFacesAttributes.getDescription("jakarta.faces.component.UIComponent", "nonExistent"));
	}

	// EXTRA_ATTRIBUTES ---

	@Test
	void extraAttributesContainsBindingForUIComponent() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UIComponent");
		assertNotNull(extras);
		assertEquals(1, extras.size());
		assertEquals("binding", extras.get(0).name());
	}

	@Test
	void extraAttributesContainsValidatorForUIInput() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UIInput");
		assertNotNull(extras);
		assertEquals(2, extras.size());
		assertTrue(extras.stream().anyMatch(e -> "validator".equals(e.name())));
		assertTrue(extras.stream().anyMatch(e -> "valueChangeListener".equals(e.name())));
	}

	@Test
	void extraAttributesContainsActionForUICommand() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UICommand");
		assertNotNull(extras);
		assertEquals(2, extras.size());
		assertTrue(extras.stream().anyMatch(e -> "action".equals(e.name())));
		assertTrue(extras.stream().anyMatch(e -> "actionListener".equals(e.name())));
	}

	@Test
	void extraAttributeValidatorHasMethodSignature() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UIInput");
		var validator = extras.stream().filter(e -> "validator".equals(e.name())).findFirst().orElseThrow();
		assertNotNull(validator.methodSignature());
		assertNull(validator.type());
	}

	@Test
	void extraAttributeBindingHasType() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UIComponent");
		var binding = extras.get(0);
		assertNotNull(binding.type());
		assertNull(binding.methodSignature());
	}

	@Test
	void extraAttributesContainsActionAndIfForUIViewAction() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UIViewAction");
		assertNotNull(extras);
		assertEquals(3, extras.size());
		assertTrue(extras.stream().anyMatch(e -> "action".equals(e.name())));
		assertTrue(extras.stream().anyMatch(e -> "actionListener".equals(e.name())));
		assertTrue(extras.stream().anyMatch(e -> "if".equals(e.name())));
	}

	@Test
	void extraAttributeIfHasTypeBoolean() {
		var extras = StandardFacesAttributes.EXTRA_ATTRIBUTES.get("jakarta.faces.component.UIViewAction");
		var ifAttr = extras.stream().filter(e -> "if".equals(e.name())).findFirst().orElseThrow();
		assertEquals("boolean", ifAttr.type());
		assertNull(ifAttr.methodSignature());
	}

}
