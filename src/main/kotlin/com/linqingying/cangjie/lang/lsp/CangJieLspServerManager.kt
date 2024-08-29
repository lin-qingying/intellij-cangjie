package com.linqingying.cangjie.lang.lsp


import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings
import com.linqingying.cangjie.ide.run.cjpm.toolchain
import com.linqingying.lsp.impl.LspServerManagerImpl
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

enum class LspServerType {
    LSPSERVER, LSPMACROSERVER
}


@Deprecated("use CangJieLspServerService")
object CangJieLspServerManager {

    //user  .cangjielspserver
    private val LSPSERVERPATH =
        System.getProperty("user.home") + "/.cangjie/lsp"

    private val LSPSERVERFILENAME = "LSPServer" + (if (SystemInfo.isWindows) ".exe" else "")

    private val LSPMacroServerName = "LSPMacroServer" + (if (SystemInfo.isWindows) ".exe" else "")

    //    override fun createCommandLine() = GeneralCommandLine("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe", "src")
//    val binaryPath: Path = Paths.get("$LSPSERVERPATH/$LSPSERVERFILENAME")


    fun getBinaryPath(LspServerType: LspServerType): Path {

        return Paths.get(
            when (LspServerType) {
                com.linqingying.cangjie.lang.lsp.LspServerType.LSPSERVER -> "$LSPSERVERPATH/$LSPSERVERFILENAME"
                com.linqingying.cangjie.lang.lsp.LspServerType.LSPMACROSERVER -> "$LSPSERVERPATH/$LSPMacroServerName"
            }
        )
    }


    /**
     * 重新启动lspserver
     */
    fun restartLspServer(project: Project) {

        LspServerManagerImpl.getInstanceImpl(project)
            .stopAndRestartIfNeeded(CangJieLspServerSupportProvider::class.java)
    }


    /**
     * 关闭所有的LSP服务
     */
    fun shutdownAllServers(project: Project) {

        LspServerManagerImpl.getInstanceImpl(project).stopServers(CangJieLspServerSupportProvider::class.java)
    }


    fun getCommandLine(project: Project, lspServerType: LspServerType): GeneralCommandLine {


//        关闭现有的lspserver
//        CangJieLspServerManager.shutdownAllServers()


        val toolchain = project.cangjieSettings.toolchain
        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getLspServerPath(project, lspServerType)
            if (toolchain != null) {
                setWorkDirectory(toolchain.location.systemIndependentPath)
                withEnvironment(toolchain.getEnvironment())

            } else {
                setWorkDirectory(getBinaryPath(lspServerType).parent.toAbsolutePath().toString())
            }
            addParameter("src")
            addParameter("-V")
//            withEnvironment(getWindowsPath())
        }
    }


    /**
     * 获取lspserver路径
     */
    private fun getLspServerPath(project: Project, lspServerType: LspServerType): String {
        val toolchain = project.cangjieSettings.toolchain


        if (toolchain != null) {

            val exename = when (lspServerType) {
                LspServerType.LSPSERVER -> "LSPServer"
                LspServerType.LSPMACROSERVER -> "LSPMacroServer"
            }
            if (Files.exists(Paths.get("${toolchain.location.systemIndependentPath}/tools/bin/$exename".toSystemPath()))) {
                return "${toolchain.location.systemIndependentPath}/tools/bin/$exename".toSystemPath()
            }


        }

        throw Exception("LSPServer not found")

//        如果插件版本更新，则复制一份新的
//        // 获取当前插件的版本
//        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("com.linqingying.cangjie"))?.version
//        // 获取保存的插件版本
//        val savedVersion = getSavePluginVersion()
//        // 如果当前版本和保存的版本不一致，则重新复制一份
//        if (currentVersion != savedVersion) {
//            reCopyLspServerToPath()
//            savePluginVersion()
//        }
//
//
//// 如果二进制文件不存在，则将其复制到固定位置
//        if (Files.notExists(binaryPath)) {
//            copyLspServerToPath()
//        }
//
//
//        return binaryPath.toAbsolutePath().toString()
//        return tempFile.absolutePath
    }


    // ... existing code ...
    fun getDefaultServerPath(project: Project): String {


        val exepath = if (SystemInfo.isWindows) "LSPServer.exe" else "LSPServer"

        val sdkHome = project.toolchain?.location.toString()

        var toolPath = project.toolchain?.toolsPath?.resolve(exepath)


        if (toolPath == null) {


            val o = if (System.getProperty("os.arch") == "arm64") "aarch64" else "x64"

            val r = when {
                System.getProperty("os.name").contains("darwin", ignoreCase = true) -> "darwin"
                SystemInfo.isLinux -> "linux"
                else -> "windows"

            }

            toolPath = Paths.get(sdkHome, r, o, exepath)

        }

        return toolPath.toString()
    }
// ... existing code ...
}


fun String.toSystemPath(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\") + ".exe"
    } else {
        this
    }
}
