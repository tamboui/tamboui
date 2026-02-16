package dev.tamboui.docs.snippets;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Flex;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.widgets.input.TextInputState;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for styling.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings("unused")
public class StylingSnippets {

    // Stub methods for application-specific code
    private Element myApp() {
        return text("My Application");
    }

    private Element header() {
        return text("Header");
    }

    private Element content() {
        return text("Content");
    }

    private void requestRedraw() {
        // stub
    }

    // tag::programmatic-styling[]
    Element programmaticExample = column(
        text("Hello").bold().cyan(),
        panel("Title", () -> text("content")).rounded().borderColor(Color.BLUE)
    );
    // end::programmatic-styling[]

    void styleEngineUsage() throws IOException {
        // tag::style-engine[]
        StyleEngine engine = StyleEngine.create();

        // Add inline CSS
        engine.addStylesheet("Panel { border-type: rounded; }");

        // Load named themes from classpath
        engine.loadStylesheet("dark", "/themes/dark.tcss");
        engine.loadStylesheet("light", "/themes/light.tcss");

        // Switch themes at runtime
        engine.setActiveStylesheet("dark");
        // end::style-engine[]
    }

    void gridTemplateAreas() {
        Element content = text("Main content");
        // tag::grid-template-areas[]
        // Combine CSS template with programmatic area assignment
        Element gridExample = grid()
            .addClass("dashboard")
            .area("header", text("Dashboard").bold())
            .area("nav", list("Menu 1", "Menu 2"))
            .area("main", content)
            .area("footer", text("Status").dim());
        // end::grid-template-areas[]
    }

    void flexProgrammaticUsage() {
        // tag::flex-programmatic[]
        // Horizontal flex (Row)
        Element horizontalFlex = row(
            panel(() -> text("Left")).rounded().length(10),
            panel(() -> text("Center")).rounded().length(10),
            panel(() -> text("Right")).rounded().length(10)
        ).flex(Flex.SPACE_BETWEEN);

        // Vertical flex (Column)
        Element verticalFlex = column(
            panel(() -> text("Top")).rounded().length(3),
            panel(() -> text("Middle")).rounded().length(3),
            panel(() -> text("Bottom")).rounded().length(3)
        ).flex(Flex.CENTER);

        // Nested layouts with different flex modes
        Element nestedFlex = column(
            row(
                text("Item A").length(8),
                text("Item B").length(8)
            ).flex(Flex.START).length(3),

            row(
                text("Item C").length(8),
                text("Item D").length(8)
            ).flex(Flex.END).length(3)
        );
        // end::flex-programmatic[]
    }

    void flexCssUsage() {
        // tag::flex-css-usage[]
        // Elements pick up flex from CSS classes
        Element toolbarRow = row(
            text("New"),
            text("Open"),
            text("Save")
        ).addClass("toolbar-row");
        // end::flex-css-usage[]
    }

    void flexVsFill() {
        // tag::flex-vs-fill[]
        // WITHOUT fill() - flex positions fixed-size items
        Element withoutFill = row(
            text("A").length(5),
            text("B").length(5),
            text("C").length(5)
        ).flex(Flex.SPACE_BETWEEN);  // Items stay 5 wide, spread out

        // WITH fill() - item grows, flex has less effect
        Element withFill = row(
            text("A").length(5),
            text("B").fill(),          // Takes all remaining space
            text("C").length(5)
        ).flex(Flex.CENTER);            // Only affects tiny leftover gaps
        // end::flex-vs-fill[]
    }

    void flexCenteredDialog() {
        // tag::flex-centered-dialog[]
        Element centeredDialog = column(
            panel("Confirm",
                column(
                    text("Are you sure?"),
                    row(
                        text("[ Yes ]").green(),
                        text("[ No ]").red()
                    ).flex(Flex.SPACE_AROUND).spacing(2)
                )
            ).rounded().length(10)
        ).flex(Flex.CENTER);
        // end::flex-centered-dialog[]
    }

