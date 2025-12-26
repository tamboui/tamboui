plugins {
    id("dev.tamboui.demo-project")
}

description = "Minimal Hello World demo using TuiRunner"

dependencies {
    implementation(project(":tamboui-tui"))
}

application {
    mainClass.set("dev.tamboui.demo.HelloWorldDemo")
}

