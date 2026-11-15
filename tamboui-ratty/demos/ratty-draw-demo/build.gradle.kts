plugins {
    id("dev.tamboui.demo-project")
}

description = "Ratty Graphics Protocol drawing demo - draw a 2D shape and see it as a rotating 3D object"

demo {
    displayName = "Ratty Drawing Demo (2D-to-3D)"
    tags = setOf("ratty", "3d", "graphics", "drawing", "mouse", "interactive")
}

application {
    mainClass.set("dev.tamboui.demos.ratty.RattyDrawDemo")
}

dependencies {
    implementation(projects.tambouiCore)
    implementation(projects.tambouiWidgets)
    implementation(projects.tambouiRatty)
    implementation(projects.tambouiJline3Backend)
    implementation(projects.tambouiTui)
}
