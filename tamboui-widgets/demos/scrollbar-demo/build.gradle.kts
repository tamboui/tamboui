plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Scrollbar widget"

demo {
    tags = setOf("scrollbar", "list", "block", "paragraph", "navigation")
}

application {
    mainClass.set("dev.tamboui.demo.ScrollbarDemo")
}
