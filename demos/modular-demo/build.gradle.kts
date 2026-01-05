plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the toolkit module using the Java module path"

dependencies {
    implementation(projects.tambouiToolkit)
    runtimeOnly(projects.tambouiJline)
}

application {
    mainClass.set("dev.tamboui.demo.modular.ModularDemo")
    mainModule.set("dev.tamboui.demo.modular")
}

tasks.withType<JavaCompile>().configureEach {
    modularity.inferModulePath = true
}

demo {
    displayName = "Module Path Demo"
    tags = setOf("toolkit", "panel", "text", "modularity", "module-path", "java-modules", "mouse")
}