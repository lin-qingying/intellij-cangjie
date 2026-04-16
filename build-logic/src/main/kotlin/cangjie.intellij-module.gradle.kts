import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cangjie.kotlin-module")
    id("org.jetbrains.intellij.platform.module")
}

val ideToRunType = providers.gradleProperty("ideToRunType")
    .orElse(providers.gradleProperty("baseIDE"))
val ideRunVersion = providers.gradleProperty("ideRunVersion")

dependencies {
    intellijPlatform {
        // 所有 IntelliJ 模块统一绑定到同一平台基线，避免子模块各自漂移。
        create(IntelliJPlatformType.fromCode(ideToRunType.get()), ideRunVersion.get())
        bundledPlugins(
            "org.toml.lang",
            "com.intellij.copyright",
            "com.intellij.modules.json",
        )
    }
}
