plugins {
    id("dev.tamboui.java-library")
}

description = "Standard widgets for TamboUI TUI library"

dependencies {
    api(projects.tambouiCore)
    testImplementation(projects.tambouiTui)
    testImplementation(testFixtures(projects.tambouiCore))
}
