plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing PicoCLI integration with TamboUI"

dependencies {
    implementation(projects.tambouiPicocli)
    annotationProcessor(libs.picocli.codegen)
}

application {
    mainClass.set("dev.tamboui.demo.PicoCLIDemo")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Aproject=${project.group}/${project.name}",
        "-Xlint:-processing"  // Suppress annotation processor warnings
    ))
}
