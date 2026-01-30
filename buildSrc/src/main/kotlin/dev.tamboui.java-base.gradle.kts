import dev.tamboui.build.JavadocTheming
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    id("com.diffplug.spotless")
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

tasks.withType<Javadoc>().configureEach {
    JavadocTheming.configure(this, project)
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xmaxwarns", "10000")
        addBooleanOption("Xwerror", true)
    }
}

spotless {
    java {
        // Import order is configurable via config/spotless/eclipse-import-order.txt
        importOrderFile(rootProject.file("config/spotless/eclipse-import-order.txt"))
        removeUnusedImports()

        eclipse().configFile(rootProject.file("config/spotless/eclipse-format.xml"))
       // formatAnnotations()
    }
}

group = "dev.tamboui"
