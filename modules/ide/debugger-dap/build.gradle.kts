plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:ide:debugger-api"))
    implementation(project(":modules:ide:base"))
    implementation(libs.cangjiePsiForIde)
    implementation(libs.cangjieCommonForIde)
    implementation(libs.lsp4j.debug)
}
