plugins {
    id("dev.tamboui.java-library")
}

description = "Annotation processor for TamboUI"

dependencies {
    implementation(projects.tambouiAnnotations)
    implementation(projects.tambouiTui)

    testImplementation(libs.compile.testing)
}
