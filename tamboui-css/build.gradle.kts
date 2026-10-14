plugins {
    id("dev.tamboui.java-library")
}

description = "CSS styling support for TamboUI TUI library"

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    testImplementation(testFixtures(projects.tambouiCore))

    compileOnly(libs.jfr.polyfill)
}

