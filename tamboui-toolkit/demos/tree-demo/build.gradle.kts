plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing tree view with expand/collapse navigation"

demo {
    displayName = "Tree Demo"
    tags = setOf("tree", "navigation", "expand", "collapse", "lazy-loading", "toolkit")
}

dependencies {
    implementation(projects.tambouiToolkit)
}

application {
    mainClass.set("dev.tamboui.demo.tree.TreeDemo")
}
