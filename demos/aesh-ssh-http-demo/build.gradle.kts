plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing TamboUI app accessible via SSH and HTTP/WebSocket using Aesh backend"

demo {
    displayName = "Aesh SSH/HTTP Demo"
    tags = setOf("aesh", "ssh", "http", "websocket", "toolkit", "network")
    internal = true
}

dependencies {
    implementation(projects.tambouiToolkit)
    implementation(projects.tambouiAeshBackend)
    implementation(libs.aesh.terminal.ssh)
    implementation(libs.aesh.terminal.http)
    
    // Apache SSHD for SSH server
    implementation(libs.apache.sshd.core)
    implementation(libs.apache.sshd.netty)
    
    // Netty for HTTP/WebSocket server
    implementation(libs.netty.all)
}

application {
    mainClass.set("dev.tamboui.demo.aesh.AeshSshHttpDemo")
}
