package com.debugger.runconfig

import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.utils.getSavePluginVersion
import com.huawei.cangjie.utils.savePluginVersion
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import com.linqingying.lsp.api.LspProcessHandler
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
    val DEBUGPORT = 65500

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
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("com.huawei.cangjie"))?.version
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

    //    获取调试服务器进程
    fun getDebugServerProcess(): BaseProcessHandler<*> {

//        删除日志文件
//        removeLogFiles()

        val processHandler = LspProcessHandler(getCommandLine())
        processHandler.startNotify()
        return processHandler
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
    fun getCommandLine(): GeneralCommandLine {

        val project = ProjectManager.getInstance().openProjects[0]

        val cangjieSettings = project.cangjieSettings


        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getDebugServerPath()

            setWorkDirectory(project.basePath)
            addParameter("--port=$DEBUGPORT")
            addParameter("--logpath=$LOGPATH".toSystemPath())
            addParameter("--debuggertype=$DEBUGGERTYPE")
            cangjieSettings.toolchain?.getEnvironment()?.let { environment.putAll(it) }

            environment["LD_LIBRARY_PATH"] =
                (cangjieSettings.toolchain?.sdkHome?.systemIndependentPath + DEFUALTLIBLLDBPATH).toSystemPath()
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
            "debugger/dap_server"
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


fun String.toSystemPath(): String {
    return if (SystemInfo.isWindows) {
        this.replace("/", "\\")
    } else {
        this
    }

}
