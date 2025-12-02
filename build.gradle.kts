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


val basePluginArchiveName = "intellij-cangjie"

val grammarKitFakePsiDeps = "grammar-kit-fake-psi-deps"

val pluginProjects: List<Project>
    get() = rootProject.allprojects.filter { it.name != grammarKitFakePsiDeps }

val platformVersion = prop("platformVersion").toInt()
val baseIDE = prop("baseIDE")
val ideToRunType = prop("ideToRunType").ifEmpty { baseIDE }


val ideRunVersion = prop("ideRunVersion")
val ideVersion = prop("ideVersion")
//插件版本
val pluginVersion = prop("pluginVersion")
val sinceBuild = prop("sinceBuild")
val untilBuild = prop("untilBuild")
val versionSuffix = prop("versionSuffix")
//val cangjiePluginVersion = "$pluginVersion-$ideVersion"
val cangjiePluginVersion = "$pluginVersion$versionSuffix"


//###############################################################
val psiViewerPlugin = prop("psiViewerPlugin")
val indexViewPlugin = prop("indexViewPlugin")
val tomlPlugin = "org.toml.lang"
val jsonPlugin = "com.intellij.modules.json"
val copyright = "com.intellij.copyright"

val terminalPlugin = "org.jetbrains.plugins.terminal"

val chinesePlugin = "com.intellij.zh:233.407"
val diagramPlugin = "com.intellij.diagram"

//###############################################################
//val sinceBuild = prop("sinceBuild")
//val untilBuild = prop("untilBuild")


/**
 * 将版本号转为合法的文件夹名
 * .改为-  如果最后面是.*去掉
 */
fun String.toValidDirectoryName(): String {
    return this.replace(".", "-").removeSuffix(".*")
}



plugins {
    idea
    id("net.saliman.properties") version "1.5.2"
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.10.2"
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
allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.intellij.platform")
//        plugin("java-test-fixtures")
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

        testImplementation("junit:junit:4.13.2")
// https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
        testImplementation(testFixtures(project(":test-common")))

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test-junit
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.0")
        implementation(kotlin("test"))
        implementation(kotlin("test-junit"))
        implementation(kotlin("stdlib"))


    }

    intellijPlatform {



    }
    sourceSets {
        main {
            java {
                srcDirs("src/main/kotlin")
                srcDirs("src/main/$platformVersion") // 添加 IDE 版本特定的源码目录
//                srcDirs("src/main/${sinceBuild.toValidDirectoryName()}-${untilBuild.toValidDirectoryName()}")
            }
            kotlin {
                srcDirs("src/main/kotlin")
                srcDirs("src/main/$platformVersion") // 添加 IDE 版本特定的源码目录
//                srcDirs("src/main/${sinceBuild.toValidDirectoryName()}-${untilBuild.toValidDirectoryName()}")

                srcDirs("src/gen")
            }
            resources {
                srcDirs("src/main/resources")
            }
        }

        test {
            java {
                srcDirs("src/test/kotlin")
                srcDirs("src/test/java")  // 添加 Java 测试源目录
            }
            kotlin {
                srcDirs("src/test/kotlin")  // 明确指定 Kotlin 测试源目录
            }
            resources {
                srcDirs("src/test/resources")
            }
        }
    }

    tasks {
        verifyPlugin {

            archiveFile.set(
                project(":plugin").layout.buildDirectory.file(
                    "distributions/$basePluginArchiveName-$cangjiePluginVersion.zip",
                ),
            )
        }

        withType<KotlinCompile> {

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.add("-Xjvm-default=all")

                freeCompilerArgs.add("-Xcontext-parameters")


            }
        }


