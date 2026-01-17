rootProject.name = "tamboui-parent"

val modules = listOf(
    "tamboui-core",
    "tamboui-css",
    "tamboui-widgets",
    "tamboui-image",
    "tamboui-jline",
    "tamboui-panama-backend",
    "tamboui-tui",
    "tamboui-picocli",
    "tamboui-toolkit",
    "tamboui-annotations",
    "tamboui-processor",
    "tamboui-tfx",
    "tamboui-tfx-tui",
    "tamboui-tfx-toolkit",
    "docs"
)

include(*modules.toTypedArray())

fun includeDemosFrom(demosDir: File, projectPathPrefix: String) {
    demosDir.listFiles()?.filter { it.isDirectory }?.forEach { demo ->
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
