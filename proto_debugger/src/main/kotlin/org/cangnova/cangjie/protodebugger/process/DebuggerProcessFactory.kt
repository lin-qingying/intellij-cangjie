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

package org.cangnova.cangjie.protodebugger.process

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.system.CpuArch
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.path.getBinFile
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType.*
import org.cangnova.cangjie.protodebugger.settings.DebuggerSettings
import org.cangnova.cangjie.protodebugger.util.appendSearchPath
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * 调试器进程工厂
 *
 * 职责：
 * - 创建调试器前端进程
 * - 构建命令行参数
 * - 配置跨平台环境变量
 */
object DebuggerProcessFactory {
    private var debugModeEnabled = false
    private val VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+(?:\\.\\d+)*).*")

    val defaultArchitecture: ArchitectureType
        get() = if (CpuArch.isArm64()) ARM64 else X86_64

    // ==================== 公共 API ====================




    /**
     * 创建调试器命令行
     * @param port 调试器监听端口
     * @param arch 架构类型
     */
    fun createDriverCommandLine(port: Int, arch: ArchitectureType = defaultArchitecture): GeneralCommandLine {
        val frameworkFile = getFrameworkFile(arch)
        val frontendFile = getFrontendFile(arch)

        validateFiles(frameworkFile, frontendFile)

        return GeneralCommandLine().apply {
            exePath = frontendFile.absolutePath
            workDirectory = frontendFile.parentFile

            setupEnvironment(frameworkFile, frontendFile)
            setupCommonParameters()

            // 添加端口参数
            addParameter(port.toString())
            if (debugModeEnabled) addParameter("--debug")
        }
    }

    /**
     * 创建调试进程处理器
     */
    fun createDebugProcessHandler(
        commandLine: GeneralCommandLine,
        isElevated: Boolean = false,
        hostMachine: HostMachine = LocalHost
    ): BaseProcessHandler<*> {
        return when {
            !hostMachine.isRemote && !isElevated -> createLocalProcessHandler(commandLine)
            else -> hostMachine.createProcessBuilder()
                .withElevated(isElevated)
                .withRunDebugEnvSetup(true)
                .build(commandLine)
        }
    }

    // ==================== 内部实现 ====================

    private fun GeneralCommandLine.setupEnvironment(framework: File, frontend: File) {
        when {
            SystemInfo.isLinux -> environment["LD_LIBRARY_PATH"] = framework.parent
            SystemInfo.isMac -> setupMacEnvironment(framework)
            SystemInfo.isWindows -> setupWindowsEnvironment(framework, frontend)
        }

        // ASLR 配置
        if (DebuggerSettings.getInstance().isDisableASLR()) {
            environment["LLDB_LAUNCH_FLAG_DISABLE_ASLR"] = "1"

            environment["LLDB_LAUNCH_INFERIORS_WITHOUT_CONSOLE"] = "1"
        }
    }

    private fun GeneralCommandLine.setupMacEnvironment(framework: File) {
        environment["DYLD_FRAMEWORK_PATH"] = framework.parent
        environment["NSUnbufferedIO"] = "YES"
    }

    private fun GeneralCommandLine.setupWindowsEnvironment(framework: File, frontend: File) {
        val frameworkDir = framework.parentFile
        val frontendDir = frontend.parentFile

        appendSearchPath(environment, "PATH", frameworkDir.path)

        if (!FileUtil.filesEqual(frontendDir, frameworkDir)) {
            appendSearchPath(environment, "PATH", frontendDir.path)
        }

        parentEnvironment["PATH"]?.let {
            appendSearchPath(environment, "PATH", it)
        }


    }

    private fun GeneralCommandLine.setupCommonParameters() {
        charset = StandardCharsets.UTF_8
    }

    private fun createLocalProcessHandler(commandLine: GeneralCommandLine) =
        object : OSProcessHandler(commandLine) {
            override fun readerOptions() = BaseOutputReader.Options.BLOCKING
        }.apply {
            setShouldDestroyProcessRecursively(false)

        }

    private fun validateFiles(framework: File, frontend: File) {
        if (!framework.exists()) {
            throw ExecutionException(
                DebuggerBundle.message("error.lldb.library.not.found", arrayOf(framework))
            )
        }
        if (!frontend.exists()) {
            throw ExecutionException(
                DebuggerBundle.message("error.lldbfrontend.not.found", arrayOf(frontend.absolutePath))
            )
        }
    }

    // ==================== 文件路径解析 ====================

    private fun getBundledFrameworkFile(arch: ArchitectureType): File {
        val relativePath = getPathForArch(
            arch, when {
                SystemInfo.isMac -> "LLDB.framework"
                SystemInfo.isWindows -> "bin/liblldb.dll"
                else -> "lib/liblldb.so"
            }
        )

        val resourcePath = if (SystemInfo.isMac) {
            getPathForArch(arch, "LLDB.framework/Resources")
        } else {
            getPathForArch(arch, "bin")
        }

        return getBinFile(relativePath, resourcePath)
    }

    private fun getFrontendFile(arch: ArchitectureType): File {
        // TODO: 移除硬编码，从配置获取
        return File("D:\\code\\cangjie\\workspace\\cangjie_debugger\\output\\CangJieLLDBFrontend.exe")

        /*
        val relativePath = getPathForArch(
            arch, when {
                SystemInfo.isMac -> "CangJieLLDBFrontend"
                SystemInfo.isWindows -> "bin/CangJieLLDBFrontend.exe"
                else -> "bin/CangJieLLDBFrontend"
            }
        )
        return getBinFile(relativePath, null)
        */
    }

    private fun getFrameworkFile(arch: ArchitectureType): File {
        // TODO: 移除硬编码，从配置获取
        return File("D:\\code\\cangjie\\workspace\\cangjie_debugger\\output\\liblldb.dll")

        // return getBundledFrameworkFile(arch)
    }

    // ==================== 架构处理 ====================

    private fun getArchDirName(arch: ArchitectureType) = when (arch) {
        I386 -> "x86"
        X86_64 -> "x64"
        ARM64 -> "aarch64"
        else -> null
    }

    private fun getPathForArch(arch: ArchitectureType, path: String): String {
        val archDir = getArchDirName(arch)
            ?: getArchDirName(ArchitectureType.forVmCpuArch(CpuArch.CURRENT))
            ?: throw UnsupportedOperationException(
                "Unable to locate bundled LLDB for architecture $arch"
            )
        return "$archDir/$path"
    }

}