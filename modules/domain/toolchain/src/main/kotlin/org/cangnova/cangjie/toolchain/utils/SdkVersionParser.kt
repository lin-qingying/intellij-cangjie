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

package org.cangnova.cangjie.toolchain.utils

import com.intellij.util.text.SemVer
import org.cangnova.cangjie.toolchain.CangJieSdkVersion

/**
 * SDK 版本解析工具
 *
 * 负责从编译器输出中解析版本信息
 */
object SdkVersionParser {
    /**
     * 从 cjc --version 的输出中解析版本信息
     *
     * 输出格式示例:
     * ```
     * Cangjie Compiler: 1.0.1 (cjnative)
     * Target: x86_64-w64-mingw32
     * ```
     * 或
     * ```
     * Cangjie Compiler: 0.53.13
     * Target: aarch64-linux-ohos
     * ```
     *
     * @param output 命令输出行列表
     * @return 解析的版本信息,失败返回 null
     */
    fun parseVersionFromOutput(output: List<String>): CangJieSdkVersion? {
        if (output.isEmpty()) return null

        // 匹配 "Cangjie Compiler: x.y.z" 或 "Cangjie Compiler: x.y.z (type)" 格式
        val compilerRegex = Regex("""Cangjie Compiler:\s*(.+)""")
        val targetRegex = Regex("""Target:\s*(.+)""")

        var versionLine: String? = null
        var target: String? = null

        for (line in output) {
            // 解析版本行
            compilerRegex.find(line)?.let { match ->
                versionLine = match.groupValues[1].trim()
            }

            // 解析目标平台
            targetRegex.find(line)?.let { match ->
                target = match.groupValues[1].trim()
            }
        }

        if (versionLine == null || target == null) return null

        // 提取版本号和类型
        var versionText = versionLine!!
        var type: String? = null

        // 检查是否有括号中的类型信息
        val typeRegex = Regex("""\(([^()]+)\)""")
        typeRegex.find(versionText)?.let { match ->
            type = match.groupValues[1]
            // 移除括号部分
            versionText = versionText.replace(typeRegex, "").trim()
        }

        // 解析语义化版本
        val semVer = SemVer.parseFromText(versionText) ?: return null

        return CangJieSdkVersion(
            semver = semVer,
            targetPlatform = target!!,
            type = type
        )
    }

    /**
     * 生成 SDK 的默认 ID
     *
     * @param version SDK 版本信息
     * @return SDK ID,格式为 "CangJie-x.y.z" 或 "CangJie-x.y.z-type"
     */
    fun generateSdkId(version: CangJieSdkVersion?): String {
        if (version == null) return "CangJie-Unknown"

        val baseId = "CangJie-${version.semver}"
        return if (version.type != null) {
            "$baseId-${version.type}"
        } else {
            baseId
        }
    }

    /**
     * 生成 SDK 的默认名称
     *
     * @param version SDK 版本信息
     * @return SDK 名称,格式为 "CangJie x.y.z (targetPlatform)" 或 "CangJie x.y.z (type) - targetPlatform"
     */
    fun generateSdkName(version: CangJieSdkVersion?): String {
        if (version == null) return "CangJie SDK (Unknown)"

        val baseName = "CangJie ${version.semver}"
        return when {
            version.type != null -> "$baseName (${version.type}) - ${version.targetPlatform}"
            else -> "$baseName - ${version.targetPlatform}"
        }
    }
}
