plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing image rendering with terminal capability detection"

demo {
    tags = setOf("image", "graphics", "terminal-detection", "half-block", "braille")
}

dependencies {
    implementation(project(":tamboui-image"))
}

application {
    mainClass.set("dev.tamboui.demo.ImageDemo")
}
