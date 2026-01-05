plugins {
    id("dev.tamboui.demo-project")
}

description = "JTop - System monitor demo using the DSL module"

demo {
    displayName = "JTop"
    tags = setOf("toolkit", "panel", "row", "column", "text", "gauge", "barchart", "sparkline", "system", "monitoring")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(libs.oshi.core)
}

application {
    mainClass.set("dev.tamboui.demo.JTopDemo")
}
