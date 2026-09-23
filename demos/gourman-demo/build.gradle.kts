plugins {
    id("dev.tamboui.demo-project")
}

description = "Gourman - a Pac-Man style maze game rendered on a canvas, with pluggable game-event listeners"

demo {
    displayName = "Gourman Maze Game"
    tags = setOf("canvas", "game", "animation", "events")
}

dependencies {
    implementation(projects.tambouiTui)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
}

application {
    mainClass.set("dev.tamboui.demo.gourman.Gourman")
}
