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

package org.cangnova.cangjie.toolchain.impl

import com.intellij.openapi.util.SystemInfo
import org.cangnova.cangjie.toolchain.api.CjSdkDetector
import org.cangnova.cangjie.toolchain.api.CjSdkDiscoverer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.streams.asSequence

/**
 * SDK 发现器的默认实现
 *
 * 在常见位置搜索 CangJie SDK
 */
internal class CjSdkDiscovererImpl : CjSdkDiscoverer {

    override val priority: Int = 0

    override val displayName: String = "Default CangJie SDK Discoverer"

    private val detector: CjSdkDetector?
        get() = CjSdkDetector.getInstance()

    override fun discoverSdkPaths(): List<Path> {
        val paths = mutableListOf<Path>()

        // 1. 检查环境变量
        paths.addAll(discoverFromEnvironment())

        // 2. 检查常见安装位置
        paths.addAll(discoverFromCommonLocations())

        // 3. 检查用户主目录
        paths.addAll(discoverFromUserHome())

        return paths.filter { detector?.isValidSdk(it) == true }.distinct()
    }

    override fun searchSdkPaths(searchPath: Path, recursive: Boolean, maxDepth: Int): List<Path> {
        if (!searchPath.exists() || !searchPath.isDirectory()) {
            return emptyList()
        }

        val results = mutableListOf<Path>()
        val det = detector ?: return emptyList()

        // 检查当前路径
        if (det.isValidSdk(searchPath)) {
            results.add(searchPath)
        }

        // 递归搜索
        if (recursive && maxDepth > 0) {
            try {
                searchPath.listDirectoryEntries().forEach { subPath ->
                    if (subPath.isDirectory()) {
                        results.addAll(searchSdkPaths(subPath, true, maxDepth - 1))
                    }
                }
            } catch (e: Exception) {
                // 忽略访问错误
            }
        }

        return results.distinct()
    }

    /**
     * 从环境变量中发现 SDK
     */
    private fun discoverFromEnvironment(): List<Path> {
        val paths = mutableListOf<Path>()

        // CANGJIE_HOME
        System.getenv("CANGJIE_HOME")?.let { home ->
            Paths.get(home).takeIf { it.exists() }?.let { paths.add(it) }
        }

        // CANGJIE_SDK
        System.getenv("CANGJIE_SDK")?.let { sdk ->
            Paths.get(sdk).takeIf { it.exists() }?.let { paths.add(it) }
        }

        // PATH 环境变量中的 cjc 路径
        System.getenv("PATH")?.split(System.getProperty("path.separator"))?.forEach { pathEntry ->
            try {
                val path = Paths.get(pathEntry)
                if (path.exists() && path.fileName.toString() == "bin") {
                    val parent = path.parent
                    if (parent != null && parent.exists()) {
                        paths.add(parent)
                    }
                }
            } catch (e: Exception) {
                // 忽略无效路径
            }
        }

        return paths
    }

    /**
     * 从常见安装位置发现 SDK
     */
    private fun discoverFromCommonLocations(): List<Path> {
        val paths = mutableListOf<Path>()

        when {
            SystemInfo.isWindows -> {
                // Windows 常见位置
                listOf(
                    "C:\\Program Files\\CangJie",
                    "C:\\Program Files (x86)\\CangJie",
                    "C:\\CangJie",
                    System.getenv("LOCALAPPDATA")?.let { "$it\\CangJie" }
                ).filterNotNull().forEach { location ->
                    searchSdkPaths(Paths.get(location), recursive = true, maxDepth = 2)
                        .forEach { paths.add(it) }
                }
            }

            SystemInfo.isMac -> {
                // macOS 常见位置
                listOf(
                    "/usr/local/cangjie",
                    "/opt/cangjie",
                    "/Library/CangJie",
                    "/Applications/CangJie"
                ).forEach { location ->
                    searchSdkPaths(Paths.get(location), recursive = true, maxDepth = 2)
                        .forEach { paths.add(it) }
                }
            }

            SystemInfo.isLinux -> {
                // Linux 常见位置
                listOf(
                    "/usr/local/cangjie",
                    "/opt/cangjie",
                    "/usr/cangjie"
                ).forEach { location ->
                    searchSdkPaths(Paths.get(location), recursive = true, maxDepth = 2)
                        .forEach { paths.add(it) }
                }
            }
        }

        return paths
    }

    /**
     * 从用户主目录发现 SDK
     */
    private fun discoverFromUserHome(): List<Path> {
        val paths = mutableListOf<Path>()
        val userHome = System.getProperty("user.home") ?: return paths

        // 用户目录下的常见位置
        listOf(
            ".cangjie",
            "cangjie",
            ".local/cangjie",
            "sdk/cangjie"
        ).forEach { subPath ->
            val sdkPath = Paths.get(userHome, subPath)
            searchSdkPaths(sdkPath, recursive = true, maxDepth = 2)
                .forEach { paths.add(it) }
        }

        return paths
    }
}
