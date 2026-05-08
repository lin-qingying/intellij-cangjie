import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("cangjie.intellij-module")
}

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
    }
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
    testImplementation(libs.junit4)
    testImplementation(kotlin("test"))
    testImplementation(libs.cangjieCommonForIde)
    testImplementation(libs.cangjiePsiForIde)
    testImplementation(libs.cangjieCfirForIde)
    testImplementation(libs.cangjieAnalysisApiForIde)
    testImplementation(libs.cangjieAnalysisApiCfirForIde)
    testImplementation(libs.cangjieAnalysisApiStandaloneForIde)
}
