plugins {
    id("dev.tamboui.java-library")
}

description = "Syntax-highlight source in TamboUI via lumis4j (source string to markup)"

repositories {
    mavenCentral()
}

dependencies {
    api(projects.tambouiCore)
    implementation(libs.lumis4j)
}
