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


import org.cangnova.cangjie.toolchain.flavors.CjToolchainFlavor
import org.cangnova.cangjie.toolchain.tools.*
import org.cangnova.cangjie.toolchain.wsl.getHomePathCandidates
import org.cangnova.cangjie.utils.toPath
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.SemVer
import org.cangnova.cangjie.utils.CangJieEnv
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

//val CjToolchainBase.cjpm: Cjpm
//    get() = Cjpm(this)
//
//
//val CjToolchainBase.cjc: Cjc
//    get() = Cjc(this)

fun CjToolchainBase.cjc(): Cjc {

    if (this.cjc == null) return Cjc(this)

    return this.cjc!!
}

fun CjToolchainBase.cjpm(): Cjpm {
    if (this.cjpm == null) return Cjpm(this)

    return this.cjpm!!
}

fun CjToolchainBase.cjfmt(): CjFmt {
    if (this.cjfmt == null) return CjFmt(this)

    return this.cjfmt!!
}


sealed interface ScriptHandler {
    fun parseEnvironmentVariables(scriptContent: String): Map<String, String>

    fun executeScript(scriptPath: String): Result<String>


    companion object {
        object EMPTY : ScriptHandler {
            override fun parseEnvironmentVariables(scriptContent: String): Map<String, String> {
                TODO("Not yet implemented")
            }

            override fun executeScript(scriptPath: String): Result<String> {
                TODO("Not yet implemented")
            }

        }

        fun getEnvironmentVariables(scriptPath: String): Map<String, String> {
            val handler = getScriptHandler()
            val result = handler.executeScript(scriptPath)
            return handler.parseEnvironmentVariables(result.getOrThrow())
        }

        fun getScriptHandler(): ScriptHandler {
            return when (SystemInfo.OS_NAME) {

                "Windows" -> WindowsScriptHandler

                "Linux" -> UnixScriptHandler

                "Mac" -> MacScriptHandler


                else -> EMPTY

            }
        }
    }

    object WindowsScriptHandler : ScriptHandler {
        override fun parseEnvironmentVariables(scriptContent: String): Map<String, String> {
            val envVars = mutableMapOf<String, String>()
            val lines = scriptContent.lines()
            var scriptDir: String? = null

            for (line in lines) {
                val trimmedLine = line.trim()

                // Skip comments and empty lines
                if (trimmedLine.startsWith("REM", ignoreCase = true) || trimmedLine.startsWith(
                        "@REM",
                        ignoreCase = true
                    ) || trimmedLine.isEmpty()
                ) {
                    continue
                }

                // Match "set" statements
                if (trimmedLine.startsWith("set", ignoreCase = true)) {
                    val match = Regex("""set\s+"?(\w+)"?\s*=\s*(.+)""").find(trimmedLine)
                    if (match != null) {
                        val key = match.groupValues[1]
                        var value = match.groupValues[2]

                        // Handle %~dp0 for script directory
                        if (scriptDir == null && value.contains("%~dp0")) {
                            scriptDir = "C:\\Path\\To\\Script\\" // Replace with actual script directory
                        }

                        // Replace %~dp0 with script directory
                        value = value.replace("%~dp0", scriptDir ?: "")

                        // Resolve variables like %VAR_NAME%
                        value = value.replace(Regex("%(\\w+)%")) { matchResult ->
                            val varName = matchResult.groupValues[1]
                            envVars[varName] ?: ""
                        }

                        envVars[key] = value
                    }
                }
            }

            return envVars
        }

