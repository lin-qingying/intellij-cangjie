import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("cangjie.intellij-module")
}

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
    }

    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:ide:ux"))
    implementation(project(":modules:ide:run"))
    implementation(project(":modules:ide:base"))
    compileOnly(libs.cangjieCommonForIde)
    compileOnly(libs.cangjiePsiForIde)

    testImplementation(testFixtures(project(":modules:test-support")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
}
