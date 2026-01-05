plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the DSL module with Widget Playground"

demo {
    tags = setOf("toolkit", "panel", "row", "column", "text", "focus", "draggable", "mouse")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.ToolkitDemo")
}