        override fun executeScript(scriptPath: String): Result<String> {
            return try {
                val process = ProcessBuilder("cmd.exe", "/c", scriptPath)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()

                if (process.exitValue() == 0) {
                    Result.success(output)
                } else {
                    Result.failure(Exception("Script execution failed: $output"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    object UnixScriptHandler : ScriptHandler {
        override fun parseEnvironmentVariables(scriptContent: String): Map<String, String> {
            val envVars = mutableMapOf<String, String>()
            val lines = scriptContent.lines()

            for (line in lines) {
                val trimmedLine = line.trim()

                // Skip comments and empty lines
                if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
                    continue
                }

                // Match "export VAR=value" or "VAR=value"
                val match = Regex("""(?:export\s+)?(\w+)\s*=\s*(.+)""").find(trimmedLine)
                if (match != null) {
                    val key = match.groupValues[1]
                    var value = match.groupValues[2]

                    // Resolve variables like $VAR_NAME
                    value = value.replace(Regex("""\$(\w+)""")) { matchResult ->
                        val varName = matchResult.groupValues[1]
                        envVars[varName] ?: ""
                    }

                    envVars[key] = value
                }
            }

            return envVars
        }

        override fun executeScript(scriptPath: String): Result<String> {
            return try {
                val process = ProcessBuilder("/bin/bash", scriptPath)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()

                if (process.exitValue() == 0) {
                    Result.success(output)
                } else {
                    Result.failure(Exception("Script execution failed: $output"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    object MacScriptHandler : ScriptHandler {
        override fun parseEnvironmentVariables(scriptContent: String): Map<String, String> {
            val envVars = mutableMapOf<String, String>()
            val lines = scriptContent.lines()

            for (line in lines) {
                val trimmedLine = line.trim()

                // Skip comments and empty lines
                if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
                    continue
                }

                // Match "export VAR=value" or "VAR=value"
                val match = Regex("""(?:export\s+)?(\w+)\s*=\s*(.+)""").find(trimmedLine)
                if (match != null) {
                    val key = match.groupValues[1]
                    var value = match.groupValues[2]

                    // Resolve variables like $VAR_NAME
                    value = value.replace(Regex("""\$(\w+)""")) { matchResult ->
                        val varName = matchResult.groupValues[1]
                        envVars[varName] ?: ""
                    }

                    envVars[key] = value
                }
            }

            return envVars
        }

        override fun executeScript(scriptPath: String): Result<String> {
            return try {
                val process = ProcessBuilder("/bin/zsh", scriptPath) // macOS 默认支持 Zsh
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()

                if (process.exitValue() == 0) {
                    Result.success(output)
                } else {
                    Result.failure(Exception("Script execution failed: $output"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}


abstract class CjToolchainBase(var location: Path = "".toPath()) {


    val binPath: Path
    val toolsPath: Path

    var cjpm: Cjpm? = null
    var cjc: Cjc? = null
    var cjfmt: CjFmt? = null
    val presentableLocation: String get() = pathToExecutable(Cjpm.Companion.NAME).toString()


//    标准库位置


    init {

        fun String.toSystemPath(): String {
            if (SystemInfo.isWindows) {
                return "$this.exe"
            } else {
                return this
            }
        }

//        判断location下是否有bin目录
        val binPath = location.resolve("bin")
        val toolsPath = location.resolve("tools")
        if (Files.exists(binPath) && Files.exists(toolsPath)) {
//            当前在主目录

            this.binPath = binPath
            this.toolsPath = toolsPath
        } else if (Files.exists(location.resolve("cjc".toSystemPath()))) {
//       当前在bin目录
            this.location = location.parent
            this.binPath = location
            this.toolsPath = this.location.resolve("tools")

        } else {

//            TODO 是否需要获取仓颉环境变量


//            this.location = Path.of(System.getenv("CANGJIE_HOME"))
//            this.binPath = this.location.resolve("bin")
//            this.toolsPath = this.location.resolve("tools")
            this.binPath = location
            this.toolsPath = location
        }

    }

    fun getEnvironment(): Map<String, String> = CangJieEnv.getInstance(location).getEnvVars()

    fun buildPath(vararg paths: String): String {
        return paths.joinToString(File.separator, File.separator)
    }

    //    val presentableLocation: String get() = pathToExecutable(CJPM.NAMED).toString()
    abstract fun pathToExecutable(toolName: String): Path
    abstract val fileSeparator: String
    abstract val executionTimeoutInMilliseconds: Int
//    fun looksLikeValidToolchain(): Boolean = CjToolchainFlavor.getFlavor(location) != null

    abstract fun hasExecutable(exec: String): Boolean

    abstract fun hasCjpmExecutable(exec: String): Boolean

    abstract val platformType: String
    fun looksLikeValidToolchain(): Boolean = CjToolchainFlavor.Companion.getFlavor(location) != null

    abstract fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine

    abstract fun toLocalPath(remotePath: String): String

    abstract fun toRemotePath(localPath: String): String

    abstract fun expandUserHome(remotePath: String): String

    abstract fun getExecutableName(toolName: String): String

    override fun toString(): String {
        return "Platform: $platformType, Location: $location"
    }

    override fun hashCode(): Int {
        return location.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == this.hashCode()
    }

    fun pathToCjpmExecutable(toolName: String): Path {

        val exePath = pathToExecutable(toolName)
        if (exePath.exists()) return exePath
        val cjpmBin = expandUserHome("~/.cangjie/bin")
        val exeName = getExecutableName(toolName)
        return Paths.get(cjpmBin, exeName)
    }

    fun createGeneralCommandLine(
        executable: Path,
        workingDirectory: Path,
        redirectInputFrom: File?,

        environmentVariables: EnvironmentVariablesData,
        parameters: List<String>,
        emulateTerminal: Boolean,
        withSudo: Boolean,
        patchToRemote: Boolean = true,

        ): GeneralCommandLine {

        val env =

            if (environmentVariables.envs.isEmpty()) {

                EnvironmentVariablesData.create(getEnvironment(), true)

            } else {
                environmentVariables
            }

        var commandLine = org.cangnova.cangjie.toolchain.tools.GeneralCommandLine(executable, withSudo)
            .withWorkDirectory(workingDirectory)
            .withInput(redirectInputFrom)
//            .withEnvironment("TERM", "ansi")
            .withParameters(parameters)
            .withCharset(Charsets.UTF_8)
            .withRedirectErrorStream(true)

        env.configureCommandLine(commandLine, true)
        if (emulateTerminal) {
            commandLine = PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false)
        }
        if (patchToRemote) {
            commandLine = patchCommandLine(commandLine)
        }
        return commandLine

    }

    fun toSerializedString(): String {

        return "$platformType::::$location"

    }


    companion object {

        val MIN_SUPPORTED_TOOLCHAIN = "0.53.4".parseSemVer()

        //        标准库下载地址
        val STDLIB_DOWNLOAD_URL =
            "https://gitee.com/Lin_Qing_Ying/intellij-cangjie-stdlib/releases/download/0.53.4/intellij-cangjie-stdlib.zip"

        fun getStdlibDowloadUrl(version: String): String {

            return "https://gitee.com/Lin_Qing_Ying/intellij-cangjie-stdlib/releases/download/$version/intellij-cangjie-stdlib.zip"
        }

        //        标准库位置
        val stdlibPath =
            File(System.getProperty("user.home")).resolve(".cangjie").resolve("stdlib").toPath()
        val stdlibPathByVersion: Path
            get() {

                return stdlibPath
            }

        init {
            if (!stdlibPath.exists()) {
                stdlibPath.toFile().mkdirs()
            }
        }


        fun fromSerializedString(serializedString: String): CjToolchainBase? {

            val (platform, location) = serializedString.split("::::")

//            TODO 扩展点选择的平台是什么？
            return CjToolchainProvider.getToolchain(Paths.get(location))

//            return when (platform) {
//                "loacl" -> CjLocalToolchain(location.toPath())
//
//                else -> null
//            }
        }

        /**
         * 检查给定路径是否是有效的 SDK。
         *
         * @param homePath SDK 的主路径
         * @return 如果是有效的 SDK，则返回 true，否则返回 false
         */
        fun checkForSdk(homePath: Path): Boolean {
            return (Files.exists(homePath.resolve("bin/cjc")) ||
                    (SystemInfo.isWindows && Files.exists(homePath.resolve("bin/cjc.exe"))) && Files.exists(
                homePath.resolve(
                    "tools/bin/cjpm.exe"
                )
            ))

        }

        @JvmOverloads
        fun suggest(projectDir: Path? = null): CjToolchainBase? {
            val distribution = projectDir?.let { WslPath.getDistributionByWindowsUncPath(it.toString()) }
            val toolchain = distribution
                ?.getHomePathCandidates()
                ?.filter { CjToolchainFlavor.Companion.getFlavor(it) != null }
                ?.mapNotNull { CjToolchainProvider.getToolchain(it.toAbsolutePath()) }
                ?.firstOrNull()
            if (toolchain != null) return toolchain

            return CjToolchainFlavor.Companion.getApplicableFlavors()
                .asSequence()
                .flatMap { it.suggestHomePaths() }
                .mapNotNull { CjToolchainProvider.getToolchain(it.toAbsolutePath()) }
                .firstOrNull()

        }
    }
}


fun String.parseSemVer(): SemVer =
    checkNotNull(SemVer.parseFromText(this)) { "Invalid version value: $this" }
