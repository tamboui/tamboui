plugins {
    id("dev.tamboui.demo-project")
}

description = "Demo showcasing streaming video (mp4/gif/etc) into the Image widget via ffmpeg frame decoding"

demo {
    displayName = "Video"
    tags = setOf("video", "image", "graphics", "ffmpeg", "streaming", "native-protocols")
}

dependencies {
    implementation(project(":tamboui-image"))
}

application {
    mainClass.set("dev.tamboui.demo.VideoDemo")
}


