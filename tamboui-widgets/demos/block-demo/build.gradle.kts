plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Block widget"

demo {
    tags = setOf("block", "paragraph", "borders", "styling")
}

application {
    mainClass.set("dev.tamboui.demo.BlockDemo")
}

