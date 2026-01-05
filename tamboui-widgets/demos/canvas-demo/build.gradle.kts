plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Canvas widget for arbitrary shape drawing"

demo {
    tags = setOf("canvas", "block", "paragraph", "graphics", "animation")
}

application {
    mainClass.set("dev.tamboui.demo.CanvasDemo")
}
