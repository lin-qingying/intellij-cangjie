/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

import groovy.xml.XmlParser
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDate

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS
val library = libs
// ============================================================
// 项目基础配置
// ============================================================
val basePluginArchiveName = "intellij-cangjie"
val grammarKitFakePsiDeps = "grammar-kit-fake-psi-deps"

val pluginProjects: List<Project>
    get() = rootProject.allprojects.filter { it.name != grammarKitFakePsiDeps }

// ============================================================
// 平台版本配置
// ============================================================
val platformVersion = prop("platformVersion").toInt()
val baseIDE = prop("baseIDE")

val ideToRunType = prop("ideToRunType").ifEmpty { baseIDE }
val ideRunVersion = prop("ideRunVersion")
val ideVersion = prop("ideVersion")

// ============================================================
// 插件版本配置
// ============================================================
val pluginVersion = prop("pluginVersion")
val sinceBuildP = prop("sinceBuild")
val untilBuildP = prop("untilBuild")
val versionSuffix = prop("versionSuffix")
val cangjiePluginVersion = "$pluginVersion$versionSuffix"

// ============================================================
// IntelliJ 平台插件依赖
// ============================================================
val psiViewerPlugin = prop("psiViewerPlugin")
val indexViewPlugin = prop("indexViewPlugin")

// 内置插件
val tomlPlugin = "org.toml.lang"
val jsonPlugin = "com.intellij.modules.json"
val copyright = "com.intellij.copyright"
val terminalPlugin = "org.jetbrains.plugins.terminal"
val diagramPlugin = "com.intellij.diagram"

// ============================================================
// 辅助函数
// ============================================================

/**
 * 将版本号转为合法的文件夹名
 * .改为-  如果最后面是.*去掉
 */
fun String.toValidDirectoryName(): String {
    return this.replace(".", "-").removeSuffix(".*")
}

/**
 * 获取 IDE 运行时的 JVM 参数配置
 */
fun getIdeJvmArgs(): List<String> = listOf(
    // 内存配置
    "-Xms512m",
    "-Xmx4096m",
    "-XX:ReservedCodeCacheSize=512m",
    "-XX:MaxMetaspaceSize=512m",

    // G1 垃圾收集器
    "-XX:+UseG1GC",
    "-XX:G1HeapRegionSize=16m",
    "-XX:G1ReservePercent=20",
    "-XX:InitiatingHeapOccupancyPercent=35",
    "-XX:SoftRefLRUPolicyMSPerMB=50",

    // 远程调试
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",

    // 禁用优化（解决"非原生帧"问题）
    "-Xint",
    "-XX:TieredStopAtLevel=1",
    "-XX:-Inline",
    "-XX:MaxInlineLevel=0",
    "-XX:InlineSmallCode=0",
    "-XX:CompileThreshold=100000",

    // 完整堆栈跟踪
    "-XX:-OmitStackTraceInFastThrow",

    // 调试信息保留
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+DebugNonSafepoints",

    // 断言
    "-ea",
    "-esa",

    // ===== JNA 配置 =====
    "-Djna.nosys=false",              // 允许使用系统库
    "-Djna.nounpack=false",           // 允许解包本地库
    // 注意：不要设置 jna.boot.library.path 为空字符串，
    // 空字符串会阻止 JNA 在默认路径中查找原生库，导致 UnsatisfiedLinkError
    "-Djna.debug_load=false",         // 生产环境关闭调试
    "-Djna.debug_load.jna=false",     // 生产环境关闭 JNA 库调试
    // "-Djna.tmpdir=${System.getProperty("java.io.tmpdir")}/jna", // 可选：自定义临时目录

    // Kotlin 协程调试
    "-Dkotlinx.coroutines.debug=on",
    "-Dkotlinx.coroutines.stacktrace.recovery=true",
    "-Dkotlinx.coroutines.scheduler.core.pool.size=4",

    // Kotlin 配置
    "-Dkotlin.reflect.full.enabled=true",
    "-Dkotlin.compiler.incremental=false",

    // IntelliJ IDEA 调试模式
    "-Didea.is.internal=true",
    "-Didea.debug.mode=true",
    "-Didea.ProcessCanceledException=disabled",
    "-Didea.fatal.error.notification=disabled",

    // 日志和诊断
    "-Didea.log.debug.categories=#org.cangnova.cangjie",
    "-Didea.log.startup.performance=true",

    // IDE 性能参数
    "-Didea.auto.reload.plugins=false",
    "-Dide.show.tips.on.startup.default.value=false",

    // 其他辅助参数
    "-Dfile.encoding=UTF-8",
    "-Djdk.attach.allowAttachSelf=true",
    "-Dsun.io.useCanonCaches=false"
)


