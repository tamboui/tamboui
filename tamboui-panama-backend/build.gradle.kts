plugins {
    id("dev.tamboui.java-library")
    id("org.graalvm.buildtools.native")
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

graalvmNative {
    binaries.named("test") {
        // AddAllCharsets so the native test exercises the decoder against real single-byte charsets
        // (ISO-8859-15, ...) rather than GraalVM's reduced default set. Applications opt in themselves;
        // see InputDecoder / UnixTerminal.detectCharset for the UTF-8 fallback without it.
        buildArgs.addAll("--enable-native-access=ALL-UNNAMED", "-H:+SharedArenaSupport", "-H:+AddAllCharsets")
    }
    toolchainDetection.set(false)
}

dependencies {
    api(projects.tambouiCore)
}

// This project is compiled with Java 22, but at runtime we want
// it to be compatible with applications that ship with multiple
// backends, in which case it will be silently ignored on older JVMs.
configurations.runtimeElements {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
}