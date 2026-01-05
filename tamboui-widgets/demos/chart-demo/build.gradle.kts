plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Chart widget for line/scatter plots"

demo {
    tags = setOf("chart", "block", "paragraph", "data-visualization", "animation")
}

application {
    mainClass.set("dev.tamboui.demo.ChartDemo")
}
