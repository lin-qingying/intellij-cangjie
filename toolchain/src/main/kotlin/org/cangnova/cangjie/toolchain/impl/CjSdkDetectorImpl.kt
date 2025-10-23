/*
 * Copyright 2025 LinQingYing. and contributors.
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
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkDetector
import org.cangnova.cangjie.toolchain.utils.SdkVersionParser
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * SDK 检测器的默认实现
 */
internal class CjSdkDetectorImpl : CjSdkDetector {

    override val requiredExecutables: List<String> = listOf("cjc")

    override fun isValidSdk(homePath: Path): Boolean {
        if (!homePath.exists() || !homePath.isDirectory()) {
            return false
        }

        // 检查 bin 目录是否存在
        val binPath = homePath.resolve("bin")
        if (!binPath.exists() || !binPath.isDirectory()) {
            return false
        }

        // 检查必需的可执行文件
        return checkRequiredExecutables(homePath).isEmpty()
    }

    override fun detectVersion(homePath: Path): CangJieSdkVersion? {
        if (!isValidSdk(homePath)) return null

        val cjcExecutable = getExecutablePath(homePath, "cjc")
        if (!Files.isExecutable(cjcExecutable)) return null

        return try {
            // 执行 cjc --version
            val process = ProcessBuilder(cjcExecutable.toString(), "--version")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readLines()
            val completed = process.waitFor(5, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                return null
            }

            // 解析版本信息
            SdkVersionParser.parseVersionFromOutput(output)
        } catch (e: Exception) {
            null
        }
    }

    override fun createSdk(homePath: Path, customName: String?): CjSdk? {
        if (!isValidSdk(homePath)) return null

        val version = detectVersion(homePath)
        val isValid = version != null

        val name = customName ?: SdkVersionParser.generateSdkName(version)
        val id = SdkVersionParser.generateSdkId(version)

        return CjSdk(
            id = id,
            name = name,
            homePath = homePath,
            version = version,
            isValid = isValid
        )
    }

    override fun checkRequiredExecutables(homePath: Path): List<String> {
        val missing = mutableListOf<String>()

        for (execName in requiredExecutables) {
            val execPath = getExecutablePath(homePath, execName)
            // 检查文件是否存在且可执行(使用 File.canExecute 或 Files.isExecutable)
            val exists = execPath.exists()
            val executable = exists && (execPath.toFile().canExecute() || Files.isExecutable(execPath))

            if (!executable) {
                missing.add(execName)
            }
        }

        return missing
    }

    /**
     * 获取可执行文件的完整路径
     *
     * @param homePath SDK 根目录
     * @param execName 可执行文件名称(不含扩展名)
     * @return 可执行文件路径
     */
    private fun getExecutablePath(homePath: Path, execName: String): Path {
        val binPath = homePath.resolve("bin")
        val executableName = if (SystemInfo.isWindows) "$execName.exe" else execName
        return binPath.resolve(executableName)
    }
}
