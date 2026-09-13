plugins {
    id("dev.tamboui.java-base")
}

description = "Integration tests for backend discovery and selection across JDK versions (not published)"

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testImplementation(projects.tambouiCore)
    testRuntimeOnly(projects.tambouiJline3Backend)
    testRuntimeOnly(projects.tambouiPanamaBackend)
}

// Run the tests on a Java 21 runtime: tamboui-panama-backend is compiled for Java 22, so
// this reproduces the real mixed-JDK deployment where a provider is present on the
// classpath but cannot be loaded on the running JVM. Unit tests simulate this with an
// isolated classloader; these tests exercise the real UnsupportedClassVersionError path.
tasks.test {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
