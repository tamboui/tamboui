plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Scrollbar widget"

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
}

demo {
    tags = setOf("scrollbar", "list", "block", "paragraph", "navigation")
}

application {
    mainClass.set("dev.tamboui.demo.ScrollbarDemo")
}
