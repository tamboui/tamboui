plugins {
    id("dev.tamboui.demo-project")
}

description = "Inline progress display demo (NPM/Gradle-style)"

demo {
    displayName = "Inline display mode using low level API demo"
}

dependencies {
    implementation(projects.tambouiTui)
    implementation(projects.tambouiWidgets)
}

application {
    mainClass.set("dev.tamboui.demo.InlineProgressDemo")
}
