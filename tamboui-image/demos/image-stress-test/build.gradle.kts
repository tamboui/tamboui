plugins {
    id("dev.tamboui.demo-project")
}

description = "Manual stress test for image memory leak verification"

demo {
    tags = setOf("image", "stress-test", "memory", "leak")
    internal = true
}

dependencies {
    implementation(project(":tamboui-image"))
}

application {
    mainClass.set("dev.tamboui.demo.ImageStressTest")
}
