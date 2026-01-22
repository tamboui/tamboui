plugins {
    id("dev.tamboui.java-library")
    `java-test-fixtures`
}

description = "Pygments (pygmentize) CLI based syntax highlighting for TamboUI Text"

dependencies {
    api(projects.tambouiCore)

    testFixturesApi(libs.assertj.core)
}

