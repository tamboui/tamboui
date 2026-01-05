plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing CSS styling without the toolkit module, using the core and widgets APIs only"

demo {
    displayName = "CSS Without Toolkit"
    tags = setOf("css", "block", "paragraph", "styling", "low-level")
}

dependencies {
    implementation(projects.tambouiWidgets)
    implementation(projects.tambouiCss)
}

application {
    mainClass.set("dev.tamboui.demo.CssNoToolkitDemo")
}
