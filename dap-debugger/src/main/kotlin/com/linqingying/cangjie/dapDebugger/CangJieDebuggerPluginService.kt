package com.linqingying.cangjie.dapDebugger

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings


import com.linqingying.cangjie.utils.getSavePluginVersion
import com.linqingying.cangjie.utils.savePluginVersion
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class CangJieDebuggerPluginService(val project: Project) : Disposable {
    override fun dispose() {}


    /**
     * 获取调试服务路径
     */
    fun getDebugServerPath(): String {


//        如果插件版本更新，则复制一份新的
        // 获取当前插件的版本
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("com.linqingying.cangjie"))?.version
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

    //    获取调试服务器路径
    fun getCommandLine(): GeneralCommandLine {


        val cangjieSettings = project.cangjieSettings


        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            exePath = getDebugServerPath()

            setWorkDirectory(project.basePath)
            addParameter("--port=$DEBUGPORT")
//            addParameter("--logpath=$LOGPATH".toSystemPath())
            addParameter("--logpath=$LOGPATH")
            addParameter("--debuggertype=$DEBUGGERTYPE")
            cangjieSettings.toolchain?.getEnvironment()?.let { environment.putAll(it) }
//            environment["LD_LIBRARY_PATH"] =
//                (cangjieSettings.toolchain?.location?.systemIndependentPath + DEFUALTLIBLLDBPATH).toSystemPath()
            environment["LD_LIBRARY_PATH"] =
                (cangjieSettings.toolchain?.location?.systemIndependentPath + DEFUALTLIBLLDBPATH)
//            //                        TODO runtime路径需要判读系统

        }
    }

    companion object {
        val DAPSERVERFILENAME = getAdapterName()

        /**
         * 获取适配器名称
         */
        fun getAdapterName(): String {

//            获取系统架构
            val arch = System.getProperty("os.arch")

//dap_server-linux_x64-cjvm
            return when {
                SystemInfo.isWindows -> "dap_server.exe"
                SystemInfo.isMac -> "dap_server-macos_$arch"
                SystemInfo.isLinux -> "dap_server-linux_$arch"
                else -> "dap_server.exe"
            }
        }

        /**
         * 复制调试服务器到目录
         */
        fun copyDapServerToPath() {

            val classLoader = this::class.java.classLoader

            val dapserverPath = "debugger/${DAPSERVERFILENAME}"


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

        val DEFUALTLIBLLDBPATH = "/third_party/llvm/lldb/lib/"

        /**
         * 日志路径
         */
        val LOGPATH = System.getProperty("user.home") + "/.cangjie/debugger/logs/server"
        val DAPSERVERPATH = System.getProperty("user.home") + "/.cangjie/debugger"

        /**
         * 调试类型
         */
        val DEBUGGERTYPE = "lldbapi"
        val binaryPath = Paths.get("$DAPSERVERPATH/$DAPSERVERFILENAME")

        /**
         * 调试端口
         */
        val DEBUGPORT = 65500


        /**
         * 重新复制调试服务器到目录
         */
        fun reCopyDapServerToPath() {
            if (Files.exists(binaryPath)) {
                Files.delete(binaryPath)
            }
            copyDapServerToPath()
        }

        @JvmStatic
        fun getInstance(project: Project): CangJieDebuggerPluginService = project.service()
    }
}
