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
    autoReload = true

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
        "-Xmx4096m",
        "-XX:+UseG1GC",
        "-XX:MaxMetaspaceSize=512m",
        "-Didea.is.internal=true",
        "-Didea.debug.mode=true",
        "-Dfile.encoding=UTF-8",
        "-XX:HeapDumpPath=${dumpDir.absolutePath}",
    )
}
