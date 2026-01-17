plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing TFX effects integration with Toolkit DSL"

dependencies {
    implementation(projects.tambouiTfxToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.TfxToolkitDemo")
}

demo {
    displayName = "TFX Toolkit Integration Demo"
    tags = setOf("tfx", "effects", "animation", "toolkit")
}