//        插件上传推送配置

        publishPlugin {
            //           先构建
            dependsOn(":plugin:buildPlugin")

            channels.set(props("channel").map { listOf(it) })

            archiveFile.set(
                project(":plugin").layout.buildDirectory.file(
                    "distributions/$basePluginArchiveName-$cangjiePluginVersion.zip",
                ),
            )

            token = environment("PUBLISH_TOKEN")
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


project(":plugin") {

    apply {
        plugin("org.jetbrains.changelog")
    }

    // Configure the changelog plugin
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


            verificationReportsDirectory.set(layout.buildDirectory.dir("verification-reports"))



            ides {
                recommended()
                select {
                    types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                    channels = listOf(ProductRelease.Channel.RELEASE)
                    sinceBuild = "242"
//                    untilBuild = "253.*"
                }
            }
        }
    }

    version = cangjiePluginVersion
    dependencies {
        intellijPlatform {
            if (!isBuildPlugin()) {
                plugins(

                    psiViewerPlugin,
                    indexViewPlugin,
                    chinesePlugin
                )

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
    val createSourceJar = tasks.register<Jar>("createSourceJar") {

        for (prj in pluginProjects) {
            from(prj.kotlin.sourceSets.main.get().kotlin) {
                include("**/*.java")
                include("**/*.kt")
            }
        }
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        archiveBaseName.set(basePluginArchiveName)
        archiveClassifier.set("src")
    }

    tasks {
        patchPluginXml {


//            sinceBuild.set(ideVersion)
//            untilBuild.set("$ideVersion.*")
//            pluginVersion.set(cangjiePluginVersion)
//
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
//            dependsOn(createSourceJar)

//            from(createSourceJar) { into("lib/src") }
            // Set proper name for final plugin zip.
            // Otherwise, base name is the same as gradle module name
            archiveBaseName.set(basePluginArchiveName)
        }
        runIde {
            enabled = true
            dependsOn(mergePluginJarTask)

            jvmArgs(
                // 内存配置（确保-Xmx参数不被后续覆盖）
                "-Xms512m",  // 初始堆内存
                "-Xmx2048m", // 最大堆内存（根据物理内存调整）

                // G1垃圾收集器优化（合并重复参数）
                "-XX:+UseG1GC",
                "-XX:G1HeapRegionSize=16m",
                "-XX:G1ReservePercent=20",
                "-XX:InitiatingHeapOccupancyPercent=35",
                "-XX:SoftRefLRUPolicyMSPerMB=50", // Soft引用缓存策略

                // IDE性能参数（合并重复项）
                "-Didea.auto.reload.plugins=false", // 禁用插件自动重载
                "-Dide.show.tips.on.startup.default.value=false", // 禁用启动提示

                // 内存溢出处理
//                "-XX:+HeapDumpOnOutOfMemoryError", // 启用堆转储
//                "-XX:HeapDumpPath=${buildDir}/heapDumps.hprof" // 转储文件路径
            )
//            jvmArgs("-Xmx768m", "-XX:+UseG1GC", "-XX:SoftRefLRUPolicyMSPerMB=50")
        }
        prepareSandbox {
            finalizedBy(mergePluginJarTask)
            enabled = true
        }
        buildSearchableOptions {
            // Force `mergePluginJarTask` be executed before `buildSearchableOptions`
            // Otherwise, `buildSearchableOptions` task can't load the plugin and searchable options are not built.
            // Should be dropped when jar merging is implemented in `gradle-intellij-plugin` itself
            dependsOn(mergePluginJarTask)
            enabled = prop("enableBuildSearchableOptions").toBoolean()
        }

//        withType<RunIdeTask> {
//            dependsOn(mergePluginJarTask)
//            jvmArgs("-Xmx768m", "-XX:+UseG1GC", "-XX:SoftRefLRUPolicyMSPerMB=50")
//            jvmArgs("-Didea.auto.reload.plugins=false")
//
//            jvmArgs("-Dide.show.tips.on.startup.default.value=false")
//        }


    }
}
/**
 * 该模块相当于core   不可被其他模块引用
 */
project(":") {

    dependencies {

        intellijPlatform{
            bundledPlugins(tomlPlugin, copyright, jsonPlugin)
        }
        implementation(project(":common"))
        implementation("org.fusesource.jansi:jansi:2.4.1")

        implementation("io.hotmoka:toml4j:0.7.3")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
        implementation(project(":toolchain"))
        implementation(project(":telemetry"))

        implementation(project(":util"))
        implementation(project(":icon"))
        implementation(project(":psi"))
        implementation(project(":messages"))

        implementation(project(":notifications"))



        implementation(project(":analysis"))

        implementation(project(":cangjie-project"))


    }
    tasks {
        processTestResources {
//            dependsOn(named(compileNativeCodeTaskName))
            from("$rootDir/bin") {
                into("bin")
                include("**")
            }
        }
    }
    tasks.register("resolveDependencies") {
        doLast {
            rootProject.allprojects
                .map { it.configurations }
                .flatMap { it.filter { c -> c.isCanBeResolved } }
                .forEach { it.resolve() }
        }
    }
}

fun isBuildPlugin(): Boolean {
    return "buildPlugin" in gradle.startParameter.taskNames
}

// 判断文件是否为插件jar包
fun File.isPluginJar(): Boolean {
    // 如果文件不是文件，则返回false
    if (!isFile) return false
    // 如果文件扩展名不是jar，则返回false
    if (extension != "jar") return false
    // 如果zipTree中的文件中存在isManifestFile()为true的文件，则返回true
    return zipTree(this).files.any { it.isManifestFile() }
}

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

fun environment(key: String) = providers.environmentVariable(key)
fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")

fun props(key: String) = providers.gradleProperty(key)
// 确保在编译前创建 IDE 版本特定的源码目录
tasks.register("createIdeVersionSourceDir") {
    val dir = layout.projectDirectory.dir("src/main/$ideVersion")
    doLast {
        file("src/main/$ideVersion").mkdirs()
    }
}

tasks.compileKotlin {
    dependsOn("createIdeVersionSourceDir")
}

