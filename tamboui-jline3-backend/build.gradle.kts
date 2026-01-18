plugins {
    id("dev.tamboui.java-library")
}

description = "JLine 3 backend for TamboUI TUI library"

dependencies {
    api(projects.tambouiCore)
    api(libs.jline)
}
