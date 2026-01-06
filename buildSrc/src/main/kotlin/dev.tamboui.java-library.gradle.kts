plugins {
    `java-library`
    id("dev.tamboui.publishing")
}

val isIDEASync = providers.systemProperty("idea.sync.active").isPresent

/**
 * Since we're using Java 8 as baseline, this creates a java11 source set
 * for compiling the module-info.java descriptor.
 * The compiled module-info.class is placed in META-INF/versions/11 to create
 * a Multi-Release JAR that provides module information for Java 11+ while
 * maintaining Java 8 compatibility for the main codebase.
 */
val java11 by sourceSets.creating {
    java {
        srcDir("src/main/java11")
    }
}

tasks.named<JavaCompile>("compileJava11Java") {
    options.release = 11
    // Remove -Werror for module-info compilation as it may have different warnings
    options.compilerArgs.remove("-Werror")

    // Configure module path for module-info.java compilation
    // The main classes and all dependencies need to be on the module path
    modularity.inferModulePath = true
    val moduleName = project.name.replace("tamboui-", "dev.tamboui.")
    doFirst {
        options.compilerArgs.addAll(
            listOf(
                "--module-path", classpath.asPath,
                "--patch-module", "${moduleName}=${sourceSets.main.get().output.classesDirs.asPath}"
            )
        )
        classpath = files()
    }
}

tasks.named<Jar>("jar") {
    into("META-INF/versions/11") {
        from(java11.output)
    }
    manifest {
        attributes("Multi-Release" to "true")
    }
}

configurations.named("java11Implementation") {
    extendsFrom(configurations.getByName("implementation"))
    extendsFrom(configurations.getByName("api"))
}

dependencies {
    val libs = versionCatalogs.named("libs")
    testImplementation(platform(libs.findLibrary("junit.bom").orElseThrow()))
    testImplementation(libs.findBundle("testing").orElseThrow())
    // java11 source set needs access to main classes for module-info compilation
    "java11Implementation"(sourceSets.main.get().output)
}

// This is not how the app will be built, but it allows IntelliJ to properly "see" the modules
// and avoids false-positive errors in the IDE.
if (isIDEASync) {
    sourceSets.main.get().java.srcDir("src/main/java11")
    tasks.withType<JavaCompile>().configureEach {
        options.release = 11
    }
}