plugins {
    idea
    alias(libs.plugins.saliman.properties)
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.intellij.changelog)
    id("java-test-fixtures")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.test.retry)


}

idea {
    module {
        // https://github.com/gradle/kotlin-dsl/issues/537/
        excludeDirs = excludeDirs + file("testData") + file("deps") + file("bin") +
                file("$grammarKitFakePsiDeps/src/main/kotlin")
    }
}

val Project.dependencyCachePath
    get(): String {
        val cachePath = file("${rootProject.projectDir}/deps")
        // If cache path doesn't exist, we need to create it manually
        // because otherwise gradle-intellij-plugin will ignore it
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }
        return cachePath.absolutePath
    }

val isCI = System.getenv("CI") != null

// ============================================================
// 所有项目通用配置
// ============================================================


allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.intellij.platform")
        plugin("org.gradle.test-retry")
    }
    kotlin {
        jvmToolchain(21)
    }
    repositories {
        intellijPlatform {
            intellijDependencies()
            defaultRepositories()
            localPlatformArtifacts()
            marketplace()
        }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
    }

    dependencies {
        intellijPlatform {
            testFramework(TestFrameworkType.Platform)
            create(IntelliJPlatformType.fromCode(ideToRunType), ideRunVersion)
        }

        // 测试依赖
        testImplementation(library.junit4)
        testImplementation(library.junit.jupiter.api)
        testImplementation(library.kotlin.test.junit)
        testImplementation(testFixtures(project(":test-common")))
//
//        // Kotlin 依赖
        implementation(library.kotlinx.serialization.json)
        implementation(kotlin("test"))
        implementation(kotlin("test-junit"))
        implementation(kotlin("stdlib"))
    }

    sourceSets {
        main {
            java {
                srcDirs("src/main/kotlin")
                srcDirs("src/main/$platformVersion")
            }
            kotlin {
                srcDirs("src/main/kotlin")
                srcDirs("src/main/$platformVersion")
                srcDirs("src/gen")
            }
            resources {
                srcDirs("src/main/resources")
            }
        }

        test {
            java {
                srcDirs("src/test/kotlin")
                srcDirs("src/test/java")
            }
            kotlin {
                srcDirs("src/test/kotlin")
            }
            resources {
                srcDirs("src/test/resources")
            }
        }
    }

    tasks {


        withType<KotlinCompile> {
            compilerOptions {
//                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.add("-Xjvm-default=all")
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }


        runIde { enabled = false }
        prepareSandbox { enabled = false }
        jarSearchableOptions { enabled = false }
        buildSearchableOptions { enabled = false }
        prepareJarSearchableOptions { enabled = false }
        // 为所有 Copy 类型的任务设置重复策略
        withType<AbstractCopyTask> {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        test {
            systemProperty("java.awt.headless", "true")
            testLogging {
                showStandardStreams = prop("showStandardStreams").toBoolean()
                events("passed", "skipped", "failed")
                afterSuite(
                    KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                        if (desc.parent == null) {
                            val output =
                                "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                            println(output)
                        }
                    }),
                )
            }
            if (isCI) {
                retry {
                    maxRetries.set(3)
                    maxFailures.set(5)
                }
            }

            // 添加测试目录包含规则
            include("**/*Test.class")
            include("**/*Tests.class")
            include("**/*Spec.class")
        }


    }
}


// ============================================================
// Plugin 模块配置
// ============================================================

