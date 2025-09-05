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

package org.cangnova.cangjie.toolchain

import org.cangnova.cangjie.toolchain.state.ToolchainSettingsState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.SemVer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

fun String.toPath(): Path {
    return Path(this)
}

interface CangJieToolchain {


    val homePath: Path

    /**
     * 获取工具链的平台类型
     */
    val platformType: PlatformType

    /**
     * 获取工具链的版本信息
     */
    val version: SdkVersion?


    /**
     * 工具链是否可用
     */
    val isAvailable: Boolean
        get() = homePath.toString().isNotEmpty() && pathToExecutable("bin".toPath().resolve("cjc")).toFile().exists()


    abstract fun pathToExecutable(toolPath: Path): Path
    abstract val fileSeparator: String
    abstract fun hasExecutable(execPath: Path): Boolean
    abstract fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine


    /**
     * 获取命令执行超时时间（毫秒）
     */
    val executionTimeoutInMilliseconds: Int get() = DEFAULT_TIMEOUT

    companion object {
        const val DEFAULT_TIMEOUT = 60000
        fun create(homePath: String): CangJieToolchain {
            return create(homePath.toPath())
        }

        fun create(homePath: Path): CangJieToolchain {
            return CangJieToolchainImpl(homePath)
        }

        fun getToolchain(): CangJieToolchain {
            return CangJieToolchainImpl(ToolchainSettingsState.getInstance().path.toPath())
        }

    }
}

internal class CangJieToolchainImpl(
    override val homePath: Path,
    override val platformType: PlatformType = PlatformType.LOCAL,
) : CangJieToolchain {



    private val executable: Path = pathToExecutable("bin".toPath().resolve("cjc"))

    private fun parseVersion(): SdkVersion? {
        if(!isAvailable) return null
        val process = ProcessBuilder(executable.toString(), "--version")
            .redirectErrorStream(true)
            .start()

        val lines = process.inputStream.bufferedReader().readLines()
        process.waitFor(executionTimeoutInMilliseconds.toLong(), TimeUnit.MILLISECONDS)


        val cangjieComiler = """Cangjie Compiler: (\d+\.\d+\.\d+.*)""".toRegex()


        val find = { re: Regex -> lines.firstNotNullOfOrNull { re.matchEntire(it) } }
        val releaseMatch = find(cangjieComiler) ?: return null

        val hostRe = "Target:(.*)".toRegex()
        val hostText = find(hostRe)?.groups?.get(1)?.value?.trim() ?: return null
        var versionText = releaseMatch.groups[1]?.value ?: return null


//    分割
        var type: String? = null
        return try {

//去掉括号
            val typeRegex = Regex("\\(([^()]*)\\)")

            type = typeRegex.find(versionText.split(" ")[1])?.groups?.get(1)?.value
            versionText = versionText.split(" ")[0]

            val semVer = SemVer.parseFromText(versionText) ?: return null
            SdkVersion(semVer, hostText, type)

        } catch (iex: IndexOutOfBoundsException) {
            null
        }

    }

    private var _version: SdkVersion? = SemVer.parseFromText("")?.let { SdkVersion(it, "", "") }
    override val version: SdkVersion?
        get() {
            return _version
        }


    override fun pathToExecutable(toolPath: Path): Path {
        return homePath.pathToExecutable(toolPath)
    }

    override val fileSeparator: String
        get() = File.separator

    override fun hasExecutable(execPath: Path): Boolean {
        return homePath.hasExecutable(execPath)
    }

    override fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine {
        return commandLine
    }

    init {
        runBlocking {
            launch(Dispatchers.IO) {
                _version = parseVersion()
            }
        }

    }
}


data class SdkVersion(
    val semver: SemVer,
    val host: String,
    val type: String?
)

/**
 * 平台类型枚举
 */
enum class PlatformType {
    /**
     * 本地平台
     */
    LOCAL,

    /**
     * 远程平台
     */
    REMOTE,

    /**
     * WSL (Windows Subsystem for Linux)
     */
    WSL,

    /**
     * Docker 容器
     */
    DOCKER,

    /**
     * 自定义平台
     */
    CUSTOM
}

fun Path.hasExecutable(toolName: Path): Boolean = pathToExecutable(toolName).isExecutable()
fun Path.pathToExecutable(toolPath: Path): Path {
    val exeName = if (SystemInfo.isWindows) "$toolPath.exe".toPath() else toolPath
    return resolve(exeName).toAbsolutePath()
}

fun Path.isExecutable(): Boolean = Files.isExecutable(this)
