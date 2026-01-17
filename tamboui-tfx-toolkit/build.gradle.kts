plugins {
    id("dev.tamboui.java-library")
}

description = "TFX effects integration with TamboUI Toolkit DSL"

dependencies {
    api(projects.tambouiTfxTui)
    api(projects.tambouiToolkit)
}
