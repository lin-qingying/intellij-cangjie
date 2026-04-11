plugins {
    id("cangjie-module")
}

dependencies {
 
    implementation(project(":toolchain"))
    implementation(project(":telemetry"))
    implementation(project(":icon"))
    implementation(project(":highlighter"))
    implementation(project(":formatter"))
    implementation(project(":messages"))
    implementation(project(":notifications"))
    implementation(project(":cangjie-project"))
    implementation(project(":common"))
    implementation(project(":util"))

    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)
    implementation(libs.cangjieCfirForIde)
    implementation(libs.cangjieAnalysisApiForIde)
    implementation(libs.cangjieAnalysisApiCfirForIde)
    implementation(libs.cangjieAnalysisApiStandaloneForIde)

    implementation(libs.jansi)
    implementation(libs.toml4j)
    implementation(libs.bundles.jackson)
}