import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.io.File

plugins {
    id("cangjie.kotlin-module")
    id("org.jetbrains.intellij.platform")
}

val ideToRunType = providers.gradleProperty("ideToRunType")
    .orElse(providers.gradleProperty("baseIDE"))
val ideRunVersion = providers.gradleProperty("ideRunVersion")
val sinceBuildProp = providers.gradleProperty("sinceBuild")
val untilBuildProp = providers.gradleProperty("untilBuild")

intellijPlatform {
    // 253 平台在 sandbox 启动早期为 dynamic plugin 目录建立 VFS 监听时会触发 LoadingState 违规；
    // host 插件开发期先关闭自动热重载，避免 runIde 启动阶段落入平台的过早 Registry/VFS 刷新路径。
    autoReload = false

    pluginConfiguration {
        name = "CangJie"
    }
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.fromCode(ideToRunType.get()), ideRunVersion.get())
        testFramework(TestFrameworkType.Platform)
        plugins("com.redhat.devtools.lsp4ij:0.19.2")
        bundledPlugins(
            "org.toml.lang",
            "com.intellij.copyright",
            "com.intellij.modules.json",
        )
    }
}

tasks {
    patchPluginXml {
        this.sinceBuild.set(sinceBuildProp)
        this.untilBuild.set(untilBuildProp)
        pluginVersion.set(provider { project.version.toString() })
        pluginDescription.set(provider {
            project.layout.projectDirectory.file("description.html").asFile.readText()
        })
    }

    buildPlugin {
        archiveBaseName.set("intellij-cangjie")
    }

    runIde {
        jvmArgs(getIdeJvmArgs())
    }
}

fun getIdeJvmArgs(): List<String> {
    val dumpDir = File(rootDir, "dumpTmp").also { it.mkdirs() }
    return listOf(
        "-Xms512m",
        "-Xmx6144m",
        "-XX:+UseG1GC",
        "-XX:+ExitOnOutOfMemoryError",
        "-XX:MaxMetaspaceSize=512m",
        "-Didea.is.internal=true",
        "-Didea.debug.mode=true",
        "-Dfile.encoding=UTF-8",
        "-XX:HeapDumpPath=${dumpDir.absolutePath}",
    )
}
