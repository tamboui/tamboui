plugins {
    id("dev.tamboui.java-library")
}

description = "Markdown rendering support for TamboUI"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

dependencies {
    api(projects.tambouiCore)
    api(projects.tambouiWidgets)
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.tables)
    implementation(libs.commonmark.ext.tasks)
    implementation(libs.commonmark.ext.strikethrough)
    testImplementation(testFixtures(projects.tambouiCore))
}
