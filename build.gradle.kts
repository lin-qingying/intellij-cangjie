import org.gradle.api.GradleException

group = "org.cangnova.cangjie"
version = providers.gradleProperty("pluginVersion")
    .zip(providers.gradleProperty("versionSuffix").orElse("")) { pluginVersion, versionSuffix ->
        "$pluginVersion$versionSuffix"
    }
    .get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}

/**
 * 架构守卫：
 * 1) 阻止依赖回退到旧模块（core/common/...）
 * 2) 守住新分层依赖方向（foundation -> domain -> ide -> product）
 * 3) 阻止测试支撑模块进入产品发布内容
 *
 * 说明：当前仍处迁移期，少量跨层边会以白名单方式显式记录，
 * 后续迁移完成后再逐步清理白名单，而不是用兜底逻辑掩盖问题。
 */
val architectureGuard = tasks.register("architectureGuard") {
    group = "verification"
    description = "验证 2.x 新骨架的模块分层、依赖边界与产品装配约束。"

    doLast {
        val violations = mutableListOf<String>()

        val forbiddenLegacyProjectDeps = listOf(
            ":core",
            ":common",
            ":util",
            ":icon",
            ":messages",
            ":toolchain",
            ":notifications",
            ":cangjie-project",
            ":cjpm",
            ":telemetry",
            ":lsp4ij",
            ":debugger",
            ":debugger:common",
            ":debugger:dap",
            ":debugger:protobuf",
            ":formatter",
            ":highlighter",
            ":macro",
            ":namedpipe",
            ":test-common",
            ":",
        )

        val moduleBuildFiles = fileTree(rootDir) {
            include("modules/**/build.gradle.kts")
            include("product/**/build.gradle.kts")
        }.files

        fun modulePathFromBuildFile(file: File): String {
            val relative = file.relativeTo(rootDir).invariantSeparatorsPath
            val moduleDir = relative.removeSuffix("/build.gradle.kts")
            return ":" + moduleDir.replace("/", ":")
        }

        fun layerOf(modulePath: String): String = when {
            modulePath.startsWith(":modules:foundation") -> "foundation"
            modulePath.startsWith(":modules:domain:") -> "domain"
            modulePath.startsWith(":modules:ide:") -> "ide"
            modulePath.startsWith(":modules:test-support") -> "test"
            modulePath.startsWith(":product:") -> "product"
            else -> "other"
        }

        // 迁移期显式例外：后续完成目录物理迁移后应逐步消除
        val allowedLayerExceptions = setOf(
            ":modules:domain:project-model -> :modules:ide:ux",
            ":modules:domain:package-manager -> :modules:ide:base",
            ":modules:domain:package-manager -> :modules:ide:ux",
            ":modules:domain:package-manager -> :modules:ide:project",
            ":modules:domain:package-manager -> :modules:ide:run",
        )

        val projectDepRegex = Regex("""project\("(:[^"]+)"\)""")

        moduleBuildFiles.forEach { buildFile ->
            val sourceModule = modulePathFromBuildFile(buildFile)
            val sourceLayer = layerOf(sourceModule)
            val text = buildFile.readText(Charsets.UTF_8)
            val targets = projectDepRegex.findAll(text).map { it.groupValues[1] }.toList()

            targets.forEach { target ->
                if (target in forbiddenLegacyProjectDeps) {
                    violations += "$sourceModule 依赖了被禁止的旧模块 $target（${buildFile.relativeTo(rootDir)}）"
                }

                if (!target.startsWith(":modules:")) {
                    return@forEach
                }

                val targetLayer = layerOf(target)
                val edge = "$sourceModule -> $target"

                val allowed = when (sourceLayer) {
                    "foundation" -> targetLayer == "foundation"
                    "domain" -> targetLayer == "foundation" || targetLayer == "domain"
                    "ide" -> targetLayer == "foundation" || targetLayer == "domain" || targetLayer == "ide"
                    "test" -> target.startsWith(":modules:")
                    "product" -> target.startsWith(":modules:")
                    else -> true
                }

                if (!allowed && edge !in allowedLayerExceptions) {
                    violations += "分层违规依赖: $edge（${buildFile.relativeTo(rootDir)}）"
                }
            }
        }

        val settingsText = file("settings.gradle.kts").readText(Charsets.UTF_8)
        val forbiddenLegacyIncludes = listOf(
            "include(\"core\")",
            "include(\"common\")",
            "include(\"util\")",
            "include(\"icon\")",
            "include(\"messages\")",
            "include(\"toolchain\")",
            "include(\"notifications\")",
            "include(\"cangjie-project\")",
            "include(\"cjpm\")",
            "include(\"telemetry\")",
            "include(\"lsp4ij\")",
            "include(\"debugger\")",
            "include(\"formatter\")",
            "include(\"highlighter\")",
            "include(\"macro\")",
            "include(\"namedpipe\")",
            "include(\"test-common\")",
        )
        forbiddenLegacyIncludes.forEach { token ->
            if (settingsText.contains(token)) {
                violations += "settings.gradle.kts 包含旧模块入口: $token"
            }
        }

        val productBuildText = file("product/idea-plugin/build.gradle.kts").readText(Charsets.UTF_8)
        if (productBuildText.contains("pluginComposedModule(project(\":modules:test-support\"))")) {
            violations += "产品插件禁止组合 modules:test-support"
        }

        val productPluginXmlText = file("product/idea-plugin/src/main/resources/META-INF/plugin.xml")
            .readText(Charsets.UTF_8)
        if (productPluginXmlText.contains("org.cangnova.cangjie.testSupport")) {
            violations += "产品 plugin.xml 禁止包含 org.cangnova.cangjie.testSupport"
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Architecture guard failed with ${violations.size} violation(s):")
                    violations.forEach { appendLine(" - $it") }
                }
            )
        }
    }
}

tasks.register("checkArchitecture") {
    group = "verification"
    description = "运行架构守卫检查。"
    dependsOn(architectureGuard)
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(architectureGuard)
}
