plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the DSL module with Widget Playground"

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.ToolkitDemo")
}
