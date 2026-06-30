plugins {
    id("java")
    id("application")
}

description = "Demo showcasing the Divider component"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":tamboui-toolkit"))
    runtimeOnly(project(":tamboui-jline3-backend"))
}

application {
    mainClass.set("dev.tamboui.demo.DividerDemo")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks.named("run", JavaExec::class).configure {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
