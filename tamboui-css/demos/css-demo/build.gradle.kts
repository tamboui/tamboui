plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing CSS styling with live theme switching"

demo {
    displayName = "CSS Styling"
    tags = setOf("css", "toolkit", "panel", "list", "text", "theming", "focus", "mouse")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.CssDemo")
}
