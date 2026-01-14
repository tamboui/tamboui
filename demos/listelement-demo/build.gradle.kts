plugins {
    id("dev.tamboui.demo-project")
}

description = "Demonstrates ListElement with rich content (any StyledElement as items)"

demo {
    displayName = "ListElement Demo"
    tags = setOf("list", "toolkit")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass = "dev.tamboui.demo.ListElementDemo"
}
