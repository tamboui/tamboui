plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Tabs widget"

demo {
    tags = setOf("tabs", "block", "paragraph", "navigation")
}

application {
    mainClass.set("dev.tamboui.demo.TabsDemo")
}
