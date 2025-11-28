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

package org.cangnova.cangjie.debugger.protobuf.util

import com.intellij.execution.CommandLineUtil
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.jna.JnaLoader
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.Strings
import com.intellij.util.EnvironmentUtil
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.sun.jna.Structure
import com.sun.jna.platform.unix.LibC
import com.sun.jna.platform.win32.*
import com.sun.jna.ptr.IntByReference
import org.cangnova.cangjie.debugger.protobuf.messages.ProtoDebuggerBundle
import org.jetbrains.annotations.Nls
import java.io.File
import java.io.IOException
import java.util.function.Supplier

// ============================================================================
// 平台检测与权限检查
// ============================================================================

/**
 * 检查当前进程是否以提升的权限运行
 * - Windows: 检查 UAC 提升状态
 * - Unix/Linux: 检查是否以 root 身份运行 (euid == 0)
 */
val isSuperUser: Boolean by lazy {
    runCatching {
        when {
            !JnaLoader.isLoaded() -> false
            SystemInfo.isWindows -> WindowsPrivilegeChecker.isElevated()
            SystemInfo.isUnix -> UnixPrivilegeChecker.isSuperUser()
            else -> false
        }
    }.getOrDefault(false)
}

// ============================================================================
// 系统工具检测（延迟加载）
// ============================================================================

private object SystemUtilities {
    val hasGkSudo = PathExecLazyValue.create("gksudo")
    val hasKdeSudo = PathExecLazyValue.create("kdesudo")
    val hasPkExec = PathExecLazyValue.create("pkexec")
    val hasGnomeTerminal = PathExecLazyValue.create("gnome-terminal")
    val hasKdeTerminal = PathExecLazyValue.create("konsole")
    val hasUrxvt = PathExecLazyValue.create("urxvt")
    val hasXTerm = PathExecLazyValue.create("xterm")
    val hasSetsid = PathExecLazyValue.create("setsid")
}

// ============================================================================
// Sudo 命令构建
// ============================================================================

/**
 * 根据当前平台为命令行添加 sudo/提权包装
 * 如果已经以超级用户身份运行，则返回原始命令
 */
@Throws(ExecutionException::class, IOException::class)
fun sudoCommand(commandLine: GeneralCommandLine, prompt: @Nls String): GeneralCommandLine {
    if (isSuperUser) return commandLine

    val sudoCommandLine = buildPlatformSpecificSudoCommand(commandLine, prompt)
        ?: throw UnsupportedOperationException("无法在此系统上执行 sudo - 未找到合适的工具")

    return sudoCommandLine.apply {
        withWorkDirectory(commandLine.workDirectory)
        withEnvironment(commandLine.environment)
        withParentEnvironmentType(determineParentEnvironmentType(commandLine))
        withRedirectErrorStream(commandLine.isRedirectErrorStream)
    }
}

/**
 * 使用 sudo 权限执行命令并返回进程
 */
@Throws(ExecutionException::class, IOException::class)
fun sudo(commandLine: GeneralCommandLine, prompt: @Nls String): Process =
    sudoCommand(commandLine, prompt).createProcess()

private fun determineParentEnvironmentType(commandLine: GeneralCommandLine) =
    if (SystemInfoRt.isWindows) GeneralCommandLine.ParentEnvironmentType.NONE
    else commandLine.parentEnvironmentType

// ============================================================================
// 平台特定的 Sudo 实现
// ============================================================================

private fun buildPlatformSpecificSudoCommand(
    wrappedCommand: GeneralCommandLine,
    prompt: @Nls String
): GeneralCommandLine? {
    val commandParts = buildList {
        add(wrappedCommand.exePath)
        addAll(wrappedCommand.parametersList.list)
    }

    return when {
        SystemInfoRt.isWindows -> WindowsSudoBuilder.build(wrappedCommand)
        SystemInfoRt.isMac -> MacOsSudoBuilder.build(commandParts, prompt)
        else -> LinuxSudoBuilder.build(wrappedCommand, commandParts, prompt)
    }
}

// ============================================================================
// Windows 特定实现
// ============================================================================

