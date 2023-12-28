package com.huawei.cangjie.lang.lsp


import com.huawei.cangjie.idea.project.CangJieProjectManager
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.utils.getSavePluginVersion
import com.huawei.cangjie.utils.savePluginVersion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfo
import com.linqingying.lsp.impl.LspServerManagerImpl
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

object CangJieLspServerManager {

    //user  .cangjielspserver
    val LSPSERVERPATH =
        System.getProperty("user.home") + "/.cangjie/lsp"

    val LSPSERVERFILENAME = "LSPServer" + (if (SystemInfo.isWindows) ".exe" else "")

    //    override fun createCommandLine() = GeneralCommandLine("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe", "src")
    val binaryPath = Paths.get("$LSPSERVERPATH/$LSPSERVERFILENAME")


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
        if (resource != null) {
            resource.openStream().use { input ->
                FileOutputStream(binaryPath.toFile()).use { output ->
                    input?.copyTo(output) ?: throw Exception("LspServer not found")
                }
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
    fun restartLspServer() {
        val project = CangJieProjectManager.getCurrentProject()
        LspServerManagerImpl.getInstanceImpl(project)
            .stopAndRestartIfNeeded(CangJieLspServerSupportProvider::class.java)
    }


    /**
     * 关闭所有的LSP服务
     */
    fun shutdownAllServers() {
        val project = CangJieProjectManager.getCurrentProject()
        LspServerManagerImpl.getInstanceImpl(project).stopServers(CangJieLspServerSupportProvider::class.java)
    }

 

    fun getCommandLine(): GeneralCommandLine {


//        关闭现有的lspserver
//        CangJieLspServerManager.shutdownAllServers()


        //获取项目使用的sdk
        val sdk = CangJieSdkManager.getProjectSdk()

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getLspServerPath()
            if (sdk != null) {
                setWorkDirectory(sdk.homePath)
            } else {
                setWorkDirectory(binaryPath.parent.toAbsolutePath().toString())
            }
            addParameter("src")
            addParameter("-V")
//            withEnvironment(getWindowsPath())
        }
    }





    /**
     * 获取lspserver路径
     */
    private fun getLspServerPath(): String {

        val sdk = CangJieSdkManager.getProjectSdk()
        if (sdk != null) {
           if(Files.exists(Paths.get("${sdk.homePath}/tools/bin/LSPServer".toSystemPath()))){
               return "${sdk.homePath}/tools/bin/LSPServer".toSystemPath()
           }
        }

//        如果插件版本更新，则复制一份新的
        // 获取当前插件的版本
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("com.huawei.cangjie"))?.version
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


fun String.toSystemPath(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\") + ".exe"
    }else{
        this
    }
}