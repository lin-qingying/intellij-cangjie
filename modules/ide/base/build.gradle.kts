plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:domain:telemetry"))

    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)


    implementation(libs.cangjieCfirForIde)
    implementation(libs.cangjieAnalysisApiForIde)
    implementation(libs.cangjieAnalysisApiCfirForIde)
    implementation(libs.cangjieAnalysisApiStandaloneForIde)
    implementation(libs.bundles.jackson)
    implementation(libs.toml4j)
    implementation(libs.jansi)
    testImplementation(testFixtures(project(":modules:test-support")))
    testImplementation(kotlin("test"))
}
