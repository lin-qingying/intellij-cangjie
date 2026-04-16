plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:ide:project"))
    implementation(project(":modules:ide:run"))
    implementation(project(":modules:ide:base"))
    implementation(project(":modules:ide:ux"))

    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)
    implementation(libs.bundles.jackson)
}
