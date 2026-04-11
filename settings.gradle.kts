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
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.10.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

buildCache {
    local {
        isEnabled = System.getenv("CI") == null
        directory = File(rootDir, "build/build-cache")
    }
}

rootProject.name = "intellij-cangjie"

val githubPackagesRepoUrl = "https://maven.pkg.github.com/lin-qingying/cangjie"
val githubPackagesUsername = providers.gradleProperty("GITHUB_PACKAGES_USERNAME")
    .orElse(providers.environmentVariable("GITHUB_PACKAGES_USERNAME"))
val githubPackagesToken = providers.gradleProperty("GITHUB_PACKAGES_TOKEN")
    .orElse(providers.environmentVariable("GITHUB_PACKAGES_TOKEN"))

/**
 * 仓库级凭据门禁：缺失凭据时直接失败，避免进入模糊的 401/403 状态。
 */
fun requireGitHubPackagesCredential(name: String, value: String?): String {
    val normalized = value?.trim().orEmpty()
    if (normalized.isEmpty()) {
        throw GradleException(
            "Missing credential `$name`. " +
                    "Please set it in ~/.gradle/gradle.properties or environment variables."
        )
    }
    return normalized
}

val resolvedGitHubPackagesUsername = requireGitHubPackagesCredential(
    name = "GITHUB_PACKAGES_USERNAME",
    value = githubPackagesUsername.orNull
)
val resolvedGitHubPackagesToken = requireGitHubPackagesCredential(
    name = "GITHUB_PACKAGES_TOKEN",
    value = githubPackagesToken.orNull
)

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
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
include("core")


include("lsp4ij")
include("telemetry")

include("common")
include("util")
include("icon")
include("messages")
include("toolchain")
include("notifications")
include("test-common")

include("cangjie-project")
include("cjpm")

include("debugger")
include("debugger:protobuf")
include("debugger:dap")
include("debugger:common")

include("highlighter")
include("formatter")

include("macro")
include("namedpipe")

