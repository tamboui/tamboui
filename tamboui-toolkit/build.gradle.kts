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
    testImplementation(testFixtures(projects.tambouiTui))
    testFixturesImplementation(projects.tambouiTui)
    testFixturesImplementation(testFixtures(projects.tambouiTui))
    testFixturesImplementation(projects.tambouiCore)
    testFixturesImplementation(projects.tambouiWidgets)
}