    void flexApplicationLayout() {
        // tag::flex-application-layout[]
        Element appLayout = column(
            // Header: left-aligned title
            row(
                text("My App").bold(),
                text("v1.0").dim()
            ).flex(Flex.START).spacing(2).length(3),

            // Content: fills available space
            panel(() -> text("Main content...")).fill(),

            // Footer: spread items across
            row(
                text("Ready").green(),
                text("Line 42, Col 15").dim(),
                text("UTF-8").dim()
            ).flex(Flex.SPACE_BETWEEN).length(1)
        );
        // end::flex-application-layout[]
    }

    void flexToolbarCss() {
        // tag::flex-toolbar-css[]
        // Main toolbar with items spread across
        Element mainToolbar = row(
            text("File"),
            text("Edit"),
            text("View"),
            text("Help")
        ).addClass("toolbar");

        // Toolbar section with items grouped left
        Element leftToolbar = row(
            text("New"),
            text("Open")
        ).addClass("toolbar-left");
        // end::flex-toolbar-css[]
    }

    void flexWithSpacing() {
        // tag::flex-with-spacing[]
        // spacing=1 creates 1-cell gaps, flex centers the whole group
        Element spacedRow = row(
            text("A"),
            text("B"),
            text("C")
        ).spacing(1).flex(Flex.CENTER);
        // end::flex-with-spacing[]
    }

    void flexCommonPatterns() {
        // tag::flex-common-patterns[]
        // Left-aligned button group
        Element leftAligned = row(
            text(" Save ").bold().black().onGreen(),
            text(" Cancel ").bold()
        ).flex(Flex.START).spacing(1);

        // Right-aligned status
        Element rightAligned = row(
            text("Ready").green()
        ).flex(Flex.END);

        // Split layout: title left, controls right
        Element splitLayout = row(
            text("Settings").bold().length(20),
            spacer(),  // Takes remaining space
            text(" OK ").bold().length(6),
            text(" Cancel ").bold().length(8)
        ).flex(Flex.START).spacing(1);

        // Perfectly centered modal
        Element centeredModal = column(
            spacer(),
            row(
                spacer(),
                panel("Notice",
                    () -> text("Operation completed")
                ).rounded().length(30),
                spacer()
            ),
            spacer()
        );
        // end::flex-common-patterns[]
    }

    void layoutCssExample() {
        // tag::layout-css-example[]
        Element layout = column(
            panel(() -> header()).addClass("header-panel"),
            panel(() -> content()).addClass("main-content"),
            panel(() -> row(
                text("Status: ").addClass("title"),
                text("Ready").addClass("dim")
            ).addClass("footer-row")).addClass("footer-panel")
        );
        // end::layout-css-example[]
    }

    void propertyDefinitionExample() {
        // tag::property-definition[]
        // Simple non-inheritable property
        PropertyDefinition<Color> GAUGE_COLOR =
            PropertyDefinition.of("gauge-color", ColorConverter.INSTANCE);

        // Inheritable property with default value
        PropertyDefinition<Color> COLOR =
            PropertyDefinition.builder("color", ColorConverter.INSTANCE)
                .inheritable()
                .build();
        // end::property-definition[]
    }

    // tag::gauge-property[]
    public static final PropertyDefinition<Color> GAUGE_COLOR =
        PropertyDefinition.of("gauge-color", ColorConverter.INSTANCE);
    // end::gauge-property[]

    void propertyRegistryExample() {
        PropertyDefinition<Color> MY_PROPERTY = PropertyDefinition.of("my-property", ColorConverter.INSTANCE);
        PropertyDefinition<Color> PROPERTY_A = PropertyDefinition.of("property-a", ColorConverter.INSTANCE);
        PropertyDefinition<Color> PROPERTY_B = PropertyDefinition.of("property-b", ColorConverter.INSTANCE);
        PropertyDefinition<Color> PROPERTY_C = PropertyDefinition.of("property-c", ColorConverter.INSTANCE);
        // tag::property-registry[]
        // Register a single property
        PropertyRegistry.register(MY_PROPERTY);

        // Register multiple properties at once
        PropertyRegistry.registerAll(PROPERTY_A, PROPERTY_B, PROPERTY_C);
        // end::property-registry[]
    }

