plugins {
    id("dev.tamboui.demo-project")
}

description = "New Year countdown demo with fireworks and 3D effects"

demo {
    displayName = "New Year"
    tags = setOf("toolkit", "canvas", "fireworks", "countdown", "3d", "audio", "animation")
}

dependencies {
    implementation(projects.tambouiToolkit)
    // Note: tamboui-panama-backend is used via jbang script header
    // If building with Gradle, ensure tamboui-panama-backend is available
    // either as a project dependency or from Maven repositories
}

application {
    mainClass.set("dev.tamboui.demo.NewYearDemo")
}

