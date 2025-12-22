plugins {
    id("ink.glimt.demo-project")
}

description = "Demo showcasing a simple file browser built with the toolkit DSL"

dependencies {
    implementation(projects.glimtToolkit)
}

application {
    mainClass.set("ink.glimt.demo.FileBrowserDemo")
}
