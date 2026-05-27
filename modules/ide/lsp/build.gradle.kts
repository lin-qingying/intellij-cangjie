plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:domain:telemetry"))
    implementation(project(":modules:ide:base"))
    // `modules:ide:base` 只保留历史包名下的 typealias，
    // 真正的 TextAttributesKey 仍定义在共享 code-insight/highlighting 制品里。
    // LSP 侧继续复用同一套高亮 key，就必须把该制品放进本模块编译类路径。
    compileOnly(libs.cangjieCodeInsightHighlightingForIde)
    compileOnly(libs.cangjieCommonForIde)
    compileOnly(libs.cangjieAnalysisApiForIde)
    compileOnly(libs.cangjieAnalysisApiCfirForIde)

    intellijPlatform {
        plugins("com.redhat.devtools.lsp4ij:0.19.2")
    }
}
