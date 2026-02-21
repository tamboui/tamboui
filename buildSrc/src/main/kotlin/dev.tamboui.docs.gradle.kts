import dev.tamboui.build.GenerateDemosGalleryTask
import dev.tamboui.build.JavadocAggregatorPlugin

plugins {
    id("org.asciidoctor.jvm.convert")
    id("org.ajoberstar.git-publish")
    `java-base`  // For source set infrastructure (snippets compilation)
}

pluginManager.apply(JavadocAggregatorPlugin::class)

repositories {
    mavenCentral()
}

// Source set for documentation code snippets
// These are compiled to ensure code examples in documentation are valid
val snippets = the<SourceSetContainer>().create("snippets") {
    java.srcDir("src/snippets/java")
}

// Configuration to resolve demo cast files from demo projects
val demoCasts = configurations.create("demoCasts") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-cast"))
    }
}
val demoScreenshots = configurations.create("demoScreenshots") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-screenshots"))
    }
}

// Configuration to resolve demo metadata files from demo projects
val demoMetadata = configurations.create("demoMetadata") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-metadata"))
    }
}

val copyCasts = tasks.register<Sync>("copyCasts") {
    from(demoCasts)
    destinationDir = layout.buildDirectory.dir("generated/docs/demos").get().asFile
}
val copyScreenshots = tasks.register<Sync>("copyScreenshots") {
    from(demoScreenshots)
    destinationDir = layout.buildDirectory.dir("generated/docs/screenshots").get().asFile
}

// Task to generate the demos gallery AsciiDoc pages (index + per-module)
val generateDemosPage = tasks.register<GenerateDemosGalleryTask>("generateDemosPage") {
    metadataFiles.from(demoMetadata)
    title = "Demo Gallery"
    castBasePath = "demos"
    outputDirectory = layout.buildDirectory.dir("generated/asciidoc")
}

rootProject.allprojects {
    pluginManager.withPlugin("dev.tamboui.demo-project") {
        dependencies {
            demoCasts.dependencies.add(project(project.path))
            demoScreenshots.dependencies.add(project(project.path))
            demoMetadata.dependencies.add(project(project.path))
        }
    }
}

// Prepare combined asciidoc sources (static + generated)
val prepareAsciidocSources = tasks.register<Sync>("prepareAsciidocSources") {
    dependsOn(generateDemosPage)
    from("src/docs/asciidoc")
    from(layout.buildDirectory.dir("generated/asciidoc"))
    into(layout.buildDirectory.dir("asciidoc-sources"))
}

tasks.asciidoctor {
    val javadoc = tasks.named<Javadoc>("javadoc")
    val snippetsClasses = tasks.named("snippetsClasses")
    inputs.files(prepareAsciidocSources)
    inputs.files(copyCasts)
    inputs.files(copyScreenshots)
    inputs.files(javadoc)
    // Ensure code snippets compile before building docs
    dependsOn(snippetsClasses)

    setSourceDir(layout.buildDirectory.dir("asciidoc-sources").get().asFile)
    setBaseDir(layout.buildDirectory.dir("asciidoc-sources").get().asFile)
    setOutputDir(layout.buildDirectory.dir("docs"))

    // Copy theme resources
    resources {
        from("src/theme") {
            into("_static")
        }
        // Copy demo cast files from resolved configuration
        from(copyCasts) {
            into("demos")
        }
        // Copy screenshot files from resolved configuration
        from(copyScreenshots) {
            into("screenshots")
        }
        // Copy javadocs
        from(javadoc.map { it.destinationDir }) {
            into("api")
        }
        // Copy versions.json to docs root when present (for version selector dropdown)
        from(project.layout.projectDirectory) {
            include("versions.json")
            into(".")
        }
    }

    attributes(
        mapOf(
            "source-highlighter" to "highlight.js",
            "highlightjsdir" to "_static/highlight",
            "highlightjs-theme" to "github-dark",
            "stylesheet" to "_static/tamboui.css",
            "linkcss" to true,
            "icons" to "font",
            "toc" to "left",
            "toclevels" to 3,
            "sectanchors" to true,
            "sectlinks" to true,
            "idprefix" to "",
            "idseparator" to "-",
            "source-indent" to 0,
            "tabsize" to 4,
            // Docinfo for theme toggle and navigation
            "docinfo" to "shared",
            // Project info
            "project-version" to project.version,
            "project-name" to "TamboUI",
            "github-repo" to "tamboui/tamboui",
            // Snippet source directory for include directives
            "snippets-dir" to project.file("src/snippets/java").absolutePath,
            // Suppress timestamps for reproducible builds
            "reproducible" to true
        )
    )
}

// Git publish configuration for publishing documentation to tamboui.dev
// For SNAPSHOT versions (main branch): publish to docs/main
// For release versions (tags): publish to docs/<version>
// The generated docs include a version dropdown that reads docs/versions.json when available.
// Format: JSON object mapping display name -> path segment (relative to docs/). Example:
//   { "main": "main", "0.1.0": "0.1.0", "latest": "0.1.0", "0.2.0-snapshot": "main" }
// URL for a version is <origin>/docs/<pathSegment>/<page> (pathSegment must not contain '/').
val targetFolder = providers.provider {
    val version = project.version.toString()
    if (version.endsWith("-SNAPSHOT")) "docs/main" else "docs/$version"
}

val currentVersionSegment = providers.provider {
    targetFolder.get().removePrefix("docs/")
}

// Generate docs/versions.json from folder names on gh-pages (clones repo to list docs/* then writes JSON)
val generateVersionsJson = tasks.register<DefaultTask>("generateVersionsJson") {
    val cloneDir = layout.buildDirectory.dir("docs-gh-pages-clone")
    val outputFile = layout.buildDirectory.file("versions.json")
    outputs.file(outputFile)
    inputs.property("targetFolder", targetFolder)
    inputs.property("currentSegment", currentVersionSegment)

    doLast {
        val repo = cloneDir.get().asFile
        if (!repo.isDirectory || !repo.resolve(".git").isDirectory) {
            repo.mkdirs()
            val proc = ProcessBuilder(
                "git", "clone", "--depth", "1",
                "--branch", "gh-pages",
                "https://github.com/tamboui/tamboui.dev.git",
                repo.absolutePath
            ).directory(repo.parentFile).redirectErrorStream(true).start()
            val ok = proc.waitFor() == 0
            if (!ok) throw GradleException("git clone failed with exit code ${proc.exitValue()}")
        }
        val docsDir = repo.resolve("docs")
        val segments = mutableSetOf<String>()
        if (docsDir.isDirectory) {
            docsDir.listFiles()?.filter { f -> f.isDirectory }?.forEach { f -> segments.add(f.name) }
        }
        segments.add(currentVersionSegment.get())
        val sorted = segments.sortedWith(compareBy<String> { if (it == "main") "" else it })
        val map = sorted.associateWith { seg -> seg }
        val json = map.entries.joinToString(", ") { e -> """ "${e.key}": "${e.value}" """ }
        outputFile.get().asFile.writeText("{$json}\n")
    }
}

gitPublish {
    repoUri.set("https://github.com/tamboui/tamboui.dev.git")
    branch.set("gh-pages")
    sign.set(false)

    contents {
        from(tasks.asciidoctor) {
            into(targetFolder)
        }
        from(generateVersionsJson) {
            into("docs")
        }
    }

    preserve {
        include("**")
        exclude(targetFolder.map { "$it/**" }.get())
    }

    commitMessage.set(targetFolder.map { "Publishing documentation to $it" })
}
