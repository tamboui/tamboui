plugins {
    id("dev.tamboui.java-library")
}

description = "High-level TUI application framework for TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    api(projects.tambouiAnnotations)
}
