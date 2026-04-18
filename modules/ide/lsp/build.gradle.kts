plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:domain:telemetry"))
    implementation(project(":modules:ide:base"))
    compileOnly(libs.cangjieCommonForIde)
    compileOnly(libs.cangjieAnalysisApiForIde)
    compileOnly(libs.cangjieAnalysisApiCfirForIde)

    intellijPlatform {
        plugins("com.redhat.devtools.lsp4ij:0.19.2")
    }
}
