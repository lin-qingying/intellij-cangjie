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

package org.cangnova.cangjie.toolchain.flavors

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.SystemInfo
import org.cangnova.cangjie.toolchain.flavors.CjToolchainFlavor.Companion.getApplicableFlavors
import org.cangnova.cangjie.toolchain.tools.Cjc
import org.cangnova.cangjie.toolchain.tools.Cjpm

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun Path.pathToExecutable(toolName: String): Path {
    val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
    return resolve(exeName).toAbsolutePath()
}

fun Path.hasExecutable(toolName: String): Boolean = pathToExecutable(toolName).isExecutable()
fun Path.isExecutable(): Boolean = Files.isExecutable(this)

/**
 * 表示一个工具链的 Flavor 抽象类。
 * 提供了工具链路径的建议、验证和适用性检查等功能。
 */
abstract class CjToolchainFlavor {

    /**
     * 获取建议的工具链路径。
     * 仅返回有效的工具链路径。
     *
     * @return 有效工具链路径的序列。
     */
    fun suggestHomePaths(): Sequence<Path> = getHomePathCandidates().filter { isValidToolchainPath(it) }

    /**
     * 获取工具链路径候选者。
     * 子类需要实现此方法以提供具体的路径候选逻辑。
     *
     * @return 工具链路径候选者的序列。
     */
    protected abstract fun getHomePathCandidates(): Sequence<Path>

    /**
     * 获取指定工具的可执行文件路径。
     *
     * @param path 工具链路径。
     * @param toolName 工具名称。
     * @return 工具的可执行文件路径。
     */
    protected open fun pathToExecutable(
        path: Path,
        toolName: String
    ): Path {
        return path.pathToExecutable(toolName)
    }

    /**
     * 验证工具链路径是否有效。
     * 工具链路径必须是目录，并且包含指定的可执行工具。
     *
     * @param path 工具链路径。
     * @return 如果路径有效则返回 true，否则返回 false。
     */
    protected open fun isValidToolchainPath(path: Path): Boolean {
        return path.isDirectory() &&
                hasExecutable(path, Cjc.Companion.NAME) &&
                hasExecutable(path, Cjpm.Companion.NAME)
    }

    /**
     * 检查指定路径下是否存在指定工具的可执行文件。
     *
     * @param path 工具链路径。
     * @param toolName 工具名称。
     * @return 如果存在可执行文件则返回 true，否则返回 false。
     */
    protected open fun hasExecutable(path: Path, toolName: String): Boolean = path.hasExecutable(toolName)

    /**
     * 检查当前 Flavor 是否适用。
     * 如果返回 true，则会将此 Flavor 添加到 [getApplicableFlavors] 的结果中。
     *
     * @return 如果适用则返回 true，否则返回 false。
     */
    protected open fun isApplicable(): Boolean = true

    companion object {
        private val EP_NAME: ExtensionPointName<CjToolchainFlavor> =
            ExtensionPointName.create("org.cangnova.cangjie.toolchainFlavor")

        /**
         * 获取所有适用的工具链 Flavor。
         *
         * @return 适用的工具链 Flavor 列表。
         */
        fun getApplicableFlavors(): List<CjToolchainFlavor> =
            EP_NAME.extensionList.filter { it.isApplicable() }

        /**
         * 根据路径获取对应的工具链 Flavor。
         *
         * @param path 工具链路径。
         * @return 如果找到适用的 Flavor，则返回该 Flavor；否则返回 null。
         */
        fun getFlavor(path: Path): CjToolchainFlavor? =
            getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
    }
}