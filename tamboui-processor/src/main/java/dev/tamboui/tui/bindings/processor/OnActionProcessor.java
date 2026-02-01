/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings.processor;

import dev.tamboui.annotations.bindings.OnAction;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor for {@link OnAction} annotations.
 * <p>
 * Generates {@code ActionHandlerRegistrar} implementations and
 * META-INF/services entries for automatic discovery via ServiceLoader.
 */
@SupportedAnnotationTypes("dev.tamboui.annotations.bindings.OnAction")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class OnActionProcessor extends AbstractProcessor {

    /**
     * Creates a new OnAction annotation processor.
     */
    public OnActionProcessor() {
    }

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private TypeMirror eventType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        // Get Event type for validation
        TypeElement eventElement = elementUtils.getTypeElement("dev.tamboui.tui.event.Event");
        if (eventElement != null) {
            this.eventType = eventElement.asType();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        // Group methods by enclosing class
        Map<TypeElement, List<MethodInfo>> methodsByClass = new HashMap<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(OnAction.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                error(element, "@OnAction can only be applied to methods");
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;
            if (!validateMethod(method)) {
                continue;
            }

            TypeElement enclosingClass = (TypeElement) method.getEnclosingElement();
            OnAction annotation = method.getAnnotation(OnAction.class);

            methodsByClass
                    .computeIfAbsent(enclosingClass, k -> new ArrayList<>())
                    .add(new MethodInfo(method.getSimpleName().toString(), annotation.value()));
        }

        // Generate registrar for each class
        Set<String> generatedClasses = new HashSet<>();
        for (Map.Entry<TypeElement, List<MethodInfo>> entry : methodsByClass.entrySet()) {
            try {
                String registrarClass = generateRegistrar(entry.getKey(), entry.getValue());
                generatedClasses.add(registrarClass);
            } catch (IOException e) {
                error(entry.getKey(), "Failed to generate registrar: " + e.getMessage());
            }
        }

        // Generate/update META-INF/services file
        if (!generatedClasses.isEmpty()) {
            try {
                generateServicesFile(generatedClasses);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate META-INF/services file: " + e.getMessage());
            }
        }

        return true;
    }

    private boolean validateMethod(ExecutableElement method) {
        // Check not private
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            error(method, "@OnAction methods must not be private");
            return false;
        }

        // Check return type is void
        if (!typeUtils.isSameType(method.getReturnType(),
                typeUtils.getNoType(javax.lang.model.type.TypeKind.VOID))) {
            error(method, "@OnAction methods must return void");
            return false;
        }

        // Check exactly one parameter
        List<? extends VariableElement> params = method.getParameters();
        if (params.size() != 1) {
            error(method, "@OnAction methods must have exactly one parameter of type Event");
            return false;
        }

        // Check parameter is Event type (or subtype)
        if (eventType != null) {
            TypeMirror paramType = params.get(0).asType();
            if (!typeUtils.isAssignable(eventType, typeUtils.erasure(paramType)) &&
                    !typeUtils.isAssignable(paramType, eventType)) {
                error(method, "@OnAction method parameter must be of type Event or a subtype");
                return false;
            }
        }

        return true;
    }

    private String generateRegistrar(TypeElement targetClass, List<MethodInfo> methods)
            throws IOException {
        String packageName = getPackageName(targetClass);
        String className = targetClass.getSimpleName().toString();
        String registrarClassName = className + "_ActionHandlerRegistrar";
        String qualifiedName = packageName.isEmpty()
                ? registrarClassName
                : packageName + "." + registrarClassName;

        JavaFileObject sourceFile = filer.createSourceFile(qualifiedName, targetClass);
        try (Writer writer = new BufferedWriter(sourceFile.openWriter())) {
            // Package declaration
            if (!packageName.isEmpty()) {
                writer.write("package " + packageName + ";\n\n");
            }

            // Imports
            writer.write("import dev.tamboui.tui.bindings.ActionHandler;\n");
            writer.write("import dev.tamboui.tui.bindings.ActionHandlerRegistrar;\n\n");

            // Class declaration
            writer.write("/**\n");
            writer.write(" * Generated registrar for {@link " + className + "}.\n");
            writer.write(" * <p>\n");
            writer.write(" * This class is generated by the annotation processor and should not be modified.\n");
            writer.write(" */\n");
            writer.write("public final class " + registrarClassName + "\n");
            writer.write("        implements ActionHandlerRegistrar<" + className + "> {\n\n");

            // register method
            writer.write("    @Override\n");
            writer.write("    public void register(" + className + " target, ActionHandler handler) {\n");
            for (MethodInfo method : methods) {
                writer.write("        handler.on(\"" + escapeString(method.actionName)
                        + "\", target::" + method.methodName + ");\n");
            }
            writer.write("    }\n\n");

            // targetType method
            writer.write("    @Override\n");
            writer.write("    public Class<" + className + "> targetType() {\n");
            writer.write("        return " + className + ".class;\n");
            writer.write("    }\n");

            writer.write("}\n");
        }

        return qualifiedName;
    }

    private void generateServicesFile(Set<String> registrarClasses) throws IOException {
        FileObject resource = filer.createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/dev.tamboui.tui.bindings.ActionHandlerRegistrar"
        );

        try (Writer writer = new BufferedWriter(resource.openWriter())) {
            for (String className : registrarClasses) {
                writer.write(className);
                writer.write("\n");
            }
        }
    }

    private String getPackageName(TypeElement typeElement) {
        Element enclosing = typeElement.getEnclosingElement();
        while (enclosing != null && enclosing.getKind() != ElementKind.PACKAGE) {
            enclosing = enclosing.getEnclosingElement();
        }
        if (enclosing instanceof PackageElement) {
            return ((PackageElement) enclosing).getQualifiedName().toString();
        }
        return "";
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private static class MethodInfo {
        final String methodName;
        final String actionName;

        MethodInfo(String methodName, String actionName) {
            this.methodName = methodName;
            this.actionName = actionName;
        }
    }
}
