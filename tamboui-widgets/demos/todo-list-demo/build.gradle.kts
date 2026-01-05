plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing Todo List application"

demo {
    displayName = "Todo List"
    tags = setOf("list", "block", "paragraph", "interactive", "application")
}

application {
    mainClass.set("dev.tamboui.demo.TodoListDemo")
}