private object WindowsPrivilegeChecker {
    fun isElevated(): Boolean {
        val tokenHandle = WinNT.HANDLEByReference()
        val currentProcess = Kernel32.INSTANCE.GetCurrentProcess()

        if (!Advapi32.INSTANCE.OpenProcessToken(
                currentProcess,
                WinNT.TOKEN_ADJUST_PRIVILEGES or WinNT.TOKEN_QUERY,
                tokenHandle
            )
        ) {
            throw createWindowsApiException("OpenProcessToken")
        }

        return try {
            val token = TOKEN_ELEVATION()
            val cbNeeded = IntByReference(0)
            val infoClass = WinNT.TOKEN_INFORMATION_CLASS.TokenElevation

            if (!Advapi32.INSTANCE.GetTokenInformation(
                    tokenHandle.value, infoClass, token, token.size(), cbNeeded
                )
            ) {
                throw createWindowsApiException("GetTokenInformation")
            }

            token.TokenIsElevated.toInt() != 0
        } finally {
            Kernel32.INSTANCE.CloseHandle(tokenHandle.value)
        }
    }

    private fun createWindowsApiException(functionName: String): RuntimeException {
        val lastError = Kernel32.INSTANCE.GetLastError()
        val message = Kernel32Util.formatMessageFromLastErrorCode(lastError)
        return RuntimeException("$functionName 失败: $lastError - $message")
    }

    class TOKEN_ELEVATION : Structure() {
        @JvmField
        var TokenIsElevated = WinDef.DWORD(0)
    }
}

private object WindowsSudoBuilder {
    fun build(wrappedCommand: GeneralCommandLine): GeneralCommandLine {
        val launcherExe = PathManager.findBinFileWithException("launcher.exe")
        return GeneralCommandLine(buildList {
            add(launcherExe.toString())
            add(wrappedCommand.exePath)
            addAll(wrappedCommand.parametersList.parameters)
        })
    }
}

// ============================================================================
// macOS 特定实现
// ============================================================================

private object MacOsSudoBuilder {
    @NlsSafe
    val osascriptPath: String = "/usr/bin/osascript"

    fun build(command: List<String>, prompt: @Nls String): GeneralCommandLine {
        val escapedCommand = command.joinToString(" & \" \" & ") { escapeAppleScriptArgument(it) }
        val promptClause = " with prompt \"${StringUtil.escapeQuotes(prompt)}\""

        val appleScript = """
            |tell current application
            |   activate
            |   do shell script $escapedCommand$promptClause with administrator privileges without altering line endings
            |end tell
        """.trimMargin()

        return GeneralCommandLine(osascriptPath, "-e", appleScript)
    }

    fun escapeAppleScriptArgument(arg: String): String =
        "quoted form of \"${arg.replace("\"", "\\\"").replace("\\", "\\\\")}\""
}

// ============================================================================
// Linux 特定实现
// ============================================================================

private object LinuxSudoBuilder {
    fun build(
        wrappedCommand: GeneralCommandLine,
        command: List<String>,
        prompt: @Nls String
    ): GeneralCommandLine? = when {
        SystemUtilities.hasGkSudo.get() == true -> buildGkSudo(wrappedCommand, command, prompt)
        SystemUtilities.hasKdeSudo.get() == true -> buildKdeSudo(wrappedCommand, command, prompt)
        SystemUtilities.hasPkExec.get() == true -> buildPkExec(wrappedCommand, command)
        hasTerminalApp() -> buildTerminalSudo(wrappedCommand, command, prompt)
        else -> null
    }

    private fun buildGkSudo(
        wrappedCommand: GeneralCommandLine,
        command: List<String>,
        prompt: @Nls String
    ) = GeneralCommandLine(buildList {
        addAll(listOf("gksudo", "--message", prompt, "--"))
        addAll(envCommand(wrappedCommand))
        addAll(command)
    })

    private fun buildKdeSudo(
        wrappedCommand: GeneralCommandLine,
        command: List<String>,
        prompt: @Nls String
    ) = GeneralCommandLine(buildList {
        addAll(listOf("kdesudo", "--comment", prompt, "--"))
        addAll(envCommand(wrappedCommand))
        addAll(command)
    })

    private fun buildPkExec(
        wrappedCommand: GeneralCommandLine,
        command: List<String>
    ) = GeneralCommandLine(buildList {
        add("pkexec")
        addAll(envCommand(wrappedCommand))
        addAll(command)
    })

    private fun buildTerminalSudo(
        wrappedCommand: GeneralCommandLine,
        command: List<String>,
        prompt: @Nls String
    ): GeneralCommandLine {
        val escapedCommand = command.joinToString(" ") { escapeUnixShellArgument(it) }
        val escapedEnv = buildEscapedEnvCommand(wrappedCommand)

        val shellScript = """
            |#!/bin/sh
            |echo ${escapeUnixShellArgument(prompt)}
            |echo
            |sudo -- $escapedEnv$escapedCommand
            |STATUS=$?
            |echo
            |read -p "Press Enter to close this window..." TEMP
            |exit ${'$'}STATUS
        """.trimMargin()

        val script = createTempExecutableScript("sudo", ".sh", shellScript)
        val terminalCommand = getTerminalCommand(
            ProtoDebuggerBundle.message("proto.terminal.title.install"),
            script.absolutePath
        )

        return GeneralCommandLine(terminalCommand)
    }

