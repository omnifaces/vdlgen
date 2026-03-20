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

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * INTERNAL ONLY
 * <p>
 * Registry of standard Jakarta Faces attribute metadata for compiled parent
 * classes where the taglib reader cannot provide descriptions. This is needed
 * for base classes whose {@code COMPONENT_TYPE} is not present in Mojarra's
 * taglib XML files (only concrete HTML component types like
 * {@code jakarta.faces.HtmlInputText} are in the taglib, not abstract base
 * types like {@code jakarta.faces.Input}).
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
final class StandardFacesAttributes {

    /**
     * Setter names per declaring class that should be excluded from tag attributes.
     * These are internal framework setters not intended for use in Facelets, or
     * setters that exist on the class but are never rendered.
     */
    static final Map<String, Set<String>> EXCLUDED_SETTERS = Map.ofEntries(
            entry("jakarta.faces.component.UIComponent", Set.of("parent", "inView", "rendererType", "valueExpression")),
            entry("jakarta.faces.component.UIComponentBase", Set.of("parent", "transient", "rendererType")),
            entry("jakarta.faces.component.UIInput", Set.of("submittedValue", "localValueSet", "valid")),
            entry("jakarta.faces.component.UICommand", Set.of("actionExpression")),
            entry("jakarta.faces.component.UIViewAction", Set.of("actionExpression")),
            entry("jakarta.faces.component.UIData", Set.of("rowIndex", "rowData", "wrappedData", "localValueSet")),
            entry("jakarta.faces.component.UIForm", Set.of("prependId", "submitted")),
            entry("jakarta.faces.component.UIViewRoot", Set.of("inView")),
            entry("jakarta.faces.component.html.HtmlInputFile", Set.of("alt", "autocomplete", "maxlength", "readonly", "size")),
            entry("jakarta.faces.convert.NumberConverter", Set.of("transient")));

    /**
     * Represents an extra tag attribute that is not discoverable from setter
     * methods.
     */
    record ExtraAttribute(String name, String description, String type, boolean required, String methodSignature) {
    }

    private StandardFacesAttributes() {
        throw new AssertionError();
    }

    /**
     * Extra attributes per declaring class (FQN) that are not discoverable from
     * setter methods. These are handled through special mechanisms in Jakarta Faces
     * (value expression binding, method expression evaluation) and must be scoped
     * to the correct class hierarchy level. They cannot be reliably extracted from
     * the taglib XML because taglib entries are indexed by component type (not
     * class), multiple tags can share the same component type (e.g. {@code param}
     * and {@code viewParam} both use {@code jakarta.faces.Parameter}), and walking
     * all parent component types would inject attributes from unrelated tag
     * contexts.
     */
    static final Map<String, List<ExtraAttribute>> EXTRA_ATTRIBUTES = Map.of(
            "jakarta.faces.component.UIComponent",
            List.of(
                    new ExtraAttribute("binding",
                            "The ValueExpression linking this component to a property in a backing bean.",
                            "jakarta.faces.component.UIComponent", false, null)),

            "jakarta.faces.component.UIInput",
            List.of(
                    new ExtraAttribute("validator",
                            "MethodExpression representing a validator method that will be called during Process Validations"
                                    + " to perform correctness checks on the value of this component. The expression must evaluate to a"
                                    + " public method that takes FacesContext, UIComponent, and Object parameters, with a return type of void.",
                            null, false, "void validate(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, java.lang.Object)"),
                    new ExtraAttribute("valueChangeListener",
                            "MethodExpression representing a value change listener method that will be notified when a new value"
                                    + " has been set for this input component. The expression must evaluate to a public method that takes"
                                    + " a ValueChangeEvent parameter, with a return type of void, or to a public method that takes no"
                                    + " arguments with a return type of void.",
                            null, false, "void valueChange(jakarta.faces.event.ValueChangeEvent)")),

            "jakarta.faces.component.UICommand",
            List.of(
                    new ExtraAttribute("action",
                            "MethodExpression representing the application action to invoke when this component is activated by"
                                    + " the user. The expression must evaluate to a public method that takes no parameters, and returns"
                                    + " an Object (the logical outcome) which is passed to the NavigationHandler for this application.",
                            null, false, "java.lang.Object action()"),
                    new ExtraAttribute("actionListener",
                            "MethodExpression representing an action listener method that will be notified when this component"
                                    + " is activated by the user. The expression must evaluate to a public method that takes an"
                                    + " ActionEvent parameter, with a return type of void.",
                            null, false, "void processAction(jakarta.faces.event.ActionEvent)")),

            "jakarta.faces.component.UIViewAction",
            List.of(
                    new ExtraAttribute("action",
                            "MethodExpression representing the application action to invoke when this component is activated."
                                    + " The expression must evaluate to a public method that takes no parameters, and returns"
                                    + " an Object (the logical outcome) which is passed to the NavigationHandler for this application.",
                            null, false, "java.lang.Object action()"),
                    new ExtraAttribute("actionListener",
                            "MethodExpression representing an action listener method that will be notified when this component"
                                    + " is activated. The expression must evaluate to a public method that takes an"
                                    + " ActionEvent parameter, with a return type of void.",
                            null, false, "void processAction(jakarta.faces.event.ActionEvent)"),
                    new ExtraAttribute("if",
                            "Invoke the application action only when this attribute evaluates true during the specified phase."
                                    + " The default is true.",
                            "boolean", false, null)));

