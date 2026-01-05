plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing collapsed borders with Block widgets"

demo {
    tags = setOf("block", "borders", "layout")
}

application {
    mainClass.set("dev.tamboui.demo.CollapsedBordersDemo")
}

