plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the TuiRunner framework"

demo {
    displayName = "TUI Runner"
    tags = setOf("tui-runner", "block", "paragraph", "events", "animation", "mouse")
}

dependencies {
    implementation(projects.tambouiTui)
}

application {
    mainClass.set("dev.tamboui.demo.TuiDemo")
}
