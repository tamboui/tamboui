plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Theme system with live theme switching"

demo {
    displayName = "Theme Switcher"
    tags = setOf("theme", "colors", "styling")
}

application {
    mainClass.set("dev.tamboui.demo.ThemeDemo")
}
