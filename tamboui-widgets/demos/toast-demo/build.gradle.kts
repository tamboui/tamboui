plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Toast widget"

demo {
    tags = setOf("toast", "notification", "overlay")
}

application {
    mainClass.set("dev.tamboui.demo.ToastDemo")
}
