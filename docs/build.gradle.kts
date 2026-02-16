plugins {
    id("dev.tamboui.docs")
}

dependencies {
    snippetsImplementation(projects.tambouiCore)
    snippetsImplementation(projects.tambouiWidgets)
    snippetsImplementation(projects.tambouiToolkit)
    snippetsImplementation(projects.tambouiCss)
    snippetsImplementation(projects.tambouiTui)
    snippetsImplementation(projects.tambouiJline3Backend)
}
