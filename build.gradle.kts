/*
 * Copyright 2024 LinQingYing. and contributors.
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
import org.gradle.kotlin.dsl.KotlinClosure2
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.JarSearchableOptionsTask
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.kotlin.dsl.testImplementation

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
val cangjiePluginVersion = "$pluginVersion-$ideVersion"


//###############################################################
val psiViewerPlugin = prop("psiViewerPlugin")
val indexViewPlugin = prop("indexViewPlugin")
val tomlPlugin = "org.toml.lang"

val terminalPlugin = "org.jetbrains.plugins.terminal"

val chinesePlugin = "com.intellij.zh:233.407"
val diagramPlugin = "com.intellij.diagram"

//###############################################################


plugins {
    idea
    id("net.saliman.properties") version "1.5.2"
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.2.1"

    kotlin("plugin.serialization") version "2.1.0"
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

        plugin("org.gradle.test-retry")
    }

    repositories {

        intellijPlatform {

            intellijDependencies()
            defaultRepositories()
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
            intellijPlatform {

                create(IntelliJPlatformType.fromCode(ideToRunType), ideRunVersion)

            }
        }

        testImplementation("junit:junit:4.13.2")
// https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test-junit
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")

    }

    intellijPlatform {
        pluginVerification {

            ides {
                recommended()
                select {
                    types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
                    channels = listOf(ProductRelease.Channel.RELEASE)
                    sinceBuild = "241"
                    untilBuild = "251.*"
                }
            }
        }
    }
    sourceSets {
        main {
            java {
                srcDirs("src/main/kotlin")
                srcDirs("src/main/$platformVersion") // 添加 IDE 版本特定的源码目录
            }
            kotlin {
                srcDirs("src/main/kotlin")
                srcDirs("src/main/$platformVersion") // 添加 IDE 版本特定的源码目录
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
        withType<JarSearchableOptionsTask> {
        }
        withType<KotlinCompile> {

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.set(listOf("-Xjvm-default=all", "-Xcontext-receivers"))
            }
        }

        withType<PatchPluginXmlTask> {
            sinceBuild.set(ideVersion)
            untilBuild.set("$ideVersion.*")

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
                events("passed", "skipped", "failed")  // 添加这行
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

        processTestResources {

            // 保持现有的配置
            from("$rootDir/bin") {
                into("bin")
                include("**")
            }
        }
    }
}


project(":plugin") {

    intellijPlatform {
        autoReload = true

        pluginConfiguration {
            name = "CangJie"
        }
        publishing {
            token.set(token)
            channels.set(listOf("dev"))
        }
    }

    version = cangjiePluginVersion
    dependencies {
        intellijPlatform {
            if (!isBuildPlugin()) {
                plugins(

                    psiViewerPlugin,
                    indexViewPlugin,
                    chinesePlugin/*, nativeDebugPlugin*/
                )
                bundledPlugins(tomlPlugin, )
            }
        }
        implementation(project(":"))


        implementation(project(":dap-debugger"))
    }

    val mergePluginJarTask = task<Jar>("mergePluginJars") {
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
    val createSourceJar = task<Jar>("createSourceJar") {

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

        buildPlugin {
            dependsOn(createSourceJar)
            from(createSourceJar) { into("lib/src") }
            // Set proper name for final plugin zip.
            // Otherwise, base name is the same as gradle module name
            archiveBaseName.set(basePluginArchiveName)
        }
        runIde { enabled = true }
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

        withType<RunIdeTask> {
            dependsOn(mergePluginJarTask)
            jvmArgs("-Xmx768m", "-XX:+UseG1GC", "-XX:SoftRefLRUPolicyMSPerMB=50")
            jvmArgs("-Didea.auto.reload.plugins=false")

            jvmArgs("-Dide.show.tips.on.startup.default.value=false")
        }

        withType<PatchPluginXmlTask> {
            pluginDescription.set(provider { file("description.html").readText() })
        }
    }
}

project(":") {

    dependencies {
        intellijPlatform {
            bundledPlugins(tomlPlugin)
        }
        implementation("org.fusesource.jansi:jansi:2.4.1")

        implementation("io.hotmoka:toml4j:0.7.3")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")



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
    task("resolveDependencies") {
        doLast {
            rootProject.allprojects
                .map { it.configurations }
                .flatMap { it.filter { c -> c.isCanBeResolved } }
                .forEach { it.resolve() }
        }
    }
}
project(":dap-debugger") {
    dependencies {
        implementation(project(":"))
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

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")

// 确保在编译前创建 IDE 版本特定的源码目录
tasks.register("createIdeVersionSourceDir") {
    doLast {
        file("src/main/$ideVersion").mkdirs()
    }
}

tasks.compileKotlin {
    dependsOn("createIdeVersionSourceDir")
}


//project(":lsp"){
//    dependencies{
//        // https://mvnrepository.com/artifact/com.github.nipunaranasinghe/lsp4intellij
////    implementation("com.github.ballerina-platform:lsp4intellij:0.96.2")
//// https://mvnrepository.com/artifact/org.eclipse.lsp4j/org.eclipse.lsp4j
//        implementation  ("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")
//        implementation(project(":"))
//
//    }
//}