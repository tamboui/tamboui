import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.graalvm.native)
    implementation(libs.nexus.publishing.plugin)
    implementation(libs.asciidoctor.plugin)
    implementation(libs.git.publish.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.test.logger.gradle.plugin)

    // junit-jupiter / junit-platform-launcher are declared without a version in
    // the catalog; their versions come from the junit BOM. Without this platform
    // the version is empty ("Could not find org.junit.jupiter:junit-jupiter:").
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
   compilerOptions {
       jvmTarget = JvmTarget.JVM_21
   }
}
