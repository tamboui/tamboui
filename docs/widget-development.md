# Widget Development Guide

This guide explains how to create TamboUI widgets with support for external styling systems. It covers the style property architecture and best practices for building reusable, style-aware widgets.

## Table of Contents

1. [Widget Basics](#widget-basics)
2. [The Style Property System](#the-style-property-system)
3. [Making Widgets Style-Aware](#making-widgets-style-aware)
4. [Positional Styling](#positional-styling)
5. [Wrapping Widgets in Toolkit Elements](#wrapping-widgets-in-toolkit-elements)

---

## Widget Basics

TamboUI provides two widget interfaces:

### Stateless Widgets

```java
public interface Widget {
    void render(Rect area, Buffer buffer);
}
```

Use for widgets that don't maintain state between renders (e.g., Block, Paragraph).

### Stateful Widgets

```java
public interface StatefulWidget<S> {
    void render(Rect area, Buffer buffer, S state);
}
```

Use for widgets that need external state (e.g., ListWidget with ListState for scroll position).

### Basic Widget Example

```java
public final class SimpleWidget implements Widget {

    private final String text;
    private final Style style;

    public SimpleWidget(String text, Style style) {
        this.text = text;
        this.style = style;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }
        buffer.setString(area.left(), area.top(), text, style);
    }
}
```

---

## The Style Property System

TamboUI's style property system allows widgets to resolve styling from external sources (like CSS engines) while supporting programmatic overrides. The system consists of four key abstractions:

### PropertyKey

A `PropertyKey<T>` identifies a style property by name and knows how to convert string values to typed values:

```java
// Standard property keys in dev.tamboui.style.StandardPropertyKeys
PropertyKey<Color> COLOR = PropertyKey.of("color", ColorConverter.INSTANCE);
PropertyKey<Color> BACKGROUND = PropertyKey.of("background", ColorConverter.INSTANCE);
PropertyKey<Color> BORDER_COLOR = PropertyKey.of("border-color", ColorConverter.INSTANCE);
PropertyKey<Alignment> TEXT_ALIGN = PropertyKey.of("text-align", AlignmentConverter.INSTANCE);

// Widget-specific property keys (in the widget class)
PropertyKey<BorderType> BORDER_TYPE = PropertyKey.of("border-type", BorderTypeConverter.INSTANCE);
PropertyKey<Overflow> TEXT_OVERFLOW = PropertyKey.of("text-overflow", OverflowConverter.INSTANCE);
```

### PropertyConverter

A `PropertyConverter<T>` transforms string values into typed values:

```java
public final class ColorConverter implements PropertyConverter<Color> {

    public static final ColorConverter INSTANCE = new ColorConverter();

    private ColorConverter() {
    }

    @Override
    public Optional<Color> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        // Handle hex colors, rgb(), named colors, etc.
        // ...
        return Optional.empty();
    }
}
```

### PropertyResolver

A `PropertyResolver` provides property values by key. This is the interface that external styling systems implement:

```java
public interface PropertyResolver {
    <T> Optional<T> get(PropertyKey<T> key);

    static PropertyResolver empty() {
        return key -> Optional.empty();
    }
}
```

The CSS module provides `CssStyleResolver` which implements this interface. Widgets don't need to know about CSS - they or more often their Builder just accept a `PropertyResolver` which is used at creation time to apply styles.

### StyledProperty

A `StyledProperty<T>` holds a property value with a resolution order: **programmatic value > resolved value > default value**.

```java
// In the builder
private final StyledProperty<Color> borderColor =
        StyledProperty.of(StandardPropertyKeys.BORDER_COLOR, null, () -> styleResolver);

// Setting programmatically (highest priority)
public Builder borderColor(Color color) {
    this.borderColor.set(color);
    return this;
}

// Resolving the final value
Color resolved = borderColor.resolve();  // Returns: programmatic || external || default
```

The third parameter `() -> styleResolver` is a `Supplier` that provides the resolver. This binding ensures `.resolve()` works without passing the resolver each time.

---

## Making Widgets Style-Aware

Here's how to add style awareness to a widget using the builder pattern:

### Step 1: Define Property Keys

Add widget-specific property keys if needed:

```java
public final class MyWidget implements Widget {

    // Widget-specific property key
    public static final PropertyKey<MyEnum> MY_PROPERTY =
            PropertyKey.of("my-property", MyEnumConverter.INSTANCE);

    // ... rest of widget
}
```

### Step 2: Add StyledProperties to the Builder

```java
public static final class Builder {
    // The resolver that provides external style values
    private PropertyResolver styleResolver = PropertyResolver.empty();

    // Style-aware properties bound to the resolver via Supplier
    private final StyledProperty<Color> borderColor =
            StyledProperty.of(StandardPropertyKeys.BORDER_COLOR, null, () -> styleResolver);
    private final StyledProperty<Color> background =
            StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
    private final StyledProperty<BorderType> borderType =
            StyledProperty.of(BORDER_TYPE, BorderType.PLAIN, () -> styleResolver);

    // ... rest of builder
}
```

### Step 3: Add Builder Methods

```java
/**
 * Sets the property resolver for style-aware properties.
 * <p>
 * When set, properties like {@code border-color} and {@code background}
 * will be resolved if not set programmatically.
 *
 * @param resolver the property resolver
 * @return this builder
 */
public Builder styleResolver(PropertyResolver resolver) {
    this.styleResolver = resolver != null ? resolver : PropertyResolver.empty();
    return this;
}

/**
 * Sets the border color programmatically.
 * <p>
 * This takes precedence over values from the style resolver.
 *
 * @param color the border color
 * @return this builder
 */
public Builder borderColor(Color color) {
    this.borderColor.set(color);
    return this;
}

/**
 * Sets the background color programmatically.
 *
 * @param color the background color
 * @return this builder
 */
public Builder background(Color color) {
    this.background.set(color);
    return this;
}
```

### Step 4: Resolve Properties in the Constructor

```java
private MyWidget(Builder builder) {
    // Resolve style-aware properties
    Color resolvedBorderColor = builder.borderColor.resolve();
    Color resolvedBackground = builder.background.resolve();
    BorderType resolvedBorderType = builder.borderType.resolve();

    // Use resolved values
    this.borderColor = resolvedBorderColor;
    this.background = resolvedBackground;
    this.borderType = resolvedBorderType;
}
```

### Complete Example: Block Widget (simplified)

```java
public final class Block implements Widget {

    public static final PropertyKey<BorderType> BORDER_TYPE =
            PropertyKey.of("border-type", BorderTypeConverter.INSTANCE);

    private final BorderType borderType;
    private final Color borderColor;
    private final Style style;

    private Block(Builder builder) {
        this.borderType = builder.borderType.resolve();

        // Build the style with resolved colors
        Color resolvedBorderColor = builder.borderColor.resolve();
        Color resolvedBg = builder.background.resolve();
        Color resolvedFg = builder.foreground.resolve();

        Style s = builder.style;
        if (resolvedBg != null) {
            s = s.bg(resolvedBg);
        }
        if (resolvedFg != null) {
            s = s.fg(resolvedFg);
        }
        this.style = s;
        this.borderColor = resolvedBorderColor != null ? resolvedBorderColor : s.fg();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        // Use this.borderType, this.borderColor, this.style
        // ...
    }

    public static final class Builder {
        private PropertyResolver styleResolver = PropertyResolver.empty();
        private Style style = Style.EMPTY;

        private final StyledProperty<BorderType> borderType =
                StyledProperty.of(BORDER_TYPE, BorderType.PLAIN, () -> styleResolver);
        private final StyledProperty<Color> borderColor =
                StyledProperty.of(StandardPropertyKeys.BORDER_COLOR, null, () -> styleResolver);
        private final StyledProperty<Color> background =
                StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
        private final StyledProperty<Color> foreground =
                StyledProperty.of(StandardPropertyKeys.COLOR, null, () -> styleResolver);

        public Builder styleResolver(PropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : PropertyResolver.empty();
            return this;
        }

        public Builder borderType(BorderType type) {
            this.borderType.set(type);
            return this;
        }

        public Builder borderColor(Color color) {
            this.borderColor.set(color);
            return this;
        }

        public Builder background(Color color) {
            this.background.set(color);
            return this;
        }

        public Builder foreground(Color color) {
            this.foreground.set(color);
            return this;
        }

        public Builder style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        public Block build() {
            return new Block(this);
        }
    }
}
```

### Usage

```java
// Without external styling (programmatic only)
Block block = Block.builder()
    .borderType(BorderType.ROUNDED)
    .borderColor(Color.CYAN)
    .build();

// With CSS styling
CssStyleResolver cssResolver = styleEngine.resolve(element);
Block block = Block.builder()
    .styleResolver(cssResolver)  // CSS provides border-type, border-color
    .build();

// Mixed: CSS provides defaults, programmatic overrides
Block block = Block.builder()
    .styleResolver(cssResolver)
    .borderColor(Color.RED)  // Overrides CSS value
    .build();
```

---

## Positional Styling

For widgets that display multiple items (like lists), you may want to style items based on their position (e.g., alternating row colors, first/last item styles).

### Using itemStyleResolver

Add a callback that receives the item index and total count:

```java
public static final class Builder {
    private BiFunction<Integer, Integer, Style> itemStyleResolver;

    /**
     * Sets a function to resolve styles for each item based on position.
     * <p>
     * The function receives the item index (0-based) and total item count,
     * and returns a Style to apply to that item.
     *
     * @param resolver function that takes (index, totalCount) and returns a Style
     * @return this builder
     */
    public Builder itemStyleResolver(BiFunction<Integer, Integer, Style> resolver) {
        this.itemStyleResolver = resolver;
        return this;
    }
}
```

Apply styles in the constructor:

```java
private ListWidget(Builder builder) {
    if (builder.itemStyleResolver != null) {
        List<ListItem> styledItems = new ArrayList<>(builder.items.size());
        int total = builder.items.size();
        for (int i = 0; i < total; i++) {
            ListItem item = builder.items.get(i);
            Style itemStyle = builder.itemStyleResolver.apply(i, total);
            if (itemStyle != null && !itemStyle.equals(Style.EMPTY)) {
                styledItems.add(item.style(item.style().patch(itemStyle)));
            } else {
                styledItems.add(item);
            }
        }
        this.items = listCopyOf(styledItems);
    } else {
        this.items = listCopyOf(builder.items);
    }
}
```

### Usage with CSS Pseudo-Classes

```java
ListWidget list = ListWidget.builder()
    .items(items)
    .itemStyleResolver((index, total) -> {
        // Build pseudo-class state for CSS resolution
        PseudoClassState state = PseudoClassState.NONE
            .withFirstChild(index == 0)
            .withLastChild(index == total - 1)
            .withNthChild(index + 1);  // CSS uses 1-based indexing

        // Resolve style with pseudo-class state
        return styleEngine.resolve(itemElement, state).toStyle();
    })
    .build();
```

### Simple Odd/Even Styling

```java
ListWidget list = ListWidget.builder()
    .items(items)
    .itemStyleResolver((index, total) ->
        index % 2 == 0
            ? Style.EMPTY.bg(Color.rgb(30, 30, 30))
            : Style.EMPTY.bg(Color.rgb(40, 40, 40))
    )
    .build();
```

---

## Wrapping Widgets in Toolkit Elements

The toolkit module provides a higher-level API with elements that wrap widgets. Here's how to create a toolkit element for your widget.

### Extend StyledElement

```java
public final class MyElement extends StyledElement<MyElement> {

    private final String content;
    private BorderType borderType;

    public MyElement(String content) {
        this.content = content;
    }

    /**
     * Sets the border type programmatically.
     */
    public MyElement borderType(BorderType type) {
        this.borderType = type;
        return this;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        // Get the current style (already resolved by StyledElement.render())
        Style effectiveStyle = context.currentStyle();

        // Get the property resolver for this element
        PropertyResolver resolver = context.resolveStyle(this)
                .map(r -> (PropertyResolver) r)
                .orElse(PropertyResolver.empty());

        // Build the widget with style resolver
        MyWidget.Builder widgetBuilder = MyWidget.builder()
                .styleResolver(resolver)
                .style(effectiveStyle);

        // Apply programmatic overrides
        if (borderType != null) {
            widgetBuilder.borderType(borderType);
        }

        // Render the widget
        frame.renderWidget(widgetBuilder.build(), area);
    }
}
```

### Key Points

1. **Extend `StyledElement<T>`**: Provides base styling methods like `.bold()`, `.cyan()`, etc.

2. **Override `renderContent()`**: This is called by `StyledElement.render()` after style resolution.

3. **Use `context.currentStyle()`**: Returns the resolved style for this element.

4. **Use `context.resolveStyle(this)`**: Returns the CSS resolver (if CSS is configured).

5. **Pass resolver to widget**: The widget's `styleResolver()` method accepts the resolver.

6. **Apply programmatic overrides**: Set values after the resolver to override CSS.

### Adding to Toolkit Factory

Add a factory method in the `Toolkit` class:

```java
public static MyElement myWidget(String content) {
    return new MyElement(content);
}
```

Users can then use it with the fluent API:

```java
import static dev.tamboui.toolkit.Toolkit.*;

myWidget("Hello")
    .bold()
    .cyan()
    .borderType(BorderType.ROUNDED)
```

---

## Summary

| Concept | Purpose |
|---------|---------|
| `PropertyKey<T>` | Identifies a property by name + provides conversion |
| `PropertyConverter<T>` | Converts string values to typed values |
| `PropertyResolver` | Interface for external styling systems to provide values |
| `StyledProperty<T>` | Holds value with resolution: programmatic > external > default |
| `styleResolver()` | Builder method to accept external style source |
| `itemStyleResolver()` | Callback for positional styling in list-like widgets |

### Resolution Order

1. **Programmatic value** (set via builder method like `.borderColor(Color.RED)`)
2. **External value** (from `PropertyResolver` / CSS)
3. **Default value** (specified when creating `StyledProperty`)

### Best Practices

- Use `StandardPropertyKeys` for common properties (color, background, border-color)
- Define widget-specific `PropertyKey`s as public static fields on the widget class
- Always provide a default value for `StyledProperty` when the property is required
- Use `null` as the default when the property is optional
- Accept `PropertyResolver` via `styleResolver()` method, not in constructor
- Keep widgets decoupled from specific styling systems (CSS, themes, etc.)
