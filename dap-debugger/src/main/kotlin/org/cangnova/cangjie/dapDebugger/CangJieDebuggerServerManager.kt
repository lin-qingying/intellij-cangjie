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

package org.cangnova.cangjie.dapDebugger


import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.utils.getSavePluginVersion
import org.cangnova.cangjie.utils.savePluginVersion
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

object CangJieDebuggerServerManager {
    //user  .cangjielspserver
    val DAPSERVERPATH = System.getProperty("user.home") + "/.cangjie/debugger"
//    val DAPSERVERFILENAME = "dap_server.exe"

    val DAPSERVERFILENAME = "dap_server" + (if (SystemInfo.isWindows) ".exe" else "")

    //    override fun createCommandLine() = GeneralCommandLine("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe", "src")
    val binaryPath = Paths.get("$DAPSERVERPATH/$DAPSERVERFILENAME")


    /**
     * 调试端口
     */
    val DEBUGPORT = 58920

    /**
     * 日志路径
     */
    val LOGPATH = System.getProperty("user.home") + "/.cangjie/debugger/logs/server"

    /**
     * 调试类型
     */
    val DEBUGGERTYPE = "lldbapi"


    val DEFUALTLIBLLDBPATH = "/third_party/llvm/lldb/lib/"

    /**
     * 获取调试服务路径
     */
    fun getDebugServerPath(): String {


//        如果插件版本更新，则复制一份新的
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
//        return tempFile.absolutePath
    }


    //    删除日志文件
    fun removeLogFiles() {
        val logPath = Paths.get(LOGPATH)
//        删除该目录下的所有文件
        if (Files.exists(logPath)) {
            Files.list(logPath).forEach {
                Files.delete(it)
            }
        }

    }

    //    获取调试服务器路径
    fun getCommandLine(project:Project): GeneralCommandLine {


        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getDebugServerPath()

            setWorkDirectory(project.basePath)
            addParameter("--port=$DEBUGPORT")
//            addParameter("--logpath=$LOGPATH".toSystemPath())
            addParameter("--logpath=${project.basePath}/.idea/log")
            addParameter("--debuggertype=$DEBUGGERTYPE")
            sdk?.getEnvironment()?.let { environment.putAll(it) }
//            environment["LD_LIBRARY_PATH"] =
//                (cangjieSettings.toolchain?.location?.systemIndependentPath + DEFUALTLIBLLDBPATH).toSystemPath()
            environment["LD_LIBRARY_PATH"] =
                (sdk?.homePath?.systemIndependentPath + DEFUALTLIBLLDBPATH)
//            //                        TODO runtime路径需要判读系统

        }
    }

    /**
     * 重新复制调试服务器到目录
     */
    fun reCopyDapServerToPath() {
        if (Files.exists(binaryPath)) {
            Files.delete(binaryPath)
        }
        copyDapServerToPath()
    }

    /**
     * 复制调试服务器到目录
     */
    fun copyDapServerToPath() {

        val classLoader = this::class.java.classLoader

        val dapserverPath = if (SystemInfo.isWindows) {
            "debugger/dap_server.exe"
        } else {
            "debugger/dap_server-linux_x64"
        }

        val resource = classLoader.getResource(dapserverPath)


//创建目录
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
