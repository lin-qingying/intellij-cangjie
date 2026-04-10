/*
 * Copyright 2026 LinQingYing. and contributors.
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

import org.gradle.api.GradleException
import org.gradle.authentication.http.BasicAuthentication
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.time.LocalDate

plugins {
    idea
    alias(libs.plugins.saliman.properties)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.intellij.changelog)
    alias(libs.plugins.gradle.test.retry)
    id("org.jetbrains.intellij.platform")
    id("java-test-fixtures")
}

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS

val basePluginArchiveName = "intellij-cangjie"
val grammarKitFakePsiDeps = "grammar-kit-fake-psi-deps"
val platformVersion = prop("platformVersion").toInt()
val baseIDE = prop("baseIDE")
val ideToRunType = prop("ideToRunType").ifEmpty { baseIDE }
val ideRunVersion = prop("ideRunVersion")
val ideVersion = prop("ideVersion")
val pluginVersion = prop("pluginVersion")
val sinceBuildP = prop("sinceBuild")
val untilBuildP = prop("untilBuild")
val versionSuffix = prop("versionSuffix")
val cangjiePluginVersion = "$pluginVersion$versionSuffix"

val psiViewerPlugin = prop("psiViewerPlugin")
val indexViewPlugin = prop("indexViewPlugin")

val tomlPlugin = "org.toml.lang"
val jsonPlugin = "com.intellij.modules.json"
val copyright = "com.intellij.copyright"
val cangjieGitHubPackagesUrl = "https://maven.pkg.github.com/lin-qingying/cangjie"
val githubPackagesUsername = credential("GITHUB_PACKAGES_USERNAME")
val githubPackagesToken = credential("GITHUB_PACKAGES_TOKEN")
val isCI = System.getenv("CI") != null

idea {
    module {
        excludeDirs = excludeDirs + file("testData") + file("deps") + file("bin") +
            file("$grammarKitFakePsiDeps/src/main/kotlin")
    }
}

fun getIdeJvmArgs(): List<String> {
    val dumpDir = File(rootDir, "dumpTmp").also { it.mkdirs() }
    return listOf(
        "-Xms512m",
        "-Xmx4096m",
        "-XX:ReservedCodeCacheSize=512m",
        "-XX:MaxMetaspaceSize=512m",
        "-XX:+UseG1GC",
        "-XX:G1HeapRegionSize=16m",
        "-XX:G1ReservePercent=20",
        "-XX:InitiatingHeapOccupancyPercent=35",
        "-XX:SoftRefLRUPolicyMSPerMB=50",
        "-XX:-OmitStackTraceInFastThrow",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+DebugNonSafepoints",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=${dumpDir.absolutePath}",
        "-Djna.nosys=false",
        "-Djna.nounpack=false",
        "-Djna.debug_load=false",
        "-Djna.debug_load.jna=false",
        "-Dkotlinx.coroutines.scheduler.core.pool.size=4",
        "-Dkotlin.reflect.full.enabled=true",
        "-Dkotlin.compiler.incremental=false",
        "-Didea.is.internal=true",
        "-Didea.debug.mode=true",
        "-Didea.ProcessCanceledException=disabled",
        "-Didea.fatal.error.notification=disabled",
        "-Didea.log.startup.performance=true",
        "-Didea.auto.reload.plugins=false",
        "-Dide.show.tips.on.startup.default.value=false",
        "-Dfile.encoding=UTF-8",
        "-Djdk.attach.allowAttachSelf=true",
        "-Dsun.io.useCanonCaches=false",
    )
}

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
        maven { setUrl("https://jitpack.io") }
        maven {
            name = "CangJieGitHubPackages"
            setUrl(cangjieGitHubPackagesUrl)
            credentials {
                username = githubPackagesUsername
                password = githubPackagesToken
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    dependencies {
        intellijPlatform {
            testFramework(TestFrameworkType.Platform)
            create(IntelliJPlatformType.fromCode(ideToRunType), ideRunVersion)
        }

        testImplementation("junit:junit:4.13.2")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
        testImplementation(kotlin("test-junit"))
        testImplementation(testFixtures(project(":test-common")))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
        implementation(kotlin("test"))
        implementation(kotlin("test-junit"))
        implementation(kotlin("stdlib"))
    }

    sourceSets {
        main {
            java.srcDirs("src/main/kotlin", "src/main/$platformVersion")
            kotlin.srcDirs("src/main/kotlin", "src/main/$platformVersion", "src/gen")
            resources.srcDirs("src/main/resources")
        }
        test {
            java.srcDirs("src/test/kotlin", "src/test/java")
            kotlin.srcDirs("src/test/kotlin")
            resources.srcDirs("src/test/resources")
        }
    }

    tasks {
        withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.add("-Xjvm-default=all")
                freeCompilerArgs.add("-Xcontext-parameters")
                freeCompilerArgs.add("-Xskip-prerelease-check")
            }
        }

        runIde { enabled = false }
        prepareSandbox { enabled = false }
        jarSearchableOptions { enabled = false }
        buildSearchableOptions { enabled = false }
        prepareJarSearchableOptions { enabled = false }

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
                            println(
                                "Results: ${result.resultType} " +
                                    "(${result.testCount} tests, ${result.successfulTestCount} passed, " +
                                    "${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                            )
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
            include("**/*Test.class", "**/*Tests.class", "**/*Spec.class")
        }
    }
}

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

    tasks {
        patchPluginXml {
            sinceBuild.set(sinceBuildP)
            if (untilBuildP.isNotEmpty()) {
                untilBuild.set(untilBuildP)
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
            jvmArgs(getIdeJvmArgs())
        }

        prepareSandbox {
            enabled = true
        }

        buildSearchableOptions {
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

project(":") {
    dependencies {
        intellijPlatform {
            bundledPlugins(tomlPlugin, copyright, jsonPlugin)
        }

        implementation(project(":toolchain"))
        implementation(project(":telemetry"))
        implementation(project(":icon"))
        implementation(project(":highlighter"))
        implementation(project(":formatter"))
        implementation(project(":messages"))
        implementation(project(":notifications"))
        implementation(project(":cangjie-project"))
        implementation(project(":common"))
        implementation(project(":util"))

        implementation(libs.cangjieCommonForIde)
        implementation(libs.cangjiePsiForIde)
        implementation(libs.cangjieCfirForIde)
        implementation(libs.cangjieAnalysisApiForIde)
        implementation(libs.cangjieAnalysisApiCfirForIde)
        implementation(libs.cangjieAnalysisApiStandaloneForIde)

        implementation(libs.jansi)
        implementation(libs.toml4j)
        implementation(libs.bundles.jackson)
    }
}

fun isBuildPlugin(): Boolean = "buildPlugin" in gradle.startParameter.taskNames

fun environment(key: String) = providers.environmentVariable(key)

fun credential(name: String): String {
    val fromGradleProperty = providers.gradleProperty(name).orNull?.trim()
    if (!fromGradleProperty.isNullOrEmpty()) return fromGradleProperty

    val fromEnvironment = providers.environmentVariable(name).orNull?.trim()
    if (!fromEnvironment.isNullOrEmpty()) return fromEnvironment

    throw GradleException(
        "Missing credential `$name`. " +
            "Please set it in ~/.gradle/gradle.properties or environment variables."
    )
}

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")

fun props(key: String) = providers.gradleProperty(key)

tasks.register("createIdeVersionSourceDir") {
    doLast {
        file("src/main/$ideVersion").mkdirs()
    }
}

tasks.compileKotlin {
    dependsOn("createIdeVersionSourceDir")
}

data class BuildConfig(
    val name: String,
    val sinceBuild: String,
    val untilBuild: String,
    val platformVersion: String,
)

val buildConfigs = listOf(
    BuildConfig("242-252", "242", "252.*", "242"),
    BuildConfig("253", "253", "253.*", "253"),
)

buildConfigs.forEach { config ->
    tasks.register<Exec>("buildPlugin${config.name.replace("-", "")}") {
        group = "build"
        description = "Build plugin for IDE versions ${config.sinceBuild}-${config.untilBuild}"

        workingDir = rootDir
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
            "-PversionSuffix=-${config.name}",
        )
    }
}

tasks.register("buildAllConfigs") {
    group = "build"
    description = "Build plugin for all configured version ranges"
    dependsOn(buildConfigs.map { "buildPlugin${it.name.replace("-", "")}" })
}

tasks.register<DefaultTask>("verifyBuildConfigs") {
    group = "verification"
    description = "Verify build configurations are correct"

    doLast {
        var hasErrors = false
        buildConfigs.forEach { config ->
            if (config.sinceBuild.toInt() > config.platformVersion.toInt()) {
                println("Error: sinceBuild (${config.sinceBuild}) > platformVersion (${config.platformVersion})")
                hasErrors = true
            }
        }
        if (hasErrors) {
            throw GradleException("Build configuration verification failed.")
        }
    }
}
