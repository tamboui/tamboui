plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
}

description = "Fluent DSL for building TUI applications with TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    api(projects.tambouiTui)
    api(projects.tambouiCss)
    testImplementation(testFixtures(projects.tambouiCore))

    // Test fixtures dependencies
    testFixturesApi(projects.tambouiCore)
    testFixturesApi(projects.tambouiTui)
    testFixturesImplementation(libs.assertj.core)
}
