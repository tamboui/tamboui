plugins {
    id("dev.tamboui.demo-project")
}

description = "Demonstrates toolkit mouse routing: left, middle, and right button presses"

demo {
    displayName = "Mouse Demo"
    tags = setOf("mouse", "input", "toolkit")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass = "dev.tamboui.demo.MouseDemo"
}
