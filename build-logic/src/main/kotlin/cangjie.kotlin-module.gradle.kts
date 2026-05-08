import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.gradle.test-retry")
}

dependencies {
    // 统一补齐 Vintage，引导 IntelliJ 插件侧仍然沿用的 JUnit3/JUnit4 测试基类进入 JUnit Platform。
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")
}

kotlin {
    jvmToolchain(21)
}

val isCI = System.getenv("CI") != null

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        freeCompilerArgs.add("-Xcontext-parameters")
        // 上游 for-ide 依赖包含 prerelease Kotlin 元数据，统一放开检查以保证整仓可编译。
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    if (isCI) {
        retry {
            maxRetries.set(3)
            maxFailures.set(5)
        }
    }
}
