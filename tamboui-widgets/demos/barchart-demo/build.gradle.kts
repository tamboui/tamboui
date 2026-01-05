plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the BarChart widget"

demo {
    tags = setOf("barchart", "block", "paragraph", "chart", "data-visualization")
}

application {
    mainClass.set("dev.tamboui.demo.BarChartDemo")
}
