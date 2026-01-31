plugins {
    id("dev.tamboui.java-library")
}

description = "Syntax-highlight source in TamboUI via lumis4j (source string to markup)"

repositories {
    mavenCentral()
}

// lumis4j requires Java 11+
tasks.withType<JavaCompile>().configureEach {
    options.release = 11
}

dependencies {
    api(projects.tambouiCore)
    implementation(libs.lumis4j)
}
