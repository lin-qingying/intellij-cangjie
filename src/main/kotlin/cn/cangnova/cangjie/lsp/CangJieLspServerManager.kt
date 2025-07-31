package cn.cangnova.cangjie.lsp

 

import cn.cangnova.cangjie.ide.project.settings.cangjieSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath

import cn.cangnova.cangjie.utils.getSavePluginVersion
import cn.cangnova.cangjie.utils.savePluginVersion
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
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("cn.cangnova.cangjie"))?.version
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
