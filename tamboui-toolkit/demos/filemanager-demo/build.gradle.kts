plugins {
    id("dev.tamboui.demo-project")
}

description = "Two-panel file manager demo showcasing MVC architecture"

demo {
    displayName = "File Manager"
    tags = setOf("toolkit", "list", "panel", "text", "application", "mvc", "interactive")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.filemanager.FileManagerDemo")
}