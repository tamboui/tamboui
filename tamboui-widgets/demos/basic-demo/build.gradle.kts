plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo TUI application using TamboUI"

demo {
    tags = setOf("list", "text-input", "block", "paragraph", "interactive")
}

application {
    mainClass.set("dev.tamboui.demo.Demo")
}

