plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Flex layout modes"

dependencies {
    implementation(projects.tambouiToolkit)
    runtimeOnly(projects.tambouiJline)
}

application {
    mainClass.set("dev.tamboui.demo.flex.FlexDemo")
}

demo {
    displayName = "Flex Layout Demo"
    tags = setOf("layout", "flex", "constraints", "cassowary")
}
