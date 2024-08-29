package com.linqingying.cangjie.lang.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings
import com.linqingying.lsp.impl.LspServerManagerImpl

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class CangJieLspServerService(val project: Project) {
    /**
     * 获取lspserver路径
     */
    private fun getLspServerPath(lspServerType: LspServerType): String {
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


    }
    fun getBinaryPath(lspServerType: LspServerType): Path {

        return Paths.get(
            when (lspServerType) {
                LspServerType.LSPSERVER -> "$LSPSERVERPATH/$LSPSERVERFILENAME"
                LspServerType.LSPMACROSERVER -> "$LSPSERVERPATH/$LSPMacroServerName"
            }
        )
    }

    fun getCommandLine(lspServerType: LspServerType): GeneralCommandLine {


//        关闭现有的lspserver
//        CangJieLspServerManager.shutdownAllServers()


        val toolchain = project.cangjieSettings.toolchain
        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getLspServerPath(lspServerType)
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
     * 重新启动lspserver
     */
    fun restartLspServer() {

        LspServerManagerImpl.getInstanceImpl(project)
            .stopAndRestartIfNeeded(CangJieLspServerSupportProvider::class.java)
    }


    companion object {
        val LOG = Logger.getInstance(CangJieLspServerService::class.java)

        //user  .cangjielspserver
        val LSPSERVERPATH =
            System.getProperty("user.home") + "/.cangjie/lsp"

        val LSPSERVERFILENAME = "LSPServer" + (if (SystemInfo.isWindows) ".exe" else "")

        val LSPMacroServerName = "LSPMacroServer" + (if (SystemInfo.isWindows) ".exe" else "")

        @JvmStatic
        fun getInstance(project: Project) = project.service<CangJieLspServerService>()
    }
}
