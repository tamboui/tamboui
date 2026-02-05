plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing TamboUI app accessible via SSH and HTTP/WebSocket using Aesh backend"

demo {
    displayName = "Aesh SSH/HTTP Demo"
    tags = setOf("aesh", "ssh", "http", "websocket", "toolkit", "network")
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiAeshBackend)
    implementation(libs.aesh.terminal.ssh)
    implementation(libs.aesh.terminal.http)
    
    // Apache SSHD for SSH server
    implementation("org.apache.sshd:sshd-core:2.14.0")
    implementation("org.apache.sshd:sshd-netty:2.14.0")
    
    // Netty for HTTP/WebSocket server
    implementation("io.netty:netty-all:4.1.81.Final")
}

application {
    mainClass.set("dev.tamboui.demo.aesh.AeshSshHttpDemo")
}
