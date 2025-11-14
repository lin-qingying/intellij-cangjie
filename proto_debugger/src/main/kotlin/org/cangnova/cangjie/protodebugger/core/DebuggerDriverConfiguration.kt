package org.cangnova.cangjie.protodebugger.core

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.util.Expirable
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.system.CpuArch
import org.cangnova.cangjie.messages.DebuggerBundle
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.path.getBinFile
import org.cangnova.cangjie.protodebugger.process.HostMachine
import org.cangnova.cangjie.protodebugger.process.LocalHost
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType
import org.cangnova.cangjie.protodebugger.settings.ArchitectureType.*
import org.cangnova.cangjie.protodebugger.settings.DebuggerSettings
import org.cangnova.cangjie.protodebugger.symbol.NtSymbolSettings

import org.cangnova.cangjie.protodebugger.util.ToolVersion
import org.cangnova.cangjie.protodebugger.util.appendSearchPath
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * 调试器驱动配置
 *
 * 核心职责：
 * - 管理调试器命令行构建
 * - 处理跨平台和多架构支持
 * - 配置调试器环境和路径
 */
class DebuggerDriverConfiguration : UserDataHolderBase() {

    // ==================== 公共 API ====================

    val driverName: String get() = "LLDB"
    val isAttachSupported: Boolean get() = true
    val designator: String get() = javaClass.name

    fun isStaticVarsLoadingEnabled() = Registry.`is`("cangjie.debugger.lldb.statics")

    fun convertToProjectModelPath(absolutePath: String?) = absolutePath ?: "null"
    fun convertToLocalPath(absolutePath: String?) = absolutePath
    fun convertToEnvPath(localPath: String?) = localPath

    // ==================== 驱动创建 ====================

//    /**
//     * 创建调试器驱动门面
//     */
//    fun createDriverFacade(handler: Handler, type: ArchitectureType = defaultArchitecture) =
//        DebuggerDriverFacade(handler, this, type)

    /**
     * 创建驱动命令行
     * @param facade 调试器门面
     * @param type 架构类型
     */
    fun createDriverCommandLine(facade: DebuggerDriverFacade, type: ArchitectureType = defaultArchitecture) =
        createFrontendCommandLine(type).apply {
            addParameter(facade.port.toString())
            if (debugModeEnabled) addParameter("--debug")
        }

    /**
     * 创建调试进程处理器
     */
    fun createDebugProcessHandler(commandLine: GeneralCommandLine): BaseProcessHandler<*> {
        val host = hostMachine
        return when {
            !host.isRemote && !isElevated -> createLocalProcessHandler(commandLine)
            else -> host.createProcessBuilder()
                .withElevated(isElevated)
                .withRunDebugEnvSetup(true)
                .build(commandLine)
        }
    }

    /**
     * 创建求值上下文
     * @param facade 调试器门面
     */
    fun createEvaluationContext(
        facade: DebuggerDriverFacade,
        expirable: Expirable?,
        thread: LLThread,
        frame: LLFrame,
        holder: UserDataHolderEx
    ) = EvaluationContext(facade, expirable, thread, frame, holder)

    // ==================== 配置属性 ====================

    val isElevated: Boolean get() = false
    val emulateTerminal: Boolean get() = true
    val isContinueAfterAttachNeeded: Boolean get() = true
    val hostMachine: HostMachine get() = LocalHost
    val useSTLRenderers: Boolean get() = true
    val disableASLR: Boolean get() = false


    // ==================== 内部实现 ====================

    private fun createFrontendCommandLine(arch: ArchitectureType): GeneralCommandLine {
        val frameworkFile = getFrameworkFile(arch)
        val frontendFile = getFrontendFile(arch)

        validateFiles(frameworkFile, frontendFile)

        return GeneralCommandLine().apply {
            exePath = frontendFile.absolutePath
            setupEnvironment(frameworkFile, frontendFile)
            setupCommonParameters()
        }
    }

    private fun GeneralCommandLine.setupEnvironment(framework: File, frontend: File) {
        when {
            SystemInfo.isLinux -> environment["LD_LIBRARY_PATH"] = framework.parent
            SystemInfo.isMac -> setupMacEnvironment(framework)
            SystemInfo.isWindows -> setupWindowsEnvironment(framework, frontend)
        }



        if (disableASLR) {
            environment["LLDB_LAUNCH_FLAG_DISABLE_ASLR"] = "1"
        }
    }

    private fun GeneralCommandLine.setupMacEnvironment(framework: File) {
        environment["DYLD_FRAMEWORK_PATH"] = framework.parent
        environment["NSUnbufferedIO"] = "YES"
    }

