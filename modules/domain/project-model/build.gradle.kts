plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:ide:ux"))
    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)
}
