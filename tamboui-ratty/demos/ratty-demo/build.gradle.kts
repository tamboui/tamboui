plugins {
    id("dev.tamboui.demo-project")
}

description = "Ratty Graphics Protocol demo"

demo {
    displayName = "Ratty Graphics Protocol (3D Objects) Demo"
    tags = setOf("ratty", "3d", "graphics", "protocol")
}

application {
    mainClass.set("dev.tamboui.demos.ratty.RattyDemo")
}

dependencies {
    implementation(projects.tambouiCore)
    implementation(projects.tambouiWidgets)
    implementation(projects.tambouiRatty)
    implementation(projects.tambouiJline3Backend)
    implementation(projects.tambouiTui)
}
