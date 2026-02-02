plugins {
    id("dev.tamboui.demo-project")
}

description = "Buffer export demo (SVG, HTML, text) using core widgets; full-buffer export only"

demo {
    displayName = "Export (Core)"
    tags = setOf("export", "svg", "html", "text", "buffer")
}

application {
    mainClass.set("dev.tamboui.demo.ExportDemo")
}