    private fun buildEscapedEnvCommand(wrappedCommand: GeneralCommandLine): String {
        val args = envCommandArgs(wrappedCommand)
        return if (args.isEmpty()) ""
        else "env ${args.joinToString(" ") { escapeUnixShellArgument(it) }} "
    }
}

// ============================================================================
// Unix/Linux 权限检查
// ============================================================================

private object UnixPrivilegeChecker {
    fun isSuperUser(): Boolean = LibC.INSTANCE.geteuid() == 0
}

// ============================================================================
// Shell 参数转义
// ============================================================================

/**
 * 转义 Unix Shell 参数，防止命令注入
 */
fun escapeUnixShellArgument(arg: String): String =
    "'${arg.replace("'", "'\"'\"'")}'"

/**
 * 转义 AppleScript 参数
 */
@NlsSafe
internal fun escapeAppleScriptArgument(arg: String): String =
    MacOsSudoBuilder.escapeAppleScriptArgument(arg)

// ============================================================================
// 终端应用程序支持
// ============================================================================

/**
 * 检查系统是否有可用的终端应用
 */
fun hasTerminalApp(): Boolean = when {
    SystemInfoRt.isWindows || SystemInfoRt.isMac -> true
    else -> SystemUtilities.run {
        hasKdeTerminal.get() == true ||
                hasGnomeTerminal.get() == true ||
                hasUrxvt.get() == true ||
                hasXTerm.get() == true
    }
}

/**
 * 获取 Windows Shell 名称
 */
val windowsShellName: String
    get() = CommandLineUtil.getWinShellName()

/**
 * 获取 macOS osascript 路径
 */
val osascriptPath: String
    @NlsSafe
    get() = MacOsSudoBuilder.osascriptPath

/**
 * 根据平台构建终端命令
 */
@NlsSafe
fun getTerminalCommand(
    @Nls(capitalization = Nls.Capitalization.Title) title: String?,
    command: String
): List<String> = when {
    SystemInfoRt.isWindows -> buildWindowsTerminalCommand(title, command)
    SystemInfoRt.isMac -> buildMacTerminalCommand(title, command)
    else -> buildLinuxTerminalCommand(title, command)
}

private fun buildWindowsTerminalCommand(title: String?, command: String) = listOf(
    windowsShellName,
    "/c",
    "start",
    GeneralCommandLine.inescapableQuote(title?.replace('"', '\'') ?: ""),
    command
)

private fun buildMacTerminalCommand(title: String?, command: String): List<String> {
    val baseScript = "\"clear ; exec \" & ${escapeAppleScriptArgument(command)}"
    val scriptWithTitle = if (title != null) {
        "\"echo -n \" & ${escapeAppleScriptArgument("\\0033]0;$title\\007")} & \" ; \" & $baseScript"
    } else baseScript

    val appleScript = """
        |tell application "Terminal"
        |  activate
        |  do script $scriptWithTitle
        |end tell
    """.trimMargin()

    return listOf(osascriptPath, "-e", appleScript)
}

private fun buildLinuxTerminalCommand(title: String?, command: String): List<String> {
    return SystemUtilities.run {
        when {
            hasKdeTerminal.get() == true -> buildKonsoleCommand(title, command)
            hasGnomeTerminal.get() == true -> buildGnomeTerminalCommand(title, command)
            hasUrxvt.get() == true -> buildUrxvtCommand(title, command)
            hasXTerm.get() == true -> buildXTermCommand(title, command)
            else -> throw UnsupportedOperationException(
                "不支持的操作系统/桌面环境: ${SystemInfoRt.OS_NAME}/${System.getenv("XDG_CURRENT_DESKTOP")}"
            )
        }
    }
}

private fun buildKonsoleCommand(title: String?, command: String) =
    if (title != null) listOf("konsole", "-p", "tabtitle=\"${title.replace('"', '\'')}\"", "-e", command)
    else listOf("konsole", "-e", command)

private fun buildGnomeTerminalCommand(title: String?, command: String) =
    if (title != null) listOf("gnome-terminal", "-t", title, "-x", command)
    else listOf("gnome-terminal", "-x", command)

private fun buildUrxvtCommand(title: String?, command: String) =
    if (title != null) listOf("urxvt", "-title", title, "-e", command)
    else listOf("urxvt", "-e", command)

