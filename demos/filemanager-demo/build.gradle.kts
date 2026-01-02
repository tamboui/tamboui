plugins {
    id("dev.tamboui.demo-project")
}

description = "Two-panel file manager demo showcasing MVC architecture"

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.filemanager.FileManagerDemo")
}