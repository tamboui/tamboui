plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the MirroredSparkline widget"

demo {
    tags = setOf("sparkline", "mirrored-sparkline", "block", "paragraph", "data-visualization", "animation")
}

application {
    mainClass.set("dev.tamboui.demo.MirroredSparklineDemo")
}
