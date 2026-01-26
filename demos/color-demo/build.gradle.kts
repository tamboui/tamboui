plugins {
    id("dev.tamboui.demo-project")
}

description = "Color modes demo - ANSI, indexed, and RGB"

demo {
    displayName = "Color modes demo"
    internal = true
}

application {
    mainClass.set("dev.tamboui.demo.ColorDemo")
}
