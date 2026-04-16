plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(libs.cangjiePsiForIde)
    implementation(libs.cangjieCommonForIde)
    implementation(libs.flatbuffers.java)
}
