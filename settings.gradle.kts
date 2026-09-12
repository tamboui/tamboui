plugins {
    // Auto-provisions JDK toolchains (e.g. the Java 21 test toolchain used by
    // tamboui-backend-integration-tests) when they are not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tamboui-parent"

val modules = listOf(
    "tamboui-bom",
    "tamboui-core",
    "tamboui-css",
    "tamboui-widgets",
    "tamboui-image",
    "tamboui-markdown",
    "tamboui-jline3-backend",
    "tamboui-panama-backend",
    "tamboui-aesh-backend",
    "tamboui-tui",
    "tamboui-picocli",
    "tamboui-toolkit",
    "tamboui-toolkit-markdown",
    "tamboui-annotations",
    "tamboui-processor",
    "tamboui-tfx",
    "tamboui-tfx-tui",
    "tamboui-tfx-toolkit",
    "tamboui-demos",
    "tamboui-benchmarks",
    "tamboui-backend-integration-tests",
    "docs"
)

include(*modules.toTypedArray())

fun includeDemosFrom(demosDir: File, projectPathPrefix: String) {
    demosDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".settings") }?.forEach { demo ->
        val projectPath = "$projectPathPrefix${demo.name}"
        include(projectPath)
        project(":$projectPath").projectDir = demo
    }
}

// Include demos from root demos directory (for demo-selector which spans all modules)
val rootDemosDir = File(settingsDir, "demos")
includeDemosFrom(rootDemosDir, "demos:")

// Include demos from each module's demos directory
modules.forEach { module ->
    val moduleDemosDir = File(settingsDir, "$module/demos")
    includeDemosFrom(moduleDemosDir, "$module:demos:")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val javaVersion = JavaVersion.current()
if (!javaVersion.isCompatibleWith(JavaVersion.VERSION_25)) {
    throw GradleException("This project is compatible with Java ${javaVersion}, but requires Java 25+ JDK for building.")
}