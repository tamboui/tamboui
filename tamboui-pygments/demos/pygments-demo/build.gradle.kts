plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing pygmentize-based syntax highlighting"

demo {
    displayName = "Syntax highlighting (Pygmentize)"
    tags = setOf("toolkit", "syntax-highlighting", "pygments", "richtext", "scrolling")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiPygments)
}

application {
    mainClass.set("dev.tamboui.demo.PygmentsDemo")
}

