import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

plugins {
    idea
    alias(libs.plugins.saliman.properties)
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.intellij.changelog)
    id("org.gradle.test-retry")            // 不带版本，从 buildSrc classpath 解析
    id("org.jetbrains.intellij.platform")
    id("java-test-fixtures")
}
gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS

// ================= 基础配置 =================

val basePluginArchiveName = "intellij-cangjie"

val platformVersion = prop("platformVersion").toInt()
val ideToRunType = prop("ideToRunType").ifEmpty { prop("baseIDE") }
val ideRunVersion = prop("ideRunVersion")
val ideVersion = prop("ideVersion")

val pluginVersion = prop("pluginVersion")
val sinceBuildP = prop("sinceBuild")
val untilBuildP = prop("untilBuild")
val versionSuffix = prop("versionSuffix")

val cangjiePluginVersion = "$pluginVersion$versionSuffix"

val tomlPlugin = "org.toml.lang"
val jsonPlugin = "com.intellij.modules.json"
val copyright = "com.intellij.copyright"

val isCI = System.getenv("CI") != null

// ================= 根项目插件配置 =================


apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
version = cangjiePluginVersion

intellijPlatform {
    autoReload = true

    pluginConfiguration {
        name = "CangJie"
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

repositories {
    maven {
        name = "CangJieGitHubPackages"
        url = uri("https://maven.pkg.github.com/lin-qingying/cangjie")
        credentials {
            username = findProperty("GITHUB_PACKAGES_USERNAME") as String?
                ?: System.getenv("GITHUB_PACKAGES_USERNAME")
            password = findProperty("GITHUB_PACKAGES_TOKEN") as String?
                ?: System.getenv("GITHUB_PACKAGES_TOKEN")
        }
    }
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.fromCode(ideToRunType), ideRunVersion)
        testFramework(TestFrameworkType.Platform)

        pluginComposedModule(project(":lsp4ij"))
        pluginComposedModule(project(":cjpm"))
        pluginComposedModule(project(":debugger"))
        pluginComposedModule(project(":core"))  // 也改成 composed module

        plugins("com.redhat.devtools.lsp4ij:0.19.2")
        bundledPlugins(tomlPlugin, copyright, jsonPlugin)
    }


}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set(sinceBuildP)
        untilBuild.set(untilBuildP)
        pluginVersion.set(cangjiePluginVersion)
        pluginDescription.set(provider { file("description.html").readText() })
    }

    buildPlugin {
        archiveBaseName.set(basePluginArchiveName)
    }

    runIde {
        jvmArgs(getIdeJvmArgs())
    }

    test {
        systemProperty("java.awt.headless", "true")

        if (isCI) {
            retry {
                maxRetries.set(3)
                maxFailures.set(5)
            }
        }
    }
}


// ================= 工具函数 =================

fun getIdeJvmArgs(): List<String> {
    val dumpDir = File(rootDir, "dumpTmp").also { it.mkdirs() }
    return listOf(
        "-Xms512m",
        "-Xmx4096m",
        "-XX:+UseG1GC",
        "-XX:MaxMetaspaceSize=512m",
        "-Didea.is.internal=true",
        "-Didea.debug.mode=true",
        "-Dfile.encoding=UTF-8",
        "-XX:HeapDumpPath=${dumpDir.absolutePath}"
    )
}

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined")

// ================= 多版本构建 =================

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

        workingDir = rootDir

        val gradlewCmd =
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                "gradlew.bat"
            } else {
                "./gradlew"
            }

        commandLine(
            gradlewCmd,
            ":buildPlugin",  // 原来是 :plugin:buildPlugin，现在根项目就是 plugin
            "-PplatformVersion=${config.platformVersion}",
            "-PsinceBuild=${config.sinceBuild}",
            "-PuntilBuild=${config.untilBuild}",
            "-PversionSuffix=-${config.name}",
        )
    }
}