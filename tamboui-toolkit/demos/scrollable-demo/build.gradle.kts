plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showing Scrollable viewport on a list"

demo {
    displayName = "Scrollable"
    tags = setOf("toolkit", "layout")
}

dependencies {
    implementation(projects.tambouiToolkit)
    // Note: tamboui-panama-backend is used via jbang script header
    // If building with Gradle, ensure tamboui-panama-backend is available
    // either as a project dependency or from Maven repositories
}

application {
    mainClass.set("dev.tamboui.demo.ScrollableDemo")
}

