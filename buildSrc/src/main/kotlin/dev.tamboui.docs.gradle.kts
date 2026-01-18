import dev.tamboui.build.GenerateDemosGalleryTask
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.io.File

plugins {
    id("org.asciidoctor.jvm.convert")
    id("org.ajoberstar.git-publish")
}

repositories {
    mavenCentral()
}

// Classpath for aggregate Javadoc generation.
//
// IMPORTANT: We must not resolve other projects' configurations directly from here (Gradle 9+ will
// fail with "unsafe configuration resolution"). Instead we create our own resolvable configuration
// and depend on the *consumable* runtimeElements variant of each module.
val aggregateJavadocClasspath = configurations.create("aggregateJavadocClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

fun selectedJavadocProjects(): List<Project> {
    // Include everything relevant, and explicitly exclude what you don't want.
    // This scales better as modules are added/removed.
    //
    // Example:
    //   -Ptamboui.docs.javadocExcludeModules=tamboui-panama-backend,tamboui-image
    // Default: exclude the panama backend (it is JDK/toolchain sensitive and not part of the
    // published website API surface yet). Set the property to an empty string to include it.
    val configuredExcludes = providers.gradleProperty("tamboui.docs.javadocExcludeModules")
        .orElse("tamboui-panama-backend")
        .get()
    val excludedNames = mutableSetOf<String>()
    if (configuredExcludes.isNotBlank()) {
        excludedNames.addAll(configuredExcludes.split(',').map { it.trim() }.filter { it.isNotEmpty() })
    }

    return rootProject.subprojects
        .filter { it.name.startsWith("tamboui-") }
        .filterNot { it.name == "docs" }
        .filterNot { it.path.contains(":demos:") }
        .filterNot { excludedNames.contains(it.name) }
}

// Configuration to resolve demo cast files from demo projects
val demoCasts = configurations.create("demoCasts") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("demo-cast"))
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

/**
 * Aggregated Javadoc for the website.
 *
 * We generate a single unified Javadoc across the relevant library modules (excluding demos),
 * and then publish it alongside the Asciidoctor site under {@code javadoc/}.
 */