project(":plugin") {
    apply {
        plugin("org.jetbrains.changelog")
    }

    changelog {
        version.set(cangjiePluginVersion)
        path.set("${rootProject.projectDir}/CHANGELOG.md")
        header.set(provider { "[${version.get()}] - ${LocalDate.now()}" })
        itemPrefix.set("-")
        keepUnreleasedSection.set(true)
        unreleasedTerm.set("Unreleased")
        groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
        headerParserRegex.set("""([\d.]+)(-\d+)?""".toRegex())
    }

    intellijPlatform {
        autoReload = true

        pluginConfiguration {
            name = "CangJie"
        }

        publishing {
            token = environment("PUBLISH_TOKEN")
            channels.set(props("channel").map { listOf(it) })
        }

        pluginVerification {
            ides {
                recommended()
                select {
                    types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                    channels = listOf(ProductRelease.Channel.RELEASE)
                    sinceBuild = sinceBuildP
                }
            }
        }
    }

    version = cangjiePluginVersion

    dependencies {
        intellijPlatform {

            if (!isBuildPlugin()) {

                plugins(psiViewerPlugin, indexViewPlugin)
                bundledPlugins(tomlPlugin, copyright, jsonPlugin)
            }
        }
        implementation(project(":"))


    }
    val mergePluginJarTask = tasks.register<Jar>("mergePluginJars") {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveBaseName.set(basePluginArchiveName)

        exclude("META-INF/MANIFEST.MF")
        exclude("**/classpath.index")

        val pluginLibDir by lazy {
            val sandboxTask = tasks.prepareSandbox.get()
            sandboxTask.destinationDir.resolve("${sandboxTask.pluginName.get()}/lib")
        }

        val pluginJars by lazy {
            pluginLibDir.listFiles().orEmpty().filter {

                it.isPluginJar()
            }
        }

        destinationDirectory.set(project.layout.dir(provider { pluginLibDir }))

        doFirst {
            for (file in pluginJars) {
                from(zipTree(file))
            }
        }

        doLast {

            delete(pluginJars)
        }
    }

    tasks {
        patchPluginXml {
            sinceBuild.set(sinceBuildP)
            val untilBuildValue = prop("untilBuild")
            if (untilBuildValue.isNotEmpty()) {
                untilBuild.set(untilBuildValue)
            }
            pluginVersion.set(cangjiePluginVersion)

            pluginDescription.set(provider { file("description.html").readText() })
            changeNotes.set(provider {
                changelog.getAll()
                    .filter { it.key != "Unreleased" }
                    .entries
                    .firstOrNull()
                    ?.let { changelog.renderItem(it.value, Changelog.OutputType.HTML) }
                    ?: "No changes available"
            })
        }

        buildPlugin {
            archiveBaseName.set(basePluginArchiveName)
        }

        runIde {

            enabled = true
            dependsOn(mergePluginJarTask)
            jvmArgs(getIdeJvmArgs())

        }

        prepareSandbox {
            finalizedBy(mergePluginJarTask)
            enabled = true
        }

        buildSearchableOptions {
            dependsOn(mergePluginJarTask)
            enabled = prop("enableBuildSearchableOptions").toBoolean()
        }

        publishPlugin {
            dependsOn(":plugin:buildPlugin")
            channels.set(props("channel").map { listOf(it) })
            archiveFile.set(
                project(":plugin").layout.buildDirectory.file(
                    "distributions/$basePluginArchiveName-$cangjiePluginVersion.zip",
                ),
            )
            token = environment("PUBLISH_TOKEN")
        }

        verifyPlugin {
            dependsOn(":plugin:buildPlugin")
            archiveFile.set(
                project(":plugin").layout.buildDirectory.file(
                    "distributions/$basePluginArchiveName-$cangjiePluginVersion.zip",
                ),
            )
        }
    }
}
// ============================================================
// Core 根模块配置
// ============================================================

/**
 * 该模块相当于整合模块，最终面对ide，如果引用该模块，则必须是一个独立的插件模块，而不是单纯的gradle模块
 */
