plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing the Calendar widget for monthly calendar views"

demo {
    tags = setOf("calendar", "block", "paragraph", "date")
}

application {
    mainClass.set("dev.tamboui.demo.CalendarDemo")
}
