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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

/**
 * INTERNAL ONLY
 * <p>
 * Reads attribute descriptions from {@code *.taglib.xml} files found on the classpath. This enables the annotation
 * processor to extract descriptions of inherited attributes from a Jakarta Faces implementation JAR (e.g.
 * {@code org.glassfish:jakarta.faces}) without needing hardcoded/copypasted description registries.
 * <p>
 * Descriptions are indexed by tag type ID (component type, converter ID, or validator ID) and attribute name.
 * HTML markup in descriptions is stripped to produce plain text suitable for generated VDL documentation.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
final class TaglibDescriptionReader {

	private static final String TAGLIB_NAMESPACE = "https://jakarta.ee/xml/ns/jakartaee";
	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

	private final Map<String, Map<String, String>> descriptionsByTypeId;

	TaglibDescriptionReader(ClassLoader classLoader) {
		descriptionsByTypeId = loadDescriptions(classLoader);
	}

	/**
	 * Returns the description for the given attribute of the given tag type ID.
	 * @param typeId The tag type ID (component type, converter ID, or validator ID).
	 * @param attributeName The attribute name (e.g. {@code "style"}).
	 * @return The description, or {@code null} if not found.
	 */
	String getDescription(String typeId, String attributeName) {
		return descriptionsByTypeId.getOrDefault(typeId, Map.of()).get(attributeName);
	}

	private static Map<String, Map<String, String>> loadDescriptions(ClassLoader classLoader) {
		var descriptions = new HashMap<String, Map<String, String>>();

		try {
			var resources = findTaglibResources(classLoader);

			while (resources.hasMoreElements()) {
				var url = resources.nextElement();
				parseTaglib(url, descriptions);
			}
		}
		catch (Exception e) {
			// Silently fall back to empty descriptions; the processor will use StandardFacesAttributes instead.
		}

		return Collections.unmodifiableMap(descriptions);
	}

	private static Enumeration<URL> findTaglibResources(ClassLoader classLoader) throws IOException {
		return classLoader.getResources("com/sun/faces/metadata/taglib/faces.html.taglib.xml");
	}

	private static void parseTaglib(URL url, Map<String, Map<String, String>> descriptions) {
		try {
			var urlStr = url.toString();
			var jarBaseUrl = urlStr.substring(0, urlStr.lastIndexOf("!/") + 2);
			var taglibPaths = new String[] {
				"com/sun/faces/metadata/taglib/faces.html.taglib.xml",
				"com/sun/faces/metadata/taglib/faces.core.taglib.xml"
			};

			var factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);

			var builder = factory.newDocumentBuilder();

			for (var path : taglibPaths) {
				var taglibUrl = new URL(jarBaseUrl + path);

				try (var input = taglibUrl.openStream()) {
					var doc = builder.parse(input);
					var tags = doc.getElementsByTagNameNS(TAGLIB_NAMESPACE, "tag");

					for (int i = 0; i < tags.getLength(); i++) {
						parseTag((Element) tags.item(i), descriptions);
					}
				}
			}
		}
		catch (Exception e) {
			// Silently skip unparseable taglib.
		}
	}

	private static void parseTag(Element tagElement, Map<String, Map<String, String>> descriptions) {
		var tagTypeId = getTagTypeId(tagElement);

		if (tagTypeId == null) {
			return;
		}

		var attributes = tagElement.getElementsByTagNameNS(TAGLIB_NAMESPACE, "attribute");
		var attrMap = descriptions.computeIfAbsent(tagTypeId, k -> new HashMap<>());

		for (int i = 0; i < attributes.getLength(); i++) {
			var attrElement = (Element) attributes.item(i);
			var nameElements = attrElement.getElementsByTagNameNS(TAGLIB_NAMESPACE, "name");
			var descElements = attrElement.getElementsByTagNameNS(TAGLIB_NAMESPACE, "description");

			if (nameElements.getLength() == 0) {
				continue;
			}

			var name = nameElements.item(0).getTextContent().strip();
			var description = descElements.getLength() > 0 ? cleanDescription(descElements.item(0).getTextContent()) : null;

			if (name.isEmpty() || description == null) {
				continue;
			}

			attrMap.put(name, description);
		}
	}

	/**
	 * Returns the component type, converter ID, or validator ID from a tag element, whichever is present.
	 */
	private static String getTagTypeId(Element tagElement) {
		for (var elementName : new String[] { "component-type", "converter-id", "validator-id" }) {
			var elements = tagElement.getElementsByTagNameNS(TAGLIB_NAMESPACE, elementName);

			if (elements.getLength() > 0) {
				var text = elements.item(0).getTextContent().strip();

				if (!text.isEmpty()) {
					return text;
				}
			}
		}

		return null;
	}

	static String cleanDescription(String text) {
		if (text == null) {
			return null;
		}

		var cleanedText = HTML_TAG.matcher(text).replaceAll("");
		cleanedText = cleanedText.replaceAll("\\s+", " ").strip();

		return cleanedText.isEmpty() ? null : cleanedText;
	}

}