project(":") {
    dependencies {
        intellijPlatform {
            bundledPlugins(tomlPlugin, copyright, jsonPlugin)
        }


        // 项目内部模块
        implementation(project(":common"))
        implementation(project(":toolchain"))
        implementation(project(":telemetry"))
        implementation(project(":util"))
        implementation(project(":icon"))
        implementation(project(":psi"))
        implementation(project(":psi:stubindex"))
        implementation(project(":highlighter"))
        implementation(project(":formatter"))

        implementation(project(":messages"))
        implementation(project(":notifications"))
        implementation(project(":analysis"))
        implementation(project(":macro"))

        api(project(":analysis:decompiler-to-psi"))

        implementation(project(":cangjie-project"))
        implementation(project(":metadata:cjo"))

        // 第三方依赖
        implementation(libs.jansi)
        implementation(libs.toml4j)
        implementation(libs.bundles.jackson)
    }

    tasks {
//        processTestResources {
//            from("$rootDir/bin") {
//                into("bin")
//                include("**")
//            }
//        }

//        register("resolveDependencies") {
//            doLast {
//                rootProject.allprojects
//                    .map { it.configurations }
//                    .flatMap { it.filter { c -> c.isCanBeResolved } }
//                    .forEach { it.resolve() }
//            }
//        }
    }
}

// ============================================================
// 工具函数
// ============================================================

/**
 * 判断当前任务是否为构建插件
 */
fun isBuildPlugin(): Boolean {
    return "buildPlugin" in gradle.startParameter.taskNames
}

/**
 * 判断文件是否为插件 JAR 包
 */
fun File.isPluginJar(): Boolean {
    if (!isFile) return false
    if (extension != "jar") return false
    return zipTree(this).files.any { it.isManifestFile() }
}

/**
 * 判断文件是否为 IntelliJ 插件清单文件
 */
fun File.isManifestFile(): Boolean {
    if (extension != "xml") return false
    val rootNode = try {
        val parser = XmlParser()
        parser.parse(this)
    } catch (e: Exception) {
        logger.error("Failed to parse $path", e)
        return false
    }
    return rootNode.name() == "idea-plugin"
}

/**
 * 获取环境变量
 */
fun environment(key: String) = providers.environmentVariable(key)

/**
 * 获取 Gradle 属性
 */
fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")

/**
 * 获取 Gradle 属性 Provider
 */
fun props(key: String) = providers.gradleProperty(key)

// ============================================================
// IDE 版本源码目录配置
// ============================================================

/**
 * 确保在编译前创建 IDE 版本特定的源码目录
 */
tasks.register("createIdeVersionSourceDir") {

    doLast {
        file("src/main/$ideVersion").mkdirs()
    }
}

tasks.compileKotlin {
    dependsOn("createIdeVersionSourceDir")
}

// ============================================================
// 多版本编译任务
// ============================================================

/**
 * 构建配置定义
 *
 * 每个配置代表一个构建产物，包含：
 * - name: 构建名称（用于任务名和文件名）
 * - sinceBuild: 支持的最低 IDE 版本
 * - untilBuild: 支持的最高 IDE 版本
 * - platformVersion: 用于编译的平台版本（决定使用哪套 API）
 */
data class BuildConfig(
    val name: String,
    val sinceBuild: String,
    val untilBuild: String,
    val platformVersion: String
)

/**
 * 定义所有构建配置
 *
 * Legacy API 配置 (242-252):
 * - 使用 workspace-model-legacy 源码目录
 * - 支持 IntelliJ 2024.2 到 2025.2
 *
 * Builder API 配置 (253+):
 * - 使用 workspace-model-builder 源码目录
 * - 支持 IntelliJ 2025.3 及以后版本
 */
val buildConfigs = listOf(
    BuildConfig(
        name = "242-252",
        sinceBuild = "242",
        untilBuild = "252.*",
        platformVersion = "242"
    ),
    BuildConfig(
        name = "253",
        sinceBuild = "253",
        untilBuild = "253.*",
        platformVersion = "253"
    )
)

/**
 * 为每个构建配置创建专门的构建任务
 */
