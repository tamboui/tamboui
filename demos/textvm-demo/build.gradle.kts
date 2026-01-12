plugins {
    id("dev.tamboui.demo-project")
}

description = "VisualVM-like Java process monitor demo"

dependencies {
    implementation(projects.tambouiWidgets)
    implementation(projects.tambouiJline)
    // Attach API is in tools.jar, but for Java 9+ it's in jdk.attach module
    // We'll use reflection to access it
}

application {
    mainClass.set("dev.tamboui.demo.JTextVM")
    applicationDefaultJvmArgs = listOf(
        "--add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED",
        "--add-opens=jdk.internal.jvmstat/sun.jvmstat.perfdata.monitor.protocol.local=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED"
    )
}

