import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
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
        "-Xmaxwarns", "10",
        "-Xmaxerrs", "10",
        "-Werror",
        "-Xlint:-options"
    ))
    options.release = 8
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    // Javadoc may not overwrite previously-copied add-stylesheet/add-script artifacts on reruns.
    // Delete the known output locations so theme tweaks reliably show up without a manual clean.
    //
    // Note: javadoc's --add-script option is not reliably additive across Gradle/Javadoc versions,
    // so we generate a single combined script which contains both the GitHub config and the theme UI.
    val combinedScript = project.layout.buildDirectory.file("tmp/javadoc/tamboui-javadoc.js").get().asFile
    doFirst {
        val outDir = destinationDir
        delete(
            File(outDir, "resource-files/javadoc.css"),
            File(outDir, "javadoc.css"),
            File(outDir, "script-files/tamboui-javadoc.js"),
            File(outDir, "script-dir/tamboui-javadoc.js"),
            // Old names from earlier iterations
            File(outDir, "script-files/javadoc-theme.js"),
            File(outDir, "script-dir/javadoc-theme.js"),
            File(outDir, "script-files/tamboui-javadoc-config.js"),
            File(outDir, "script-dir/tamboui-javadoc-config.js")
        )

        combinedScript.parentFile.mkdirs()
        val repo = providers.gradleProperty("tamboui.githubRepo").orElse("tamboui/tamboui").get()
        val ref = providers.gradleProperty("tamboui.githubRef").orElse("main").get()
        val themeScript = rootProject.file("docs/src/theme/javadoc-theme.js")
        val themeContent = if (themeScript.exists()) themeScript.readText() else ""
        combinedScript.writeText(
            "window.__TAMBOUI_GITHUB_REPO=" + "'" + repo + "'" + ";\n" +
                "window.__TAMBOUI_GITHUB_REF=" + "'" + ref + "'" + ";\n" +
                themeContent
        )
    }

    // Align Javadoc HTML styling with the Asciidoctor theme (docs/src/theme).
    // We add a supplemental stylesheet rather than replacing the built-in one.
    val themeStylesheet = rootProject.file("docs/src/theme/javadoc.css")
    if (themeStylesheet.exists()) {
        val opts = options as StandardJavadocDocletOptions
        // Javadoc option is GNU-style: --add-stylesheet. Gradle prefixes "-" automatically, so we
        // provide a leading "-" to end up with "--add-stylesheet".
        opts.addFileOption("-add-stylesheet", themeStylesheet)
    }

    // Provide GitHub repo/ref config + theme toggle + "View on GitHub" link via a single script.
    // Can be overridden with:
    // -Ptamboui.githubRepo=tamboui/tamboui
    // -Ptamboui.githubRef=<branch-or-tag>
    (options as StandardJavadocDocletOptions).addFileOption("-add-script", combinedScript)

    // Limit warning/error output volume (non-standard javadoc options).
    (options as StandardJavadocDocletOptions).addStringOption("Xmaxwarns", "10")
    (options as StandardJavadocDocletOptions).addStringOption("Xmaxerrs", "10")

    // Generate HTML with links to the source code for types and members.
    // (Javadoc option: -linksource)
    //(options as StandardJavadocDocletOptions).addBooleanOption("linksource", true)
}

group = "dev.tamboui"
