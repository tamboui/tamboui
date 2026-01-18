plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
}

description = "Core types and abstractions for TamboUI TUI library"

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    manifest {
        attributes("Main-Class" to "dev.tamboui.Main")
    }
}

dependencies {
    testFixturesApi(libs.assertj.core)
}
