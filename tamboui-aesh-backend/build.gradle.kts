plugins {
    id("dev.tamboui.java-library")
}

description = "Aesh Readline backend for TamboUI TUI library"

dependencies {
    api(projects.tambouiCore)
    api(libs.aesh.terminal)
}
