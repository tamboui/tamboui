import dev.tamboui.build.SplitPackageCheckTask
import dev.tamboui.build.UpdateJBangCatalogTask
import dev.tamboui.build.model.DemosModelBuilder
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

plugins {
    base
    id("dev.tamboui.maven-central-publishing")
}

// Register the Tooling API model builder for demos
serviceOf<ToolingModelBuilderRegistry>()
    .register(DemosModelBuilder())

val splitPackageCheck = tasks.register<SplitPackageCheckTask>("splitPackageCheck") {
    description = "Checks for split packages across library modules"
    group = "verification"

    // Discover all modules by looking for top-level directories with build.gradle.kts
    rootDir.listFiles()
        ?.filter { it.isDirectory && file("${it.name}/build.gradle.kts").exists() }
        ?.forEach { moduleDir ->
            val sourceDir = file("${moduleDir.name}/src/main/java")
            if (sourceDir.exists()) {
                sourceSet(moduleDir.name, fileTree(sourceDir) { include("**/*.java") })
            }
        }

    reportFile.set(layout.buildDirectory.file("reports/split-packages/report.txt"))
}

tasks.named("check") {
    dependsOn(splitPackageCheck)
}

tasks.register<UpdateJBangCatalogTask>("updateJBangCatalog") {
    description = "Updates the jbang-catalog.json file with discovered demos"
    group = "build"

    projectDir = layout.projectDirectory
    this.modules.set(modules)
    catalogFile = layout.projectDirectory.file("jbang-catalog.json")
}
