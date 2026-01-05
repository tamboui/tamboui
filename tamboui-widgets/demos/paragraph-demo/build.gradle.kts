plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Paragraph widget"

demo {
    tags = setOf("paragraph", "block", "text", "wrapping", "scrolling")
}

application {
    mainClass.set("dev.tamboui.demo.ParagraphDemo")
}

