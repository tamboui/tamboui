plugins {
    id("dev.tamboui.demo-project")
}

description = "Toolkit DSL demo for the markdown renderer with CSS-based styling"

demo {
    tags = setOf("toolkit", "markdown", "css", "styling")
    internal = true
}

dependencies {
    implementation(project(":tamboui-toolkit-markdown"))
    implementation(project(":tamboui-css"))
}

application {
    mainClass.set("dev.tamboui.demo.MarkdownToolkitDemo")
}
