import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("cangjie.intellij-product")
}

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
    }
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:domain:package-manager"))
    implementation(project(":modules:domain:telemetry"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:ide:base"))
    implementation(project(":modules:ide:project"))
    implementation(project(":modules:ide:run"))
    implementation(project(":modules:ide:lsp"))
    implementation(project(":modules:ide:debugger-api"))
    implementation(project(":modules:ide:debugger-dap"))
    implementation(project(":modules:ide:debugger-proto"))

    // 产品层统一声明并打包外部运行时制品。
    // 业务模块只声明编译所需依赖，真正随插件分发的第三方与前端产物在这里收口。
    runtimeOnly(libs.cangjieCommonForIde)
    runtimeOnly(libs.cangjiePsiForIde)
    runtimeOnly(libs.cangjieCfirForIde)
    runtimeOnly(libs.cangjieAnalysisApiForIde)
    runtimeOnly(libs.cangjieAnalysisApiCfirForIde)
    runtimeOnly(libs.cangjieAnalysisApiStandaloneForIde)
    runtimeOnly(libs.bundles.jackson)
    runtimeOnly(libs.toml4j)
    runtimeOnly(libs.jansi)
    runtimeOnly(libs.vavr)
    runtimeOnly(libs.lsp4j.debug)
    runtimeOnly(libs.bundles.protobuf)
    runtimeOnly("com.squareup.okio:okio:3.9.0")
    runtimeOnly("io.github.oshai:kotlin-logging-jvm:6.0.9")
    runtimeOnly("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    testImplementation(testFixtures(project(":modules:test-support")))
    testImplementation(libs.junit4)
    testImplementation(kotlin("test"))
    testImplementation(libs.cangjieCommonForIde)
    testImplementation(libs.cangjiePsiForIde)
    testImplementation(libs.cangjieCfirForIde)
    testImplementation(libs.cangjieAnalysisApiForIde)
    testImplementation(libs.cangjieAnalysisApiCfirForIde)
    testImplementation(libs.cangjieAnalysisApiStandaloneForIde)
}
