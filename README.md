# Vdlgen

Tired of writing and syncing Facelets `.taglib.xml` files? Vdlgen might be what you're looking for!

Vdlgen is a compile-time annotation processor that generates Jakarta Faces Facelet tag library descriptor files (`*.taglib.xml`) from source code annotations and Javadoc. Instead of maintaining verbose XML by hand, you annotate your components, converters, validators, behaviors, tag handlers, and EL functions — and the processor generates a correct, complete `taglib.xml` during compilation. Your Javadoc doubles as your VDL documentation, so the two can never drift apart.

## Requirements

- Java 17+
- Jakarta Faces 4.0+

## Installation

Add the dependency with `provided` scope — it is only needed at compile time and must not be included at runtime:

```xml
<dependency>
    <groupId>org.omnifaces</groupId>
    <artifactId>vdlgen</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

No additional build plugin or compiler configuration is required. The annotation processor is discovered automatically via `META-INF/services/javax.annotation.processing.Processor`, as long as your `maven-compiler-plugin` does not have `<annotationProcessorPaths>` configured. If it does, then processor discovery from the compile classpath is disabled and you must explicitly add vdlgen there (see below).

If your components extend standard HTML component classes (e.g. `HtmlInputText`, `HtmlMessages`), the processor needs access to Jakarta Faces implementation-bundled taglib XML files to resolve inherited attribute descriptions. In that case, add the implementation jar to the annotation processor path. Below example uses Mojarra:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.omnifaces</groupId>
                <artifactId>vdlgen</artifactId>
                <version>1.0-SNAPSHOT</version>
            </path>
            <path>
                <groupId>org.glassfish</groupId>
                <artifactId>jakarta.faces</artifactId>
                <version>${mojarra.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

This is not needed when your components only extend base classes like `UIComponentBase` directly.

## Quick start

### 1. Declare a tag library

Place `@FaceletTagLibrary` on any class in your project (typically a configuration or startup class):

```java
@FaceletTagLibrary(
    id = "mylib",
    namespace = "http://example.com/mylib",
    description = "My custom component library"
)
public class MyLibConfig {
}
```

This will generate `META-INF/mylib.taglib.xml` during compilation.

The annotation is `@Repeatable`, so you can declare multiple tag libraries on the same class if your project ships more than one namespace.

### 2. Add component tags

Any `@FacesComponent` whose `namespace` matches the declared `@FaceletTagLibrary` namespace is automatically included:

```java
@FacesComponent(value = "com.example.MyPanel", namespace = "http://example.com/mylib")
public class MyPanel extends UIComponentBase {

    public static final String COMPONENT_TYPE = "com.example.MyPanel";

    @Override
    public String getFamily() {
        return "com.example";
    }

    /**
     * Sets the CSS style class applied to the panel container.
     */
    public void setStyleClass(String styleClass) {
        getStateHelper().put(PropertyKeys.styleClass, styleClass);
    }

    /**
     * Sets whether the panel content is collapsible. Defaults to {@code false}.
     */
    public void setCollapsible(boolean collapsible) {
        getStateHelper().put(PropertyKeys.collapsible, collapsible);
    }
}
```

Tag attributes are **auto-detected** from all public setter methods in the class hierarchy. The setter's Javadoc becomes the attribute description (with the leading "Sets " or "Set " prefix stripped). The attribute type is derived from the setter parameter type.

The tag name defaults to the decapitalized simple class name (here: `myPanel`). Override it with `@FacesComponent(tagName = "panel")`.

The class-level Javadoc becomes the tag description.

### 3. Add converter and validator tags

Annotate a `@FacesConverter` class with `@FacesConverterTag`:

```java
@FacesConverter("com.example.TrimConverter")
@FacesConverterTag(namespace = "http://example.com/mylib")
public class TrimConverter implements Converter<String> {

    /**
     * Sets the character to trim. Defaults to whitespace.
     */
    public void setCharacter(char character) { ... }

    @Override
    public String getAsObject(FacesContext context, UIComponent component, String value) { ... }

    @Override
    public String getAsString(FacesContext context, UIComponent component, String value) { ... }
}
```

Annotate a `@FacesValidator` class with `@FacesValidatorTag`:

```java
@FacesValidator("com.example.RequiredCheckboxValidator")
@FacesValidatorTag(namespace = "http://example.com/mylib")
public class RequiredCheckboxValidator implements Validator<Boolean> {

    @Override
    public void validate(FacesContext context, UIComponent component, Boolean value) throws ValidatorException { ... }
}
```

### 4. Add behavior tags

Annotate a `@FacesBehavior` class with `@FacesBehaviorTag`:

```java
@FacesBehavior("com.example.ConfirmBehavior")
@FacesBehaviorTag(namespace = "http://example.com/mylib")
public class ConfirmBehavior extends ClientBehaviorBase {

