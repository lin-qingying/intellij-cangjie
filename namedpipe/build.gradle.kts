plugins {
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}
dependencies {
    // ── JNA ──────────────────────────────────────────────────────────────────
    // jna-platform 已内置 Kernel32/W32FileIO 等 Windows API 高层封装，
    // 替换原先的手写 Kernel32Ex binding，减少约 80 行样板代码
    // 使用 compileOnly：运行时由 IDE 平台提供（IDE 通过 JnaLoader 正确初始化 JNA）。
    // 若以 implementation 打包，插件 classloader（child-first）会加载自己的 JNA 副本，
    // 绕过 IDE 的 JnaLoader 初始化，导致 UnsatisfiedLinkError
    compileOnly("net.java.dev.jna:jna:5.14.0")
    compileOnly("net.java.dev.jna:jna-platform:5.14.0")

    // ── Kotlin Coroutines ─────────────────────────────────────────────────────
    // 替换所有 Executors.newSingleThreadExecutor + 回调风格异步，
    // waitForConnectionAsync / connectAsync 改为 suspend fun，代码量减少 ~60%
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // ── Okio ─────────────────────────────────────────────────────────────────
    // Square Okio 提供 BufferedSource / BufferedSink，
    // 统一替换 Unix 端手工 BufferedInputStream/OutputStream 包装，
    // 并提供 readUtf8Line() / writeUtf8() 等便捷 API，消除 PipeExtensions 中的重复代码
    implementation("com.squareup.okio:okio:3.9.0")

    // ── kotlin-logging + SLF4J ────────────────────────────────────────────────
    // 门面式日志，替换裸 println，在 IntelliJ 插件/IDE 工具窗口中正确路由日志
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
    implementation("org.slf4j:slf4j-api:2.0.13")
    // 测试/独立运行时的 SLF4J 后端（插件环境由 IntelliJ 自带 log4j 接管）
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    // ── 测试 ──────────────────────────────────────────────────────────────────
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("net.java.dev.jna:jna:5.14.0")
    testImplementation("net.java.dev.jna:jna-platform:5.14.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
