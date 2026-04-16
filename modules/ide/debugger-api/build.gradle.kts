plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:ide:base"))
    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)
}
