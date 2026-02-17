plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
}

description = "High-level TUI application framework for TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    api(projects.tambouiAnnotations)
    testImplementation(testFixtures(projects.tambouiCore))
    testImplementation(projects.tambouiToolkit)
    testImplementation(testFixtures(projects.tambouiToolkit))
    testFixturesImplementation(projects.tambouiCore)
    testFixturesImplementation(testFixtures(projects.tambouiCore))
}