    /**
     * Sets the confirmation message to display.
     */
    public void setMessage(String message) { ... }
}
```

### 5. Add tag handler tags

Annotate a `TagHandler` subclass with `@FacesTagHandler` and its `TagAttribute` fields with `@FacesAttribute`:

```java
@FacesTagHandler(namespace = "http://example.com/mylib")
public class ImportConstantsHandler extends TagHandler {

    /**
     * The fully qualified class name of the constants class.
     */
    @FacesAttribute(required = true)
    private TagAttribute type;

    /**
     * The variable name to expose the constants map in the EL scope.
     */
    @FacesAttribute
    private TagAttribute var;

    public ImportConstantsHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException { ... }
}
```

For tag handlers that wrap a converter or validator, use `converterId` or `validatorId` (mutually exclusive):

```java
@FacesTagHandler(namespace = "http://example.com/mylib", validatorId = "com.example.MyValidator")
public class MyValidatorHandler extends ValidatorHandler {
    ...
}
```

### 6. Add EL functions

Register individual methods with `@FacesFunction`:

```java
public class MyFunctions {

    /**
     * Capitalizes the first letter of the given string.
     */
    @FacesFunction(namespace = "http://example.com/mylib")
    public static String capitalize(String input) {
        return input == null || input.isBlank() ? input : Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
```

Or register all public static non-void methods in a class at once with `@FacesFunctions`:

```java
@FacesFunctions(namespace = "http://example.com/mylib")
public class MyFunctions {

    /** Capitalizes the first letter. */
    public static String capitalize(String input) { ... }

    /** Joins a collection with a delimiter. */
    public static String join(Collection<?> items, String delimiter) { ... }

    // Non-public or void methods are automatically excluded.
    static void internal() { ... }
}
```

## Annotation reference

### `@FaceletTagLibrary`

Placed on a class to declare a tag library.

| Attribute     | Required | Default                                  | Description                                                                                          |
|---------------|----------|------------------------------------------|------------------------------------------------------------------------------------------------------|
| `id`          | yes      |                                          | Unique identifier. Used as base filename (`META-INF/{id}.taglib.xml`) and default `<short-name>`.    |
| `namespace`   | yes      |                                          | Unique namespace URI. Used as matching key for all other annotations.                                |
| `shortName`   | no       | same as `id`                             | The `<short-name>` in the generated XML. Override when the short name should differ from the id.     |
| `description` | no       | empty                                    | The `<description>` in the generated XML.                                                            |
| `version`     | no       | `V_4_1`                                  | Taglib schema version. Available values: `V_4_1`, `V_4_0`.                                          |

### `@FacesConverterTag`

Placed on a `@FacesConverter` class to include it as a converter tag.

| Attribute      | Required | Default                            | Description                                          |
|----------------|----------|------------------------------------|------------------------------------------------------|
| `namespace`    | yes      |                                    | Must match a declared `@FaceletTagLibrary` namespace. |
| `tagName`      | no       | decapitalized simple class name    | Override the generated tag name.                     |
| `handlerClass` | no       | `ConverterHandler`                 | Custom `ConverterHandler` class. Generates `<handler-class>`. |

### `@FacesValidatorTag`

Placed on a `@FacesValidator` class to include it as a validator tag.

| Attribute      | Required | Default                            | Description                                          |
|----------------|----------|------------------------------------|------------------------------------------------------|
| `namespace`    | yes      |                                    | Must match a declared `@FaceletTagLibrary` namespace. |
| `tagName`      | no       | decapitalized simple class name    | Override the generated tag name.                     |
| `handlerClass` | no       | `ValidatorHandler`                 | Custom `ValidatorHandler` class. Generates `<handler-class>`. |

### `@FacesBehaviorTag`

Placed on a `@FacesBehavior` class to include it as a behavior tag.

| Attribute      | Required | Default                            | Description                                          |
|----------------|----------|------------------------------------|------------------------------------------------------|
| `namespace`    | yes      |                                    | Must match a declared `@FaceletTagLibrary` namespace. |
| `tagName`      | no       | decapitalized simple class name    | Override the generated tag name.                     |
| `handlerClass` | no       | `BehaviorHandler`                  | Custom `BehaviorHandler` class. Generates `<handler-class>`. |

### `@FacesTagHandler`

Placed on a `TagHandler` subclass to include it as a tag.

| Attribute     | Required | Default                            | Description                                                                |
|---------------|----------|------------------------------------|----------------------------------------------------------------------------|
| `namespace`   | yes      |                                    | Must match a declared `@FaceletTagLibrary` namespace.                      |
| `tagName`     | no       | decapitalized simple class name    | Override the generated tag name.                                           |
| `converterId` | no       | empty                              | Generates a `<converter>` element. Mutually exclusive with `validatorId`.  |
| `validatorId` | no       | empty                              | Generates a `<validator>` element. Mutually exclusive with `converterId`.  |

### `@FacesAttribute`

Placed on a setter method (component) or `TagAttribute` field (tag handler) to declare or customize a tag attribute.

| Attribute         | Required | Default     | Description                                                                                                       |
|-------------------|----------|-------------|-------------------------------------------------------------------------------------------------------------------|
| `name`            | no       | derived     | Override the attribute name. Defaults to setter-derived name or field name.                                        |
| `description`     | no       | from Javadoc| Override the attribute description. Takes precedence over Javadoc.                                                |
| `required`        | no       | `false`     | Whether the attribute is required.                                                                                |
| `methodSignature` | no       | empty       | For `MethodExpression` attributes on component setters. Generates `<method-signature>` instead of `<type>`.       |
| `type`            | no       | `void.class`| For tag handler fields. Overrides the default type (`java.lang.String`). Ignored on component setters.            |

On component setters, this annotation is **optional** — all public setters are auto-detected. Use it only when you need to set `required`, `methodSignature`, or override the `description`.

On tag handler fields, this annotation is **required** to opt a field in as a tag attribute.

### `@FacesComponentConfig`

Placed on a `@FacesComponent` class to specify additional taglib configuration.

| Attribute          | Required | Default            | Description                                                  |
|--------------------|----------|--------------------|--------------------------------------------------------------|
| `componentHandler` | no       | `ComponentHandler` | Custom `ComponentHandler` class. Generates `<handler-class>`. |
| `rendererType`     | no       | empty              | Generates `<renderer-type>` inside the `<component>` element. |

### `@FacesFunction`

Placed on a public static non-void method to register it as an EL function.

| Attribute   | Required | Default      | Description                                          |
|-------------|----------|--------------|------------------------------------------------------|
| `namespace` | yes      |              | Must match a declared `@FaceletTagLibrary` namespace. |
| `name`      | no       | method name  | Override the function name.                          |

### `@FacesFunctions`

Placed on a class to register all its public static non-void methods as EL functions.

| Attribute   | Required | Default | Description                                          |
|-------------|----------|---------|------------------------------------------------------|
| `namespace` | yes      |         | Must match a declared `@FaceletTagLibrary` namespace. |

## How descriptions are resolved

Tag attribute descriptions are resolved from multiple sources, in order of precedence:

1. **`@FacesAttribute(description = "...")`** — explicit override, always wins.
2. **Setter Javadoc** (components) or **field Javadoc** (tag handlers) — the leading "Sets " or "Set " prefix is stripped and the first letter is capitalized. Javadoc `{@code ...}`, `{@link ...}`, and `{@literal ...}` inline tags are converted to readable text.
3. **Jakarta Faces taglib XML** — for inherited attributes from standard component types (e.g. `HtmlInputText`), the processor reads descriptions from Jakarta Faces implementation-bundled `faces.html.taglib.xml` and `faces.core.taglib.xml`, matched by component type.
4. **Built-in fallback descriptions** — for base class attributes like `UIComponent#id` and `UIComponent#rendered` whose component types are not present in the standard taglib files.

Tag and function descriptions are taken from the class-level or method-level Javadoc respectively.

## `MethodExpression` attributes

If your component has a `MethodExpression`-typed attribute (e.g. an action or listener), annotate the setter with `@FacesAttribute(methodSignature = "...")` so the processor generates a `<method-signature>` element instead of a `<type>` element:

```java
@FacesAttribute(methodSignature = "void itemSelected(jakarta.faces.event.AjaxBehaviorEvent)")
public void setItemSelectListener(MethodExpression itemSelectListener) {
    getStateHelper().put(PropertyKeys.itemSelectListener, itemSelectListener);
}
```

Without this annotation, the attribute would incorrectly get `<type>jakarta.el.MethodExpression</type>`.

## Inherited attributes

The processor walks the full class hierarchy of each component. Public setter methods from parent classes (e.g. `UIComponent`, `UIInput`, `HtmlInputText`) are included as attributes, with descriptions resolved from the sources listed above.

Certain framework-internal setters are automatically excluded (e.g. `UIComponent#setParent`, `UIInput#setSubmittedValue`, `UIComponentBase#setTransient`).

The standard `binding` attribute and method expression attributes like `validator`, `valueChangeListener`, `action`, and `actionListener` are automatically added where applicable based on the component's position in the class hierarchy.

## Generating VDL documentation

Once your `taglib.xml` is generated, you can turn it into browsable HTML documentation using [Vdldoc](https://github.com/omnifaces/vdldoc) — the Javadoc-style documentation generator for Facelets tag libraries. Since Vdlgen produces standard `taglib.xml` files, Vdldoc works out of the box. Together they form a complete pipeline: annotate your source code, compile to generate the `taglib.xml`, and run Vdldoc to publish polished VDL documentation.

For a live example, see the [OmniFaces VDL documentation](https://omnifaces.org/docs/vdldoc/current/).

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
