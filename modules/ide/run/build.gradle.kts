plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:ide:base"))
    implementation(project(":modules:ide:debugger-api"))
    implementation(project(":modules:ide:debugger-dap"))
    implementation(project(":modules:ide:debugger-proto"))
    compileOnly(libs.cangjieCommonForIde)
    compileOnly(libs.cangjiePsiForIde)
}
