plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing ActionHandler with programmatic and annotation APIs"

demo {
    displayName = "Action Handler"
    tags = setOf("toolkit", "bindings", "actions", "annotations", "focus", "panel", "row", "column", "text")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiAnnotations)
    annotationProcessor(projects.tambouiProcessor)
}

application {
    mainClass.set("dev.tamboui.demo.actionhandler.ActionHandlerDemo")
}

tasks.withType<JavaCompile>().configureEach {
    // Remove -Werror for demo project - Gradle's internal TimeTrackingProcessor
    // causes unavoidable warning when using annotation processors with Java 21
    options.compilerArgs.remove("-Werror")
}
