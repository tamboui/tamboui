plugins {
    id("dev.tamboui.demo-project")
}

description = "Doom-style raycasting demo using TamboUI"

demo {
    displayName = "Doom Raycaster"
    tags = setOf("tui-runner", "raycasting", "game", "3d", "interactive")
}

dependencies {
    implementation(projects.tambouiTui)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
}

application {
    mainClass.set("dev.tamboui.demo.doom.DoomDemo")
}
