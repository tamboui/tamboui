plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing RGB color support"

demo {
    displayName = "RGB Colors"
    tags = setOf("colors", "animation", "styling")
}

application {
    mainClass.set("dev.tamboui.demo.ColorsRgbDemo")
}

