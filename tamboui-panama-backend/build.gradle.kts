plugins {
    id("dev.tamboui.java-library")
}

description = "Panama FFI backend for TamboUI TUI library"

tasks.withType<JavaCompile>().configureEach {
    options.release = 22
    // Suppress warnings for restricted Panama FFI methods
    options.compilerArgs.add("-Xlint:-restricted")
}

tasks.withType<Test> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    api(projects.tambouiCore)
}
