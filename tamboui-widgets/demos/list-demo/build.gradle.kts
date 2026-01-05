plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing List widget"

demo {
    tags = setOf("list", "block", "paragraph", "navigation")
}

application {
    mainClass.set("dev.tamboui.demo.ListDemo")
}

