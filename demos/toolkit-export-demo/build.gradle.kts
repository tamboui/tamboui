plugins {
    id("dev.tamboui.demo-project")
}

description = "Toolkit export demo: uses Element.renderedArea() for export regions. Press E to export."

demo {
    displayName = "Export (Toolkit)"
    tags = setOf("export", "toolkit", "renderedArea", "crop", "svg", "html")
}

dependencies {
    implementation(project(":tamboui-toolkit"))
}

application {
    mainClass.set("dev.tamboui.demo.ToolkitExportDemo")
}
