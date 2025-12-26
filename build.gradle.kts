plugins {
    id("dev.tamboui.maven-central-publishing")
}

tasks.register("listModules") {
    doLast {
        println("Modules found in build:")
        subprojects.forEach { project ->
            println("  - ${project.path}")
        }
    }
}
