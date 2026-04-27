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
