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

package org.cangnova.cangjie.toolchain.api

import com.intellij.openapi.util.SystemInfo
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.cangnova.cangjie.toolchain.env.CangJieEnv
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * 仓颉 SDK 信息
 *
 * 表示一个已注册的仓颉 SDK 实例,包含 SDK 的路径和版本信息
 */
data class CjSdk(
    /**
     * SDK 的唯一标识符
     * 通常使用 "CangJie-{version}" 格式
     */
    val id: String,

    /**
     * SDK 的显示名称
     */
    val name: String,

    /**
     * SDK 的根目录路径
     */
    val homePath: Path,

    /**
     * SDK 版本信息
     * 如果无法解析版本则为 null
     */
    val version: CangJieSdkVersion?,

    /**
     * SDK 是否有效
     * 检查必要的可执行文件是否存在
     */
    val isValid: Boolean
) {
    /**
     * 获取 SDK 的工具目录路径
     */
    val binPath: Path
        get() = homePath.resolve("bin")

    /**
     * 获取标准库路径
     *
     * 标准库位于 {sdkHome}/modules/{platform} 目录
     * platform 格式: {os}_{arch}_llvm
     *
     * 支持的平台:
     * - windows_x86_64_llvm
     * - linux_x86_64_llvm
     * - linux_aarch64_llvm
     * - darwin_x86_64_llvm (macOS Intel)
     * - darwin_aarch64_llvm (macOS Apple Silicon)
     */
    val stdlibPath: Path
        get() {
            val modulesDir = homePath.resolve("modules")
            val baseplatform = "${detectOS()}_${detectArch()}"

            // 按优先级依次尝试各后缀
            val candidates = listOf("_llvm", "_cjnative")
                .map { suffix -> modulesDir.resolve("$baseplatform$suffix") }
                .filter { it.exists() }

            return candidates.firstOrNull() ?: modulesDir
        }

    /**
     * 检测当前平台的标识符
     *
     * @return 平台标识符，格式: {os}_{arch}_llvm
     */
    private fun detectPlatform(): String {
        val os = detectOS()
        val arch = detectArch()
        return "${os}_${arch}_llvm"
    }

    /**
     * 检测操作系统
     */
    private fun detectOS(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> "windows"
            osName.contains("mac") || osName.contains("darwin") -> "darwin"
            osName.contains("nux") -> "linux"
            else -> "unknown"
        }
    }

    /**
     * 检测 CPU 架构
     */
    private fun detectArch(): String {
        val osArch = System.getProperty("os.arch").lowercase()
        return when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "aarch64"
            osArch.contains("x86_64") || osArch.contains("amd64") -> "x86_64"
            else -> "unknown"
        }
    }

    /**
     * 获取平台特定的可执行文件路径
     *
     * 在 Windows 上自动添加 `.exe` 后缀。
     * 若未指定 [paths]，则在 [binPath] 下查找；否则从 [homePath] 开始依次拼接路径段。
     *
     * @param name 不带后缀的可执行文件名（如 `"cjc"`、`"cjc-frontend"`）
     * @param paths 可选路径段，从 [homePath] 开始拼接；为空时使用 [binPath]
     * @return 可执行文件的完整路径
     */
    fun getExecutable(name: String, vararg paths: String): Path {
        val execName = if (SystemInfo.isWindows) "$name.exe" else name
        return if (paths.isEmpty()) {
            binPath.resolve(execName)
        } else {
            paths.fold(homePath) { acc, p -> acc.resolve(p) }.resolve(execName)
        }
    }

    /**
     * 宏动态库文件扩展名（平台相关）
     *
     * - Windows: `dll`
     * - macOS: `dylib`
     * - Linux: `so`
     */
    val macroLibExtension: String
        get() = when {
            SystemInfo.isWindows -> "dll"
            SystemInfo.isMac -> "dylib"
            else -> "so"
        }

    override fun toString(): String = "$name ($version) at $homePath"

    fun getEnvironment(): Map<String, String> = CangJieEnv.getInstance(homePath).getEnvVars()
}
