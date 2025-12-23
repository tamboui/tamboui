rootProject.name = "tamboui-parent"

include(
    "tamboui-core",
    "tamboui-widgets",
    "tamboui-jline",
    "tamboui-tui",
    "tamboui-picocli",
    "tamboui-toolkit",
    "tamboui-tfx"
)

File(settingsDir, "demos").listFiles()?.forEach {
    if (it.isDirectory) {
        include("demos:${it.name}")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")