    private fun GeneralCommandLine.setupWindowsEnvironment(framework: File, frontend: File) {
        val frameworkDir = framework.parentFile
        val frontendDir = frontend.parentFile

        appendSearchPath(environment, "PATH", frameworkDir.path)

        if (!FileUtil.filesEqual(frontendDir, frameworkDir)) {
            appendSearchPath(environment, "PATH", frontendDir.path)
        }

        parentEnvironment["PATH"]?.let {
            appendSearchPath(environment, "PATH", it)
        }

        // NT Symbol 配置
        val settings = DebuggerSettings.getInstance().ntSymbolSettings
        if (settings.useNtSymbolServers) {
            environment["_NT_SYMBOL_PATH"] = buildNtSymbolPath(settings)
        }
    }

    private fun GeneralCommandLine.setupCommonParameters() {
        charset = StandardCharsets.UTF_8
    }

    private fun createLocalProcessHandler(commandLine: GeneralCommandLine) =
        object : OSProcessHandler(commandLine) {
            override fun readerOptions() = BaseOutputReader.Options.BLOCKING
        }.apply {
            setShouldDestroyProcessRecursively(false)
        }

    private fun validateFiles(framework: File, frontend: File) {
        if (!framework.exists()) {
            throw ExecutionException(
                DebuggerBundle.message("error.lldb.library.not.found", arrayOf(framework))
            )
        }
        if (!frontend.exists()) {
            throw ExecutionException(
                DebuggerBundle.message("error.lldbfrontend.not.found", arrayOf(frontend.absolutePath))
            )
        }
    }

    // ==================== 文件路径解析 ====================

    protected fun getBundledFrameworkFile(arch: ArchitectureType): File {
        val relativePath = getPathForArch(
            arch, when {
                SystemInfo.isMac -> "LLDB.framework"
                SystemInfo.isWindows -> "bin/liblldb.dll"
                else -> "lib/liblldb.so"
            }
        )

        val resourcePath = if (SystemInfo.isMac) {
            getPathForArch(arch, "LLDB.framework/Resources")
        } else {
            getPathForArch(arch, "bin")
        }

        return getBinFile(relativePath, resourcePath)
    }

    protected fun getFrontendFile(arch: ArchitectureType): File {

        return File("D:\\code\\cangjie\\workspace\\cangjie_debugger\\output\\CangJieLLDBFrontend.exe")

        val relativePath = getPathForArch(
            arch, when {
                SystemInfo.isMac -> "CangJieLLDBFrontend"
                SystemInfo.isWindows -> "bin/CangJieLLDBFrontend.exe"
                else -> "bin/CangJieLLDBFrontend"
            }
        )
        return getBinFile(relativePath, null)
    }

    protected fun getFrameworkFile(arch: ArchitectureType): File {

        return File("D:\\code\\cangjie\\workspace\\cangjie_debugger\\output\\liblldb.dll")

        return getBundledFrameworkFile(arch)
    }

    // ==================== 伴生对象 ====================

    companion object {
        private var debugModeEnabled = false
        private val VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+(?:\\.\\d+)*).*")

        /**
         * UserData key for STL renderers enablement
         */
        val ENABLE_STL_RENDERERS = Key.create<Boolean>("ENABLE_STL_RENDERERS")

        val defaultArchitecture: ArchitectureType
            get() = if (CpuArch.isArm64()) ARM64 else X86_64

        fun setDebugMode(enabled: Boolean) {
            debugModeEnabled = enabled
        }

        fun isDebugModeEnabled() = debugModeEnabled

        fun parseVersion(displayVersion: String): ToolVersion =
            ToolVersion.parse(displayVersion, VERSION_PATTERN)

        fun hasBundledLLDB() =
            DebuggerDriverConfiguration()
                .getBundledFrameworkFile(defaultArchitecture)
                .exists()

        // ==================== 架构处理 ====================

        private fun getArchDirName(arch: ArchitectureType) = when (arch) {
            I386 -> "x86"
            X86_64 -> "x64"
            ARM64 -> "aarch64"
            else -> null
        }

        private fun getPathForArch(arch: ArchitectureType, path: String): String {
            val archDir = getArchDirName(arch)
                ?: getArchDirName(ArchitectureType.forVmCpuArch(CpuArch.CURRENT))
                ?: throw UnsupportedOperationException(
                    "Unable to locate bundled LLDB for architecture $arch"
                )
            return "$archDir/$path"
        }

        // ==================== NT Symbol 路径构建 ====================

        private fun buildNtSymbolPath(settings: NtSymbolSettings) = buildString {
            val cachePath = settings.ntSymbolCache.trim()
                .ifEmpty { NtSymbolSettings.getDefaultSymbolCachePath() }

            // 服务器路径
            settings.ntSymbolServers
                .filter { it.isEnabled && it.url.isNotBlank() }
                .forEach { append("srv*$cachePath*${it.url.trim()};") }

            // 缓存路径
            append("cache*$cachePath;")

            // 本地路径
            settings.ntSymbolPaths
                .filter { it.isEnabled && it.url.isNotBlank() }
                .forEach { append("${it.url.trim()};") }
        }
    }
}