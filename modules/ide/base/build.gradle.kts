plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:domain:telemetry"))

    compileOnly(libs.cangjieCommonForIde)
    compileOnly(libs.cangjiePsiForIde)


    compileOnly(libs.cangjieCfirForIde)
    compileOnly(libs.cangjieAnalysisApiForIde)
    compileOnly(libs.cangjieAnalysisApiCfirForIde)
    compileOnly(libs.cangjieAnalysisApiStandaloneForIde)
    implementation(libs.bundles.jackson)
    implementation(libs.toml4j)
    implementation(libs.jansi)
    testImplementation(testFixtures(project(":modules:test-support")))
    testImplementation(kotlin("test"))
}
