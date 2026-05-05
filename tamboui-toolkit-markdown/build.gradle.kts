plugins {
    id("dev.tamboui.java-library")
}

description = "Toolkit DSL element wrapping the markdown renderer"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

dependencies {
    api(projects.tambouiMarkdown)
    api(projects.tambouiToolkit)
    testImplementation(testFixtures(projects.tambouiCore))
    testImplementation(testFixtures(projects.tambouiToolkit))
}
