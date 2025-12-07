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
val sinceBuild = prop("sinceBuild")
val untilBuild = prop("untilBuild")
val versionSuffix = prop("versionSuffix")
val cangjiePluginVersion = "$pluginVersion$versionSuffix"

// ============================================================
// IntelliJ 平台插件依赖
// ============================================================
val psiViewerPlugin = prop("psiViewerPlugin")
val indexViewPlugin = prop("indexViewPlugin")
val chinesePlugin = "com.intellij.zh:233.407"

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
    id("net.saliman.properties") version "1.5.2"
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog") version "2.2.1"
    id("java-test-fixtures")
    kotlin("plugin.serialization") version "2.2.0"
    id("org.gradle.test-retry") version "1.5.3"


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
        testImplementation("junit:junit:4.13.2")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.0")
        testImplementation(testFixtures(project(":test-common")))

        // Kotlin 依赖
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
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
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.add("-Xjvm-default=all")
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }


        runIde { enabled = false }
        prepareSandbox { enabled = false }
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
                    sinceBuild = "242"
                }
            }
        }
    }

    version = cangjiePluginVersion

    dependencies {
        intellijPlatform {
            if (!isBuildPlugin()) {
                plugins(psiViewerPlugin, indexViewPlugin, chinesePlugin)
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
            sinceBuild.set(prop("sinceBuild"))
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
 * 该模块相当于 core，包含核心功能
 * 注意：不应被其他模块引用
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
        implementation(project(":messages"))
        implementation(project(":notifications"))
        implementation(project(":analysis"))
        implementation(project(":cangjie-project"))

        // 第三方依赖
        implementation("org.fusesource.jansi:jansi:2.4.1")
        implementation("io.hotmoka:toml4j:0.7.3")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
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

