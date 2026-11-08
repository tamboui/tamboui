plugins {
    id("dev.tamboui.demo-project")
}

description = "Interactive offline world map with keyboard and mouse pan/zoom"

demo {
    displayName = "WorldMap"
    tags = setOf("toolkit", "map", "worldmap", "coastline", "geography")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.WorldMapDemo")
}
