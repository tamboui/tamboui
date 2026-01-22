plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing InlineApp and InlineToolkitRunner abstractions"

demo {
    displayName = "Inline display mode using Toolkit"
    tags = setOf("inline", "toolkit", "progress", "animation")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.InlineToolkitDemo")
}
