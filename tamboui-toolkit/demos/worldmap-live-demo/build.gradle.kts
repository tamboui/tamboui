plugins {
    id("dev.tamboui.demo-project")
}

description = "Live OSM slippy map: raster tiles + vector overlay, Web Mercator, async fetch, pan/zoom"

demo {
    displayName = "WorldMap Live"
    tags = setOf("toolkit", "image", "map", "osm", "network")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiImage)
    implementation(projects.tambouiJline3Backend)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

application {
    mainClass.set("dev.tamboui.demo.WorldMapLiveDemo")
}
