import dev.tamboui.build.DemoExtension
import gradle.kotlin.dsl.accessors._d7c1cb8291fcf7e869bfba85a0dc6ae2.java

plugins {
    id("dev.tamboui.java-base")
    application
    id("org.graalvm.buildtools.native")
}

dependencies {
    implementation(project(":tamboui-core"))
    implementation(project(":tamboui-widgets"))
    runtimeOnly(project(":tamboui-jline"))
}

tasks.withType<JavaExec>().configureEach {
    enabled = false
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.addAll(
                "--no-fallback",
                "--initialize-at-build-time=org.jline",
                "--initialize-at-run-time=org.jline.nativ",
                "--allow-incomplete-classpath",
                "-H:+ReportExceptionStackTraces"
            )

            // JLine requires access to terminal
            jvmArgs.addAll(
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.io=ALL-UNNAMED"
            )
            resources {
                autodetection {
                    enabled = true
                }
            }
        }
    }

    toolchainDetection.set(false)
}

val demoExtension = extensions.create("demo", DemoExtension::class)
demoExtension.displayName.convention(provider {
    // Convert "barchart-demo" to "Barchart" (remove -demo suffix, capitalize)
    val baseName = project.name.removeSuffix("-demo")
    baseName.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
})
demoExtension.description.convention(provider { project.description ?: "" })
demoExtension.module.convention(provider {
    // Extract module from path: :tamboui-widgets:demos:foo -> "Widgets"
    val path = project.path
    if (path.startsWith(":demos:")) {
        "Other"
    } else {
        val parts = path.split(":")
        if (parts.size >= 2) {
            val moduleName = parts[1].removePrefix("tamboui-")
            if (moduleName.length <= 3) {
                moduleName.uppercase()
            } else {
                moduleName.replaceFirstChar { it.uppercase() }
            }
        } else {
            "Unknown"
        }
    }
})