val aggregateJavadocOutputDir = layout.buildDirectory.dir("generated/javadoc")
val aggregateJavadoc = tasks.register<Javadoc>("aggregateJavadoc") {
    group = "documentation"
    description = "Generates a unified Javadoc across library modules for publishing on the website"

    destinationDir = aggregateJavadocOutputDir.get().asFile

    val libraryProjects = selectedJavadocProjects()

    // Sources: only main java sources, avoid java11/module-info multi-release sources.
    libraryProjects.forEach { p ->
        val srcDir = p.file("src/main/java")
        if (srcDir.exists()) {
            source(p.fileTree(srcDir) { include("**/*.java") })
        }
    }

    // Classpath: resolved from this project configuration (see above).
    classpath = aggregateJavadocClasspath

    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        docEncoding = "UTF-8"
        charSet = "UTF-8"
        windowTitle = "TamboUI API"
        docTitle = "TamboUI API"
    }

    // Theme: reuse the same stylesheet/script we use for per-module Javadoc.
    val themeStylesheet = rootProject.file("docs/src/theme/javadoc.css")
    if (themeStylesheet.exists()) {
        (options as StandardJavadocDocletOptions).addFileOption("-add-stylesheet", themeStylesheet)
    }

    val combinedScript = layout.buildDirectory.file("tmp/javadoc/tamboui-javadoc.js").get().asFile
    doFirst {
        delete(
            File(destinationDir, "resource-files/javadoc.css"),
            File(destinationDir, "javadoc.css"),
            File(destinationDir, "script-files/tamboui-javadoc.js"),
            File(destinationDir, "script-dir/tamboui-javadoc.js")
        )

        combinedScript.parentFile.mkdirs()
        val repo = providers.gradleProperty("tamboui.githubRepo").orElse("tamboui/tamboui").get()
        val ref = providers.gradleProperty("tamboui.githubRef").orElse("main").get()
        val themeScript = rootProject.file("docs/src/theme/javadoc-theme.js")
        val themeContent = if (themeScript.exists()) themeScript.readText() else ""

        fun jsonEscape(s: String): String {
            // Minimal JSON string escape (enough for file paths + Java identifiers).
            return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        // For aggregated Javadoc we can't reliably infer the Gradle module name from the page title.
        // Instead, we generate a mapping so the JS can build correct GitHub links.
        //
        // Keep this small: only map package -> source directory. The JS can derive the class file name
        // from the URL (Outer.Inner.html -> Outer.java) and join it with the mapped package directory.
        val packageToDirPath = linkedMapOf<String, String>()
        val ambiguousPackages = mutableSetOf<String>()
        libraryProjects.forEach { p ->
            val srcDir = p.file("src/main/java")
            if (!srcDir.exists()) {
                return@forEach
            }
            srcDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .forEach { f ->
                    val rel = f.relativeTo(srcDir).invariantSeparatorsPath
                    val pkgPath = rel.substringBeforeLast('/', "")
                    val pkg = if (pkgPath.isEmpty()) "" else pkgPath.replace('/', '.')
                    val type = f.name.removeSuffix(".java")
                    if (pkg.isNotEmpty()) {
                        val dirPath = "${p.name}/src/main/java/$pkgPath"
                        val existing = packageToDirPath[pkg]
                        if (existing == null) {
                            if (!ambiguousPackages.contains(pkg)) {
                                packageToDirPath[pkg] = dirPath
                            }
                        } else if (existing != dirPath) {
                            packageToDirPath.remove(pkg)
                            ambiguousPackages.add(pkg)
                        }
                    }
                    // Intentionally do not record per-class mappings to avoid large inline scripts.
                }
        }
        val pkgMapJson = packageToDirPath.entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { (k, v) -> "\"" + jsonEscape(k) + "\":\"" + jsonEscape(v) + "\"" }

        combinedScript.writeText(
            "window.__TAMBOUI_GITHUB_REPO=" + "'" + repo + "'" + ";\n" +
                "window.__TAMBOUI_GITHUB_REF=" + "'" + ref + "'" + ";\n" +
                "window.__TAMBOUI_GITHUB_PACKAGE_TO_DIR_PATH=" + pkgMapJson + ";\n" +
                themeContent
        )
    }
    (options as StandardJavadocDocletOptions).addFileOption("-add-script", combinedScript)
}

// Populate aggregate Javadoc classpath with module outputs + their runtime deps.
dependencies {
    selectedJavadocProjects().forEach { p ->
        add("aggregateJavadocClasspath", project(mapOf("path" to p.path, "configuration" to "runtimeElements")))
    }
}

tasks.asciidoctor {
    dependsOn(prepareAsciidocSources, copyCasts, aggregateJavadoc)

    setSourceDir(layout.buildDirectory.dir("asciidoc-sources").get().asFile)
    setBaseDir(layout.buildDirectory.dir("asciidoc-sources").get().asFile)
    setOutputDir(layout.buildDirectory.dir("docs"))

    // Copy theme resources
    resources {
        from("src/theme") {
            into("_static")
        }
        // Publish aggregated Javadoc under docs/javadoc/
        from(aggregateJavadocOutputDir) {
            into("javadoc")
        }
        // Copy demo cast files from resolved configuration
        from(copyCasts) {
            into("demos")
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
            "github-repo" to "tamboui/tamboui"
        )
    )
}

// Git publish configuration for publishing documentation to tamboui.dev
// For SNAPSHOT versions (main branch): publish to docs/main
// For release versions (tags): publish to docs/<version>
val targetFolder = providers.provider {
    val version = project.version.toString()
    if (version.endsWith("-SNAPSHOT")) "docs/main" else "docs/$version"
}

gitPublish {
    repoUri.set("git@github.com:tamboui/tamboui.dev.git")
    branch.set("gh-pages")
    sign.set(false)

    contents {
        from(tasks.asciidoctor) {
            into(targetFolder)
        }
    }

    preserve {
        include("**")
        exclude(targetFolder.map { "$it/**" }.get())
    }

    commitMessage.set(targetFolder.map { "Publishing documentation to $it" })
}
