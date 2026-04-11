import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
    id("org.gradle.test-retry")
}

kotlin {
    jvmToolchain(21)
}

val isCI = System.getenv("CI") != null

// 不声明任何 intellijPlatform 依赖，由根项目通过 pluginComposedModule 注入

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks.withType<Test> {
    systemProperty("java.awt.headless", "true")
    if (isCI) {
        extensions.findByType<org.gradle.testretry.TestRetryTaskExtension>()?.apply {
            maxRetries.set(3)
            maxFailures.set(5)
        }
    }
}