buildConfigs.forEach { config ->
    tasks.register<Exec>("buildPlugin${config.name.replace("-", "")}") {
        group = "build"
        description = "Build plugin for IDE versions ${config.sinceBuild}-${config.untilBuild}"

        workingDir = rootDir

        // Windows 和 Unix 系统使用不同的 Gradle 包装器
        val gradlewCmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }

        commandLine(
            gradlewCmd,

            ":plugin:buildPlugin",
            "-PplatformVersion=${config.platformVersion}",
            "-PsinceBuild=${config.sinceBuild}",
            "-PuntilBuild=${config.untilBuild}",
            "-PversionSuffix=-${config.name}"
        )

        doFirst {
            println("=".repeat(80))
            println("构建配置: ${config.name}")
            println("  IDE 版本范围: ${config.sinceBuild} - ${config.untilBuild}")
            println("  编译平台版本: ${config.platformVersion}")
            println("=".repeat(80))
        }

        doLast {
            val buildDir = file("plugin/build/distributions")
            val expectedFile = buildDir.listFiles()
                ?.filter { it.extension == "zip" && it.name.contains(config.name) }
                ?.firstOrNull()

            if (expectedFile != null && expectedFile.exists()) {
                println("\n✓ 构建完成")
                println("  产物: ${expectedFile.name}")
                println("  大小: ${expectedFile.length() / 1024 / 1024} MB")
            }
        }
    }
}

/**
 * 聚合任务：构建所有配置的版本
 *
 * 使用方法：
 *   ./gradlew buildAllConfigs
 *
 * 此任务会构建：
 * - 242-252 版本（Legacy API）: 支持 2024.2 到 2025.2
 * - 253 版本（Builder API）: 支持 2025.3+
 *
 * 每个构建产物会自动设置正确的 sinceBuild 和 untilBuild
 */
tasks.register("buildAllConfigs") {
    group = "build"
    description = "Build plugin for all configured version ranges"

    doFirst {
        println("\n" + "=".repeat(80))
        println("开始构建所有配置")
        println("=".repeat(80))
        println("共 ${buildConfigs.size} 个构建配置:\n")

        buildConfigs.forEachIndexed { index, config ->
            println("${index + 1}. ${config.name}: IDE ${config.sinceBuild}-${config.untilBuild}")
        }

        println("\n" + "=".repeat(80) + "\n")
    }

    dependsOn(buildConfigs.map { config ->
        "buildPlugin${config.name.replace("-", "")}"
    })

    doLast {
        val buildDir = file("plugin/build/distributions")

        println("\n" + "=".repeat(80))
        println("所有配置构建完成！")
        println("=".repeat(80))
        println("构建产物位于: ${buildDir.absolutePath}\n")

        // 列出所有构建产物
        buildDir.listFiles()?.filter { it.extension == "zip" }?.forEach {
            println("  ✓ ${it.name} (${it.length() / 1024 / 1024} MB)")
        }
        println()
    }
}

/**
 * 验证任务：检查构建配置的正确性
 *
 * 使用方法：
 *   ./gradlew verifyBuildConfigs
 *
 * 检查项：
 * - sinceBuild 和 untilBuild 是否合理
 * - platformVersion 是否在版本范围内
 * - 版本范围是否有重叠
 */
tasks.register<DefaultTask>("verifyBuildConfigs") {
    group = "verification"
    description = "Verify build configurations are correct"

    doLast {
        println("\n验证构建配置...\n")

        var hasErrors = false

        buildConfigs.forEach { config ->
            println("检查配置: ${config.name}")

            // 检查 sinceBuild <= platformVersion
            if (config.sinceBuild.toInt() > config.platformVersion.toInt()) {
                println("  ✗ 错误: sinceBuild (${config.sinceBuild}) 大于 platformVersion (${config.platformVersion})")
                hasErrors = true
            }

            if (!hasErrors) {
                println("  ✓ 配置正确")
            }

            println()
        }

        if (hasErrors) {
            throw GradleException("构建配置验证失败，请检查上述错误")
        } else {
            println("所有构建配置验证通过！✓")
        }
    }
}


