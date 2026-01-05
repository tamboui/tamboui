import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases")
        content {
            includeGroup("org.gradle")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}


tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:all",
        "-Xlint:-serial",
        "-Werror",
        "-Xlint:-options"
    ))
    options.release = 8
}

tasks.withType<Test> {
    useJUnitPlatform()
}

group = "dev.tamboui"
