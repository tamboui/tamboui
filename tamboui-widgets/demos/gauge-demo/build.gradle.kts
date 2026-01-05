plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Gauge and LineGauge widgets"

demo {
    tags = setOf("gauge", "block", "paragraph", "progress")
}

application {
    mainClass.set("dev.tamboui.demo.GaugeDemo")
}
