plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the markdown renderer, including simulated LLM streaming"

demo {
    tags = setOf("markdown", "text", "streaming")
    internal = true
}

dependencies {
    implementation(project(":tamboui-markdown"))
}

application {
    mainClass.set("dev.tamboui.demo.MarkdownDemo")
}