    // tag::widget-custom-properties[]
    public static class MyWidget {
        public static final PropertyDefinition<Color> BAR_COLOR =
            PropertyDefinition.of("bar-color", ColorConverter.INSTANCE);
        public static final PropertyDefinition<Integer> BAR_WIDTH =
            PropertyDefinition.of("bar-width", intConverter());

        static {
            PropertyRegistry.registerAll(BAR_COLOR, BAR_WIDTH);
        }

        private static PropertyConverter<Integer> intConverter() {
            return value -> Optional.of(Integer.parseInt(value));
        }
    }
    // end::widget-custom-properties[]

    void inheritedPropertiesExample() {
        // tag::inherited-properties[]
        // All text inside the sidebar automatically gets cyan + dim styling
        Element sidebar = panel(() -> column(
            text("Menu"),           // cyan, dim (inherited from Panel)
            text("Dashboard"),      // cyan, dim (inherited from Panel)
            text("Settings").bold() // cyan, dim + bold (inherits color, adds bold)
        )).addClass("sidebar");
        // end::inherited-properties[]
    }

    void toolkitDslStyling() {
        TextInputState usernameState = new TextInputState("");
        TextInputState passwordState = new TextInputState("");
        // tag::toolkit-dsl-styling[]
        Element settingsPanel = panel("Settings",
            () -> column(
                text("Username").addClass("label"),
                textInput(usernameState).id("username-input"),
                text("Password").addClass("label"),
                textInput(passwordState).id("password-input").addClass("secret")
            )
        )
        .id("settings-panel")
        .addClass("primary");
        // end::toolkit-dsl-styling[]
    }

    void toolkitRunnerWithStyleEngine() throws Exception {
        // tag::toolkit-runner-style-engine[]
        StyleEngine engine = StyleEngine.create();
        engine.loadStylesheet("dark", "/themes/dark.tcss");
        engine.setActiveStylesheet("dark");

        try (var runner = ToolkitRunner.builder()
                .styleEngine(engine)
                .build()) {
            runner.run(() -> myApp());
        }
        // end::toolkit-runner-style-engine[]
    }

    // tag::implementing-styleable[]
    public static class MyStylableWidget implements Styleable {
        private String id;
        private Set<String> classes = new HashSet<>();

        @Override
        public String styleType() {
            return "MyWidget";  // Used for type selectors
        }

        @Override
        public Optional<String> cssId() {
            return Optional.ofNullable(id);
        }

        @Override
        public Set<String> cssClasses() {
            return classes;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();  // Or return parent for descendant selectors
        }
    }
    // end::implementing-styleable[]

    void resolveAndApplyStyles() {
        StyleEngine engine = StyleEngine.create();
        MyStylableWidget widget = new MyStylableWidget();
        // tag::resolve-apply-styles[]
        CssStyleResolver resolved = engine.resolve(widget);

        Style style = Style.EMPTY;
        if (resolved.foreground().isPresent()) {
            style = style.fg(resolved.foreground().get());
        }
        if (resolved.background().isPresent()) {
            style = style.bg(resolved.background().get());
        }
        for (var modifier : resolved.modifiers()) {
            style = style.addModifier(modifier);
        }
        // end::resolve-apply-styles[]
    }

    // tag::theme-switching[]
    void onToggleTheme(StyleEngine engine) {
        String current = engine.getActiveStylesheet().orElse("dark");
        String next = "dark".equals(current) ? "light" : "dark";
        engine.setActiveStylesheet(next);
    }
    // end::theme-switching[]

    void styleChangeListener() {
        StyleEngine engine = StyleEngine.create();
        // tag::style-change-listener[]
        engine.addChangeListener(() -> {
            // Styles changed, trigger redraw
            requestRedraw();
        });
        // end::style-change-listener[]
    }
}
