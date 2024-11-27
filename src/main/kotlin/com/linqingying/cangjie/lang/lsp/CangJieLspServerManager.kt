/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.lang.lsp


import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings

import com.linqingying.cangjie.utils.getSavePluginVersion
import com.linqingying.cangjie.utils.savePluginVersion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import com.linqingying.lsp.impl.LspServerManagerImpl
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object CangJieLspServerManager {

    //user  .cangjielspserver
    private val LSPSERVERPATH =
        System.getProperty("user.home") + "/.cangjie/lsp"

    private val LSPSERVERFILENAME = "LSPServer" + (if (SystemInfo.isWindows) ".exe" else "")

    //    override fun createCommandLine() = GeneralCommandLine("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe", "src")
    val binaryPath: Path = Paths.get("$LSPSERVERPATH/$LSPSERVERFILENAME")


    /**
     * 复制lspserver到目录
     */
    fun copyLspServerToPath() {


        val classLoader = this::class.java.classLoader

        val lspserverPath = if (SystemInfo.isWindows) {
            "lsp/LSPServer.exe"
        } else {
            "lsp/LSPServer"
        }

        val resource = classLoader.getResource(lspserverPath)


//创建目录
        if (Files.notExists(Paths.get(LSPSERVERPATH))) {
            Files.createDirectories(Paths.get(LSPSERVERPATH))
        }
        resource?.openStream()?.use { input ->
            FileOutputStream(binaryPath.toFile()).use { output ->
                input.copyTo(output)
            }
        }


    }

    /**
     * 重新复制lspserver到目录
     */
    fun reCopyLspServerToPath() {
        if (Files.exists(binaryPath)) {
            Files.delete(binaryPath)
        }
        copyLspServerToPath()
    }


    /**
     * 重新启动lspserver
     */
    fun restartLspServer(project: Project) {

//        LspServerManagerImpl.getInstanceImpl(project)
//            .stopAndRestartIfNeeded(CangJieLspServerSupportProvider::class.java)
    }


//    /**
//     * 关闭所有的LSP服务
//     */
//    fun shutdownAllServers(project: Project) {
//
//        LspServerManagerImpl.getInstanceImpl(project).stopServers(CangJieLspServerSupportProvider::class.java)
//    }


    fun getCommandLine(project: Project): GeneralCommandLine {


//        关闭现有的lspserver
//        CangJieLspServerManager.shutdownAllServers()


        val toolchain = project.cangjieSettings.toolchain
        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getLspServerPath(project)
            if (toolchain != null) {
                setWorkDirectory(toolchain.location.systemIndependentPath)
                withEnvironment(toolchain.getEnvironment())
//                setWorkDirectory(binaryPath.parent.toAbsolutePath().toString())

            } else {
                setWorkDirectory(binaryPath.parent.toAbsolutePath().toString())
            }
            addParameter("src")
            addParameter("--log-path=${project.basePath}/.idea/log")
//            addParameter("--log-path=${binaryPath.parent.toAbsolutePath().toString()}")
//            withEnvironment(getWindowsPath())
        }
    }


    /**
     * 获取lspserver路径
     */
    private fun getLspServerPath(project: Project): String {
        val toolchain = project.cangjieSettings.toolchain


        if (toolchain != null) {
            if (Files.exists(Paths.get("${toolchain.location.systemIndependentPath}/tools/bin/LSPServer".toSystemPath()))) {
                return "${toolchain.location.systemIndependentPath}/tools/bin/LSPServer".toSystemPath()
            }
        }

        throw Exception("LSPServer not found")

//        如果插件版本更新，则复制一份新的
        // 获取当前插件的版本
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("com.linqingying.cangjie"))?.version
        // 获取保存的插件版本
        val savedVersion = getSavePluginVersion()
        // 如果当前版本和保存的版本不一致，则重新复制一份
        if (currentVersion != savedVersion) {
            reCopyLspServerToPath()
            savePluginVersion()
        }


// 如果二进制文件不存在，则将其复制到固定位置
        if (Files.notExists(binaryPath)) {
            copyLspServerToPath()
        }


        return binaryPath.toAbsolutePath().toString()
//        return tempFile.absolutePath
    }


}


fun String.replacePathBySystem(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\")
    } else {
        this
    }
}

fun String.toSystemPath(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\") + ".exe"
    } else {
        this
    }
}
