plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing RichText and MarkupText components"

demo {
    displayName = "Text area with rich formatting"
    tags = setOf("toolkit", "richtext", "markup", "styled-text", "scrolling", "interactive")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.RichTextDemo")
}
