plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
}

description = "High-level TUI application framework for TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    api(projects.tambouiAnnotations)
    testImplementation(projects.tambouiToolkit)
    testImplementation(testFixtures(projects.tambouiToolkit))
}
