plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing TextArea multi-line text input"

demo {
    tags = setOf("toolkit", "textarea", "text-input", "panel", "interactive", "mouse")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.TextAreaDemo")
}
