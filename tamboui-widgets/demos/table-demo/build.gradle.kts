plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Table widget"

demo {
    tags = setOf("table", "block", "paragraph", "data")
}

application {
    mainClass.set("dev.tamboui.demo.TableDemo")
}
