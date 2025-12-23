plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo TUI application showcasing TamboUI effects"

dependencies {
    implementation(project(":tamboui-tfx"))
    implementation(project(":tamboui-tui"))
    runtimeOnly(project(":tamboui-jline"))
}

application {
    mainClass.set("dev.tamboui.demo.FxDemo")
}


