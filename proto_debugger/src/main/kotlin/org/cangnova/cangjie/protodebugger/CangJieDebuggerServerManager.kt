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

package org.cangnova.cangjie.protodebugger

import org.cangnova.cangjie.utils.getSavePluginVersion
import org.cangnova.cangjie.utils.savePluginVersion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 仓颉调试器服务器管理器
 *
 * 该对象负责管理仓颉语言调试器的服务器进程，包括服务器的部署、配置和启动。
 * 它确保调试器服务器在正确的环境中运行，并提供版本管理和自动更新功能。
 *
 * 使用场景：
 * - 管理调试器服务器的生命周期
 * - 处理服务器二进制文件的部署和更新
 * - 配置调试器运行环境
 * - 启动调试器服务器进程
 *
 * 主要功能：
 * - 自动部署和更新调试器服务器
 * - 管理服务器的配置参数
 * - 处理不同操作系统的兼容性
 * - 提供日志文件管理
 */
object CangJieDebuggerServerManager {
    /**
     * DAP服务器存储路径
     * 用户主目录下的.cangjie/debugger文件夹，用于存储调试器服务器二进制文件
     */
    val DAPSERVERPATH = System.getProperty("user.home") + "/.cangjie/debugger"

    /**
     * DAP服务器文件名
     * 根据操作系统自动添加相应的扩展名：Windows添加.exe，Linux/macOS不添加扩展名
     */
    val DAPSERVERFILENAME = "dap_server" + (if (SystemInfo.isWindows) ".exe" else "")

    /**
     * 调试器服务器二进制文件的完整路径
     * 由DAPSERVERPATH和DAPSERVERFILENAME组合而成
     */
    val binaryPath = Paths.get("$DAPSERVERPATH/$DAPSERVERFILENAME")

    /**
     * 调试器服务器监听的端口号
     * 用于与IntelliJ IDE的DAP客户端通信
     */
    val DEBUGPORT = 58920

    /**
     * 调试器服务器日志文件存储路径
     * 用于存储调试过程中的详细日志信息
     */
    val LOGPATH = System.getProperty("user.home") + "/.cangjie/debugger/logs/server"

    /**
     * 调试器类型标识
     * 当前使用"lldbapi"表示基于LLDB API的调试器
     */
    val DEBUGGERTYPE = "lldbapi"

    /**
     * 默认的LLDB库路径
     * 相对于SDK安装路径的LLDB库文件位置
     */
    val DEFUALTLIBLLDBPATH = "/third_party/llvm/lldb/lib/"

    /**
     * 获取调试服务器路径
     *
     * 该方法负责确保调试器服务器二进制文件存在且为最新版本。
     * 它会检查插件版本是否更新，如果更新则重新部署服务器文件。
     * 如果服务器文件不存在，则会从插件资源中复制。
     *
     * 使用场景：
     * - 启动调试器前确保服务器文件可用
     * - 处理插件更新后的服务器文件更新
     * - 首次使用时自动部署调试器服务器
     *
     * @return 调试器服务器二进制文件的绝对路径
     */
    fun getDebugServerPath(): String {
        // 如果插件版本更新，则复制一份新的
        // 获取当前插件的版本
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("org.cangnova.cangjie"))?.version
        // 获取保存的插件版本
        val savedVersion = getSavePluginVersion()
        // 如果当前版本和保存的版本不一致，则重新复制一份
        if (currentVersion != savedVersion) {
            reCopyDapServerToPath()
            savePluginVersion()
        }

        // 如果二进制文件不存在，则将其复制到固定位置
        if (Files.notExists(binaryPath)) {
            copyDapServerToPath()
        }

        return binaryPath.toAbsolutePath().toString()
    }

    /**
     * 删除日志文件
     *
     * 清理调试器服务器生成的所有日志文件，用于释放磁盘空间
     * 或重新开始调试会话时清理旧的日志。
     *
     * 使用场景：
     * - 调试会话开始前清理日志
     * - 磁盘空间不足时清理旧日志
     * - 故障排查时清理损坏的日志文件
     */
    fun removeLogFiles() {
        val logPath = Paths.get(LOGPATH)
        // 删除该目录下的所有文件
        if (Files.exists(logPath)) {
            Files.list(logPath).forEach {
                Files.delete(it)
            }
        }
    }

    /**
     * 获取调试器服务器的命令行配置
     *
     * 为指定项目创建完整的调试器服务器启动命令行配置，
     * 包括端口设置、日志路径、调试器类型和环境变量等。
     *
     * 使用场景：
     * - 启动调试器服务器进程
     * - 配置调试器的运行环境
     * - 设置调试器与IDE的通信参数
     *
     * @param project IntelliJ项目对象
     * @return 配置好的GeneralCommandLine对象
     */
    fun getCommandLine(project:Project): GeneralCommandLine {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getDebugServerPath()

            setWorkDirectory(project.basePath)
            addParameter("--port=$DEBUGPORT")
            addParameter("--logpath=${project.basePath}/.idea/log")
            addParameter("--debuggertype=$DEBUGGERTYPE")
            sdk?.getEnvironment()?.let { environment.putAll(it) }
            environment["LD_LIBRARY_PATH"] =
                (sdk?.homePath?.systemIndependentPath + DEFUALTLIBLLDBPATH)
        }
    }

    /**
     * 重新复制调试服务器到目录
     *
     * 强制重新部署调试器服务器文件，先删除现有文件，
     * 然后从插件资源中重新复制。
     *
     * 使用场景：
     * - 插件版本更新后强制更新服务器
     * - 服务器文件损坏时重新部署
     * - 开发过程中测试新的服务器版本
     */
    fun reCopyDapServerToPath() {
        if (Files.exists(binaryPath)) {
            Files.delete(binaryPath)
        }
        copyDapServerToPath()
    }

    /**
     * 复制调试服务器到目录
     *
     * 从插件的资源中提取调试器服务器二进制文件并复制到
     * 用户目录中。支持不同操作系统的二进制文件选择。
     *
     * 使用场景：
     * - 首次安装插件时部署调试器服务器
     * - 服务器文件不存在时自动部署
     * - 支持跨平台的调试器服务器分发
     */
    fun copyDapServerToPath() {
        val classLoader = this::class.java.classLoader

        val dapserverPath = if (SystemInfo.isWindows) {
            "debugger/dap_server.exe"
        } else {
            "debugger/dap_server-linux_x64"
        }

        val resource = classLoader.getResource(dapserverPath)

        // 创建目录
        if (Files.notExists(Paths.get(DAPSERVERPATH))) {
            Files.createDirectories(Paths.get(DAPSERVERPATH))
        }
        resource?.openStream()?.use { input ->
            FileOutputStream(binaryPath.toFile()).use { output ->
                input.copyTo(output)
            }
        }
    }


}



//fun String.toSystemPath(): String {
//    return if (SystemInfo.isWindows) {
//        this.replace("/", "\\")
//    } else {
//        this
//    }
//
//}
