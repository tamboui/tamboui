import dev.tamboui.build.DemoExtension
import dev.tamboui.build.GenerateDemoManifestTask
import dev.tamboui.build.MergeServiceFilesTask

plugins {
    id("dev.tamboui.java-base")
    id("dev.tamboui.publishing")
}

val aggregatedDemos by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = false
}

val aggregatedDemosClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(aggregatedDemos)
    attributes {
        val runtimeClasspath = configurations.runtimeClasspath.get()
        runtimeClasspath.attributes.keySet().forEach {
            val key: Attribute<Any> = it as Attribute<Any>
            // We intentionally don't include the target version here, since we want
            // to include all demos in the fat jar independently of which Java version they target
            if (!TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE.equals(key)) {
                val attrValue: Any = runtimeClasspath.attributes.getAttribute(it)!!
                attribute(key, attrValue)
            }
        }
    }
}

// Data class to hold demo information
data class DemoInfo(
    val projectPath: String,
    val name: String,
    var displayName: String = "",
    var description: String = "",
    var module: String = "",
    var mainClass: String = "",
    var tags: Set<String> = emptySet()
)

// Map to collect demo info during configuration
val demoInfoMap = mutableMapOf<String, DemoInfo>()

// Capture reference to this project (tamboui-demos) for use inside allprojects callback
val aggregatorProject = project

// Automatically add dependencies to all non-internal demo projects
rootProject.allprojects {
    // Inside allprojects, 'this' is the current project being iterated
    val demoProject = this

    pluginManager.withPlugin("dev.tamboui.demo-project") {
        val demoExt = demoProject.extensions.getByType<DemoExtension>()

        // Add dependency from tamboui-demos to this demo project
        aggregatedDemos.dependencies.addAllLater(demoExt.internal.map { isInternal ->
            if (isInternal) listOf() else listOf(aggregatorProject.project.dependencies.project(demoProject.path))
        })

        // Collect demo info after the demo project is evaluated
        afterEvaluate {
            if (!demoExt.internal.get()) {
                // Create entry for demo info
                val info = DemoInfo(demoProject.path, demoProject.name)
                demoInfoMap[demoProject.path] = info
                val ext = this.extensions.findByType<DemoExtension>()
                val app = this.extensions.findByType<JavaApplication>()

                demoInfoMap[this.path]?.let { demoInfo ->
                    demoInfo.displayName = ext?.displayName?.orNull ?: this.name
                    demoInfo.description = ext?.description?.orNull ?: ""
                    demoInfo.module = ext?.module?.orNull ?: "Other"
                    demoInfo.mainClass = app?.mainClass?.orNull ?: ""
                    demoInfo.tags = ext?.tags?.orNull ?: emptySet()
                }
            }
        }
    }
}

// Task to generate the demo manifest
val generateDemoManifest = tasks.register<GenerateDemoManifestTask>("generateDemoManifest") {
    // Use provider to defer evaluation until task execution
    demos.set(provider {
        demoInfoMap.values
            .filter { it.mainClass.isNotEmpty() }
            .sortedBy { it.name }
            .map { info ->
                GenerateDemoManifestTask.DemoEntry(
                    info.name,
                    info.displayName,
                    info.description,
                    info.module,
                    info.mainClass,
                    info.tags
                )
            }
    })
    outputFile = layout.buildDirectory.file("generated/demos-manifest.json")
}

val mergeServiceFiles = tasks.register<MergeServiceFilesTask>("mergeServiceFiles") {
    classpath.from(configurations.named("runtimeClasspath"))
    classpath.from(aggregatedDemosClasspath)
    outputDirectory = layout.buildDirectory.dir("generated/services")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(generateDemoManifest.map { it.outputFile })
    from(mergeServiceFiles.map { it.outputDirectory })
    from(aggregatedDemosClasspath.incoming.artifacts.resolvedArtifacts.map { artifacts ->
        artifacts.stream()
            .map { result -> {
                val file = result.file
                if (file.isDirectory) file else zipTree(file)
            }}
            .toList()
    }) {
        exclude("META-INF/services/**") // Handled by merge task
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA") // Remove signatures
        exclude("META-INF/MANIFEST.MF") // Use our manifest
        exclude("module-info.class") // Avoid module conflicts
        exclude("META-INF/versions/*/module-info.class")
    }

    manifest {
        attributes(
            "Main-Class" to "dev.tamboui.demos.DemoLauncher",
            "Multi-Release" to "true"
        )
    }
}
