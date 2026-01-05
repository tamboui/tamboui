plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Sparkline widget"

demo {
    tags = setOf("sparkline", "block", "paragraph", "data-visualization", "animation")
}

application {
    mainClass.set("dev.tamboui.demo.SparklineDemo")
}