private fun buildXTermCommand(title: String?, command: String) =
    if (title != null) listOf("xterm", "-T", title, "-e", command)
    else listOf("xterm", "-e", command)

// ============================================================================
// 环境变量处理
// ============================================================================

/**
 * 构建 env 命令，用于在 sudo 中传递环境变量
 */
fun envCommand(commandLine: GeneralCommandLine): List<String> =
    envCommandArgs(commandLine).let { args ->
        if (args.isEmpty()) emptyList() else listOf("env") + args
    }

/**
 * 获取环境变量参数列表
 * sudo 出于安全原因不会传递父进程环境变量，所以只传递显式配置的环境变量
 */
internal fun envCommandArgs(commandLine: GeneralCommandLine): List<String> =
    commandLine.environment.map { (key, value) -> "$key=$value" }

/**
 * 为构建路径添加环境变量（主要用于 macOS 动态库路径）
 */
fun appendBuildPathVars(env: MutableMap<String, String>, buildPath: String?): Map<String, String> {
    appendSearchPath(env, "DYLD_LIBRARY_PATH", buildPath)
    appendSearchPath(env, "DYLD_FRAMEWORK_PATH", buildPath)
    appendSearchPath(env, "__XPC_DYLD_LIBRARY_PATH", buildPath)
    appendSearchPath(env, "__XPC_DYLD_FRAMEWORK_PATH", buildPath)
    return env
}

/**
 * 向搜索路径添加新路径
 */
fun appendSearchPath(env: MutableMap<String, String>, paramName: String, appendPath: String?) {
    if (appendPath != null) {
        env[paramName] = env[paramName]?.let { "$it${File.pathSeparator}$appendPath" } ?: appendPath
    }
}

/**
 * 如果环境变量不存在则设置
 */
fun setIfAbsent(env: MutableMap<String, String>, paramName: String, value: String?) {
    if (value != null && !env.containsKey(paramName)) {
        env[paramName] = value
    }
}

// ============================================================================
// 文件与脚本创建
// ============================================================================

/**
 * 创建临时可执行脚本
 */
@Throws(IOException::class, ExecutionException::class)
fun createTempExecutableScript(
    @NlsSafe prefix: String,
    @NlsSafe suffix: String,
    @NlsSafe content: String
): File {
    val tempDir = File(PathManager.getTempPath())
    val tempFile = FileUtil.createTempFile(tempDir, prefix, suffix, true, true)

    FileUtil.writeToFile(tempFile, content.toByteArray(Charsets.UTF_8))

    if (!tempFile.setExecutable(true, true)) {
        throw ExecutionException(
            ProtoDebuggerBundle.message("proto.dialog.message.failed.to.make.temp.file.executable", tempFile)
        )
    }

    return tempFile
}

// ============================================================================
// 文本格式化工具
// ============================================================================

private const val ERROR_MESSAGE_WRAP_LENGTH = 80

/**
 * 包装错误输出，使其符合默认的行宽限制
 */
fun wrapErrorOutput(str: String): String =
    wrapErrorOutput(str, ERROR_MESSAGE_WRAP_LENGTH)

/**
 * 包装错误输出到指定行宽
 */
fun wrapErrorOutput(str: String, wrapLen: Int): String {
    val lines = StringUtil.splitByLinesKeepSeparators(str)
    return buildString {
        lines.forEachIndexed { index, line ->
            val words = line.split(Regex("[ \n\r]"))
            var currentLineLength = 0

            words.forEachIndexed { wordIndex, word ->
                val wordLength = word.length

                when {
                    wordIndex == 0 -> {
                        append(word)
                        currentLineLength = wordLength
                    }

                    currentLineLength + wordLength + 1 > wrapLen -> {
                        append('\n')
                        append(word)
                        currentLineLength = wordLength
                    }

                    else -> {
                        append(' ')
                        append(word)
                        currentLineLength += wordLength + 1
                    }
                }
            }

            if (index < lines.size - 1) {
                append('\n')
            }
        }
    }
}

// ============================================================================
// 路径可执行文件检查器
// ============================================================================

/**
 * 用于延迟检查系统 PATH 中是否存在可执行文件
 */
object PathExecLazyValue {
    fun create(name: @NlsSafe String): Supplier<Boolean?> {
        require(!Strings.containsAnyChar(name, "/\\")) { "无效的可执行文件名: $name" }

        return SynchronizedClearableLazy {
            val path = EnvironmentUtil.getValue("PATH") ?: return@SynchronizedClearableLazy false

            StringUtil.tokenize(path, File.pathSeparator)
                .any { dir -> File(dir, name).canExecute() }
        }
    }
}