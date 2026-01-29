plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Spinner widget"

demo {
    tags = setOf("spinner", "animation", "loading")
}

application {
    mainClass.set("dev.tamboui.demo.SpinnerDemo")
}
