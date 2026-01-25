import dev.tamboui.build.DemoExtension
import dev.tamboui.build.GenerateDemoMetadataTask
import dev.tamboui.build.RecordDemoTask
import gradle.kotlin.dsl.accessors._d7c1cb8291fcf7e869bfba85a0dc6ae2.java

plugins {
    id("dev.tamboui.java-base")
    application
    id("org.graalvm.buildtools.native")
}

dependencies {
    implementation(project(":tamboui-core"))
    implementation(project(":tamboui-widgets"))
    // Order of backend dependencies matters: first one has higher priority
    runtimeOnly(project(":tamboui-panama-backend"))
    runtimeOnly(project(":tamboui-jline3-backend"))
}

tasks.withType<JavaExec>().configureEach {
    enabled = false
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 22
}

java {
    sourceCompatibility = JavaVersion.VERSION_22
    targetCompatibility = JavaVersion.VERSION_22
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
                "-H:+ReportExceptionStackTraces",
                "--enable-native-access=ALL-UNNAMED",
                "-H:+SharedArenaSupport"
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

// Configuration for cast files
val casts by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-cast"))
    }
}
val screenshots by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-screenshots"))
    }
}

// Configuration for metadata files
val demoMetadata by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-metadata"))
    }
}

// Create demo extension first so tasks can reference it
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
        "Core"
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
            "Other"
        }
    }
})
demoExtension.tags.convention(emptySet())
demoExtension.internal.convention(false)

val tape = rootProject.layout.projectDirectory.file("docs/video/${project.name}.tape")

// Task to record the demo to cast file
val recordDemo = tasks.register<RecordDemoTask>("recordDemo") {
    val outputDir = layout.buildDirectory.dir("generated/casts")

    mainClass = application.mainClass
    classpath.from(sourceSets["main"].runtimeClasspath)
    outputDirectory = outputDir

    fps = 10
    duration = 5000
    width = 120
    height = 40

    // Look for VHS tape file in docs/video/
    val tapeFile = tape.asFile
    if (tapeFile.exists()) {
        configFile.set(tapeFile)
    }
}

// Task to generate metadata file
val generateDemoMetadata = tasks.register<GenerateDemoMetadataTask>("generateDemoMetadata") {
    demoId = project.name
    displayName = demoExtension.displayName
    demoDescription = demoExtension.description
    module = demoExtension.module
    tags = demoExtension.tags
    internal = demoExtension.internal
    castFileName = provider { "${project.name}.cast" }
    outputFile = layout.buildDirectory.file("generated/metadata/${project.name}.json")
}

// Register artifacts
casts.outgoing.artifact(recordDemo.map(RecordDemoTask::getOutputCastFile))
screenshots.outgoing.artifact(recordDemo.map(RecordDemoTask::getScreenshotsDirectory))
demoMetadata.outgoing.artifact(generateDemoMetadata)

val validateDemo = tasks.register("validateDemo") {
    doLast {
        if (!demoExtension.internal.get()) {
            if (!tape.asFile.exists()) {
                throw GradleException("Demo tape file is missing: ${tape.asFile.absolutePath}")
            }
        }
    }
}

tasks.check {
    dependsOn(validateDemo)
}