plugins {
    id("dev.tamboui.java-library")
}

description = "TFX effects integration with TamboUI TUI framework"

dependencies {
    api(projects.tambouiTfx)
    api(projects.tambouiTui)
}
