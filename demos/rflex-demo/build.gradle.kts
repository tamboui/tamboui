plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Flex layout modes (Ratatui's Flex demo)"

dependencies {
    implementation(projects.tambouiToolkit)
    runtimeOnly(projects.tambouiJline)
}

application {
    mainClass.set("dev.tamboui.demo.flex.RFlexDemo")
}

demo {
    displayName = "Ratatui Flex Layout Demo"
    tags = setOf("layout", "flex", "constraints", "cassowary")
}
