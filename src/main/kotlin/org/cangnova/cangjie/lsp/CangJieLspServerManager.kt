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

package org.cangnova.cangjie.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File
import com.intellij.util.io.systemIndependentPath
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 仓颉语言 LSP 服务器管理器
 * 负责配置和启动 LSP 服务器进程
 */
@Service(Service.Level.PROJECT)
class CangJieLspServerManager(private val project: Project) {

    companion object {
        private const val LSP_DIR: String = ".cangjie/lsp"
        private const val LSP_SERVER_NAME: String = "LSPServer"
        private const val LOG_DIR: String = ".idea/log"
        private const val SDK_LSP_PATH: String = "tools/bin/LSPServer"

        fun getInstance(project: Project): CangJieLspServerManager {
            return project.getService(CangJieLspServerManager::class.java)
        }
    }

    /**
     * 获取 LSP 服务器的完整路径
     * 优先使用项目 SDK 中的 LSP 服务器
     *
     * @return LSP 服务器可执行文件的路径
     * @throws IllegalStateException 如果未配置项目 SDK 或找不到 LSP 服务器
     */
    private fun getLspServerPath(): String {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: throw IllegalStateException("项目未配置 SDK,请先配置仓颉 SDK")

        val sdkLspPath = Paths.get(sdk.homePath.systemIndependentPath, SDK_LSP_PATH)
        val systemSpecificPath = sdkLspPath.toSystemSpecificPath()

        return if (Files.exists(systemSpecificPath)) {
            systemSpecificPath.toString()
        } else {
            // 回退到用户目录下的 LSP 服务器
            val fallbackPath = getUserHomeLspPath()
            if (Files.exists(fallbackPath)) {
                fallbackPath.toString()
            } else {
                throw IllegalStateException(
                    "找不到 LSP 服务器。已检查:\n" +
                            "1. SDK 路径: $systemSpecificPath\n" +
                            "2. 用户目录: $fallbackPath"
                )
            }
        }
    }

    /**
     * 获取用户目录下的 LSP 服务器路径
     */
    private fun getUserHomeLspPath(): Path {
        val fileName = if (SystemInfo.isWindows) "$LSP_SERVER_NAME.exe" else LSP_SERVER_NAME
        return Paths.get(System.getProperty("user.home"), LSP_DIR, fileName)
    }

    /**
     * 创建日志目录
     */
    private fun ensureLogDirectory(): Path {
        val logPath = Paths.get(project.basePath, LOG_DIR)
        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath)
        }
        return logPath
    }

    /**
     * 获取工作目录
     * 优先使用 SDK 主目录,否则使用 LSP 服务器所在目录
     */
    private fun getWorkingDirectory(): String {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        return sdk?.homePath?.systemIndependentPath
            ?: getUserHomeLspPath().parent.toAbsolutePath().toString()
    }

    /**
     * 构建 LSP 服务器命令行
     *
     * @return 配置好的命令行对象
     */
    fun getCommandLine(): GeneralCommandLine {
        val logPath = ensureLogDirectory()
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)

            // 设置可执行文件路径
            exePath = getLspServerPath()

            // 设置工作目录
            setWorkDirectory(this@CangJieLspServerManager.getWorkingDirectory())

            // 添加 SDK 环境变量
            sdk?.let { withEnvironment(it.getEnvironment()) }

            // 配置参数
            addParameter("src")
            addParameter("--enable-log=true")
            addParameter("--log-path=${logPath.toAbsolutePath()}")
        }
    }

    /**
     * 获取 IDE 运行时的 java 可执行文件路径（通常为 JBR）
     */
    fun getJavaExecutableFromIde(): String {
        val javaHome = System.getProperty("java.home")
        val javaBin = if (SystemInfo.isWindows) "java.exe" else "java"
        return File(javaHome, "bin${File.separator}$javaBin").absolutePath
    }

    /**
     * 获取项目 SDK 中的 java 可执行文件路径（如果项目配置了 SDK）
     */
    fun getJavaExecutableFromProjectSdk(): String? {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null
        val javaBin = if (SystemInfo.isWindows) "java.exe" else "java"
        return Paths.get(sdk.homePath.systemIndependentPath, "bin", javaBin).toString()
    }

    /**
     * 基于安装目录中的 jar 包直接启动 LSP
     * 使用 IDE 运行时的 java 启动
     * 设置 UTF-8 编码，classpath 和主类
     */
    fun getCommandLineForJar(): GeneralCommandLine {
        val logPath = ensureLogDirectory()

        // 使用 IDE 运行时的 java
        val javaExe = getJavaExecutableFromIde()

        // 固定的安装目录 lib 路径
        val libDir = "D:\\code\\intellij\\cangjie\\lsp\\build\\install\\lsp\\lib\\*"

        // 获取项目 SDK 以添加环境变量
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)

            exePath = javaExe

            // 设置系统属性
            addParameter("-Dfile.encoding=UTF-8")
            addParameter("-Dstdout.encoding=UTF-8")
            addParameter("-Dstderr.encoding=UTF-8")

            // 设置 classpath
            addParameter("-cp")
            addParameter(libDir)

            // 主类
            addParameter("org.cangnova.cangjie.lsp.CangjieLspServerLauncherKt")

            // 程序参数
            addParameter("--enable-log=true")
            addParameter("--log-path=${logPath.toAbsolutePath()}")

            // 工作目录
            setWorkDirectory(this@CangJieLspServerManager.getWorkingDirectory())

            // 设置 JAVA_HOME 环境变量为 IDE 运行时
            withEnvironment("JAVA_HOME", System.getProperty("java.home"))

            // 添加项目 SDK 的环境变量
            sdk?.let { withEnvironment(it.getEnvironment()) }
        }
    }
}

/**
 * 扩展函数:将路径转换为系统特定格式
 */
private fun Path.toSystemSpecificPath(): Path {
    return if (SystemInfo.isWindows) {
        Paths.get(this.toString().replace("/", "\\") + ".exe")
    } else {
        this
    }
}

/**
 * 扩展函数:将字符串路径转换为系统特定格式
 */
fun String.toSystemPath(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\") + ".exe"
    } else {
        this
    }
}

/**
 * 扩展函数:替换路径分隔符为系统特定格式
 */
fun String.replacePathBySystem(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\")
    } else {
        this
    }
}