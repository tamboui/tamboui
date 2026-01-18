plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing error handling and fault-tolerant rendering"

dependencies {
    implementation(projects.tambouiToolkit)
    runtimeOnly(projects.tambouiJline3Backend)
}

application {
    mainClass.set("dev.tamboui.demo.errorhandling.ErrorHandlingDemo")
}

demo {
    displayName = "Error Handling Demo"
    tags = setOf("toolkit", "error-handling", "fault-tolerant", "exceptions")
}
