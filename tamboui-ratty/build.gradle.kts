plugins {
    id("dev.tamboui.java-library")
}

description = "Ratty Graphics Protocol (3D object rendering) support for TamboUI"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    testImplementation(testFixtures(projects.tambouiCore))
}
