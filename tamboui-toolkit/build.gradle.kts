plugins {
    id("dev.tamboui.java-library")
}

description = "Fluent DSL for building TUI applications with TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    api(projects.tambouiTui)
    api(projects.tambouiCss)
    testImplementation(testFixtures(projects.tambouiCore))
}
