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

val generateSearchIndex = tasks.register("generateSearchIndex") {
    dependsOn(tasks.asciidoctor)
    val outDir = layout.buildDirectory.dir("docs")
    outputs.file(layout.buildDirectory.file("docs/search-index.json"))
    doLast {
        val root = outDir.get().asFile
        val htmlFiles = fileTree(root) {
            include("**/*.html")
            exclude("api/**")
        }.files.sortedBy { it.relativeTo(root).invariantSeparatorsPath }

        fun esc(input: String): String = buildString {
            input.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }

        fun stripHtml(html: String): String {
            return html
                .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun titleOf(html: String, fallback: String): String {
            val h1 = Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), " ")
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
            if (!h1.isNullOrBlank()) return h1
            val t = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
            return if (!t.isNullOrBlank()) t else fallback
        }

        val entries = htmlFiles.map { f ->
            val rel = f.relativeTo(root).invariantSeparatorsPath
            val html = f.readText(Charsets.UTF_8)
            val title = titleOf(html, rel.removeSuffix(".html"))
            val body = stripHtml(html)
            "{\"url\":\"${esc(rel)}\",\"title\":\"${esc(title)}\",\"body\":\"${esc(body)}\"}"
        }

        val out = root.resolve("search-index.json")
        out.writeText("[\n" + entries.joinToString(",\n") + "\n]\n", Charsets.UTF_8)
    }
}

tasks.asciidoctor {
    finalizedBy(generateSearchIndex)
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
