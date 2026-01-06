plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing custom CSS-styled components with @OnAction annotations"

demo {
    tags = setOf("toolkit", "custom-component", "css", "panel", "gauge", "mouse")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiAnnotations)
    annotationProcessor(projects.tambouiProcessor)
}

application {
    mainClass.set("dev.tamboui.demo.CustomComponentDemo")
}

tasks.withType<JavaCompile>().configureEach {
    // Remove -Werror for demo project - Gradle's internal TimeTrackingProcessor
    // causes unavoidable warning when using annotation processors with Java 21
    options.compilerArgs.remove("-Werror")
}
