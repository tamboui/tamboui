plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing form and table layouts with flex constraints"

dependencies {
    implementation(projects.tambouiToolkit)
    runtimeOnly(projects.tambouiJline)
}

application {
    mainClass.set("dev.tamboui.demo.layout.FormTableDemo")
}

demo {
    displayName = "Form & Table Layout Demo"
    tags = setOf("layout", "flex", "cassowary", "form", "table")
}
