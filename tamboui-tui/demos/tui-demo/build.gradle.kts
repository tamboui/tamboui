plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the TuiRunner framework"

dependencies {
    implementation(projects.tambouiTui)
}

application {
    mainClass.set("dev.tamboui.demo.TuiDemo")
}
