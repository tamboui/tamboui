plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Input Form with focus management"

demo {
    tags = setOf("text-input", "form", "interactive", "focus")
}

application {
    mainClass.set("dev.tamboui.demo.InputFormDemo")
}

