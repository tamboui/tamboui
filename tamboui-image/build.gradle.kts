plugins {
    id("dev.tamboui.java-library")
}

description = "Image rendering support for TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    testImplementation(testFixtures(projects.tambouiCore))
}
