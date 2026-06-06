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
 */
import org.gradle.api.GradleException
import org.gradle.authentication.http.BasicAuthentication
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    includeBuild("build-logic")


    repositories {

        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.10.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
includeBuild("../") {
    dependencySubstitution {
        substitute(module("org.cangnova.cangjie:cangjie-frontend-common-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-psi-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-psi-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-formatting-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-formatting-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-folding-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-folding-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-highlighting-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-highlighting-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-code-insight-refactoring-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-refactoring-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-cfir-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-cfir-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-analysis-api-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-analysis-api-cfir-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-cfir-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-analysis-api-standalone-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-standalone-for-ide-module"))
    }
}
rootProject.name = "intellij-cangjie"

buildCache {
    local {
        isEnabled = System.getenv("CI") == null
        directory = File(rootDir, "build/build-cache")
    }
}

val githubPackagesRepoUrl = "https://maven.pkg.github.com/lin-qingying/cangjie"
val githubPackagesUsername = providers.gradleProperty("GITHUB_PACKAGES_USERNAME")
    .orElse(providers.environmentVariable("GITHUB_PACKAGES_USERNAME"))
val githubPackagesToken = providers.gradleProperty("GITHUB_PACKAGES_TOKEN")
    .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))

fun requireGitHubPackagesCredential(name: String, value: String?): String {
    val normalized = value?.trim().orEmpty()
    if (normalized.isEmpty()) {
        throw GradleException(
            "Missing credential `$name`. Please set it in ~/.gradle/gradle.properties or environment variables."
        )
    }
    return normalized
}

val resolvedGitHubPackagesUsername = requireGitHubPackagesCredential(
    name = "GITHUB_PACKAGES_USERNAME",
    value = githubPackagesUsername.orNull,
)
val resolvedGitHubPackagesToken = requireGitHubPackagesCredential(
    name = "GITHUB_PACKAGES_TOKEN",
    value = githubPackagesToken.orNull,
)

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        // 优先解析本地发布产物，支持与主仓库联调：
        // 1. ../build/repo 是 cangjie 根工程默认 publish 落盘的位置
        // 2. mavenLocal() 兼容显式执行 publishToMavenLocal 的场景
        maven {
            url = uri("../build/repo")
        }
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            content {
                // 复合构建下，根工程源码桥接模块会解析 IntelliJ Platform 的 Maven 工件。
                // 这些坐标如果先命中通用镜像仓库，Gradle 会把后续工件下载也粘在该仓库上，
                // 一旦镜像同步滞后，就会把 not-found 结果缓存下来并直接导致 IDEA 同步失败。
                // 因此这里显式禁止华为云仓库接管 JetBrains 平台相关 group，
                // 统一交给下面的 intellijPlatform/defaultRepositories 体系解析。
                excludeGroup("com.jetbrains.intellij.platform")
                excludeGroup("com.jetbrains.intellij")
                excludeGroup("com.jetbrains.intellij.remoteDev")
                excludeGroup("org.jetbrains.intellij")
                excludeGroup("org.jetbrains.intellij.plugins")
                excludeGroup("com.intellij.platform")
            }
        }
        maven { url = uri("https://jitpack.io") }
        maven {
            name = "CangJieGitHubPackages"
            url = uri(githubPackagesRepoUrl)
            credentials {
                username = resolvedGitHubPackagesUsername
                password = resolvedGitHubPackagesToken
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        gradlePluginPortal()
        intellijPlatform {
            defaultRepositories()
            intellijDependencies()
            localPlatformArtifacts()
            marketplace()
        }
    }
}

include(":product:idea-plugin")

include(":modules:foundation")
include(":modules:domain:toolchain")
include(":modules:domain:project-model")
include(":modules:domain:package-manager")
include(":modules:domain:telemetry")
include(":modules:ide:base")
include(":modules:ide:project")
include(":modules:ide:run")
include(":modules:ide:lsp")
include(":modules:ide:debugger-api")
include(":modules:ide:debugger-dap")
include(":modules:ide:debugger-proto")
include(":modules:ide:macro")
include(":modules:ide:ux")
include(":modules:test-support")
