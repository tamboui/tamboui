plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo TUI application showcasing TamboUI effects"

dependencies {
    implementation(projects.tambouiTfx)
    implementation(projects.tambouiTui)
}

application {
    mainClass.set("dev.tamboui.demo.TFxDemo")
}

demo {
    displayName = "TamboUI TFX Demo"
    tags = setOf("tfx", "effects", "animation", "progress", "widgets")
}