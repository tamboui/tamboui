/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class OnActionProcessorTest {

    @Test
    void generatesRegistrarForAnnotatedMethods() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "test.MyHandler",
                "package test;\n" +
                "\n" +
                "import dev.tamboui.annotations.bindings.OnAction;\n" +
                "import dev.tamboui.tui.event.Event;\n" +
                "\n" +
                "public class MyHandler {\n" +
                "    @OnAction(\"save\")\n" +
                "    void save(Event event) {}\n" +
                "\n" +
                "    @OnAction(\"quit\")\n" +
                "    void quit(Event event) {}\n" +
                "}\n"
        );

        Compilation compilation = javac()
                .withProcessors(new OnActionProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.MyHandler_ActionHandlerRegistrar")
                .contentsAsUtf8String()
                .contains("handler.on(\"save\", target::save)");
        assertThat(compilation)
                .generatedSourceFile("test.MyHandler_ActionHandlerRegistrar")
                .contentsAsUtf8String()
                .contains("handler.on(\"quit\", target::quit)");
        assertThat(compilation)
                .generatedSourceFile("test.MyHandler_ActionHandlerRegistrar")
                .contentsAsUtf8String()
                .contains("implements ActionHandlerRegistrar<MyHandler>");
    }

    @Test
    void generatesServiceLoaderEntry() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "test.MyHandler",
                "package test;\n" +
                "\n" +
                "import dev.tamboui.annotations.bindings.OnAction;\n" +
                "import dev.tamboui.tui.event.Event;\n" +
                "\n" +
                "public class MyHandler {\n" +
                "    @OnAction(\"action\")\n" +
                "    void handle(Event event) {}\n" +
                "}\n"
        );

        Compilation compilation = javac()
                .withProcessors(new OnActionProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(
                        javax.tools.StandardLocation.CLASS_OUTPUT,
                        "META-INF/services/dev.tamboui.tui.bindings.ActionHandlerRegistrar")
                .contentsAsUtf8String()
                .contains("test.MyHandler_ActionHandlerRegistrar");
    }

    @Test
    void handlesMultipleClassesInSameCompilation() {
        JavaFileObject source1 = JavaFileObjects.forSourceString(
                "test.Handler1",
                "package test;\n" +
                "\n" +
                "import dev.tamboui.annotations.bindings.OnAction;\n" +
                "import dev.tamboui.tui.event.Event;\n" +
                "\n" +
                "public class Handler1 {\n" +
                "    @OnAction(\"action1\")\n" +
                "    void handle(Event event) {}\n" +
                "}\n"
        );

        JavaFileObject source2 = JavaFileObjects.forSourceString(
                "test.Handler2",
                "package test;\n" +
                "\n" +
                "import dev.tamboui.annotations.bindings.OnAction;\n" +
                "import dev.tamboui.tui.event.Event;\n" +
                "\n" +
                "public class Handler2 {\n" +
                "    @OnAction(\"action2\")\n" +
                "    void handle(Event event) {}\n" +
                "}\n"
        );

        Compilation compilation = javac()
                .withProcessors(new OnActionProcessor())
                .compile(source1, source2);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.Handler1_ActionHandlerRegistrar");
        assertThat(compilation)
                .generatedSourceFile("test.Handler2_ActionHandlerRegistrar");
    }

    @Test
    void rejectsMethodWithNoParameters() {
        JavaFileObject source = JavaFileObjects.forSourceString(
                "test.MyHandler",
                "package test;\n" +
                "\n" +
                "import dev.tamboui.annotations.bindings.OnAction;\n" +
                "\n" +
                "public class MyHandler {\n" +
                "    @OnAction(\"action\")\n" +
                "    void handle() {}\n" +
                "}\n"
        );

        Compilation compilation = javac()
                .withProcessors(new OnActionProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining("@OnAction methods must have exactly one parameter of type Event");
    }

    @Test
    void usesConstantValueForActionName() {
        // When @OnAction uses a constant like Actions.QUIT, the processor
        // should resolve the string value
        JavaFileObject source = JavaFileObjects.forSourceString(
                "test.MyHandler",
                "package test;\n" +
                "\n" +
                "import dev.tamboui.annotations.bindings.OnAction;\n" +
                "import dev.tamboui.tui.bindings.Actions;\n" +
                "import dev.tamboui.tui.event.Event;\n" +
                "\n" +
                "public class MyHandler {\n" +
                "    @OnAction(Actions.QUIT)\n" +
                "    void quit(Event event) {}\n" +
                "}\n"
        );

        Compilation compilation = javac()
                .withProcessors(new OnActionProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.MyHandler_ActionHandlerRegistrar")
                .contentsAsUtf8String()
                .contains("handler.on(\"quit\", target::quit)");
    }
}
