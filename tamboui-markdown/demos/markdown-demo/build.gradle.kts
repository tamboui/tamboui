plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the markdown renderer, including simulated LLM streaming"

demo {
    tags = setOf("markdown", "text", "streaming")
}

dependencies {
    implementation(project(":tamboui-markdown"))
}

application {
    mainClass.set("dev.tamboui.demo.MarkdownDemo")
}