    /**
     * Descriptions for setter-based attributes from compiled Jakarta Faces parent
     * classes whose {@code COMPONENT_TYPE} is not present in the taglib XML. Key
     * format: {@code "fully.qualified.ClassName#attributeName"}.
     */
    static final Map<String, String> DESCRIPTIONS = Map.ofEntries(

            // UIComponent
            entry("jakarta.faces.component.UIComponent#id",
                    "The component identifier for this component. This value must be unique within the closest parent component that is a naming container."),
            entry("jakarta.faces.component.UIComponent#rendered",
                    "Flag indicating whether or not this component should be rendered (during Render Response Phase), or processed on any subsequent form submit. The default value for this property is true."),

            // UIViewParameter — needed because Mojarra's faces.core.taglib.xml incorrectly uses component-type
            // "jakarta.faces.Parameter" (UIParameter) for the viewParam tag instead of "jakarta.faces.ViewParameter"
            // (UIViewParameter), so the taglib reader can't find these descriptions via the COMPONENT_TYPE hierarchy.
            // TODO: remove once fixed in Mojarra.
            entry("jakarta.faces.component.UIViewParameter#name",
                    "The name of the request parameter from which the value for this component is retrieved on an initial request or to override the stored value on a postback."),

            // UIMessages — needed because Mojarra's faces.html.taglib.xml is missing the redisplay and role
            // attributes for the h:messages tag, so the taglib reader can't find these descriptions.
            // TODO: remove once fixed in Mojarra.
            entry("jakarta.faces.component.UIMessages#redisplay",
                    "Flag indicating whether already-handled messages should be redisplayed. The default value is true."),
            entry("jakarta.faces.component.UIMessages#role",
                    "Per the WAI-ARIA spec and its relationship to HTML5, every HTML element may have a \"role\" attribute"
                            + " whose value must be passed through unmodified on the element on which it is declared."));

    /**
     * Returns the description for a setter-based attribute from a compiled parent
     * class.
     *
     * @param declaringClassFqn The fully qualified name of the class declaring the
     *                          setter.
     * @param attributeName     The attribute name.
     * @return The description, or {@code null} if not found.
     */
    static String getDescription(String declaringClassFqn, String attributeName) {
        return DESCRIPTIONS.get(declaringClassFqn + "#" + attributeName);
    }

}
