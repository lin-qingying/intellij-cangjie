plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    compileOnly(libs.cangjiePsiForIde)
    compileOnly(libs.cangjieCommonForIde)
    implementation(libs.flatbuffers.java)
}
