plugins {
    id("dev.tamboui.demo-project")
}

description = "Buffer export demo (SVG, HTML, text)"

demo {
    displayName = "Export demo"
    internal = true
}

application {
    mainClass.set("dev.tamboui.demo.ExportDemo")
}
