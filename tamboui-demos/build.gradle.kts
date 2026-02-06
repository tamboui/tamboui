plugins {
    id("dev.tamboui.demo-aggregator")
}

description = "All TamboUI demos in a single executable jar"

dependencies {
    // Dependencies needed for the interactive demo launcher
    implementation(projects.tambouiCore)
    implementation(projects.tambouiWidgets)
    implementation(projects.tambouiTui)
}
