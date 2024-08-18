package com.linqingying.cangjie.cjpm.toolchain

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.text.SemVer
import com.linqingying.cangjie.cjpm.project.toPath
import com.linqingying.cangjie.cjpm.toolchain.flavors.CjToolchainFlavor
import com.linqingying.cangjie.cjpm.toolchain.tools.*
import com.linqingying.cangjie.cjpm.toolchain.wsl.getHomePathCandidates
import com.linqingying.utils.Config
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

//val CjToolchainBase.cjpm: Cjpm
//    get() = Cjpm(this)
//
//
//val CjToolchainBase.cjc: Cjc
//    get() = Cjc(this)

fun CjToolchainBase.cjc(): Cjc {

    if (this.cjc == null) return Cjc(this)

    return this.cjc!!
}

fun CjToolchainBase.cjpm(): Cjpm {
    if (this.cjpm == null) return Cjpm(this)

    return this.cjpm!!
}

fun CjToolchainBase.cjfmt(): CjFmt {
    if (this.cjfmt == null) return CjFmt(this)

    return this.cjfmt!!
}

abstract class CjToolchainBase(var location: Path = "".toPath()) {


    val binPath: Path
    val toolsPath: Path

    var cjpm: Cjpm? = null
    var cjc: Cjc? = null
    var cjfmt: CjFmt? = null
    val presentableLocation: String get() = pathToExecutable(Cjpm.NAME).toString()

    var project: Project? = null

    init {

        fun String.toSystemPath(): String {
            if (SystemInfo.isWindows) {
                return "$this.exe"
            } else {
                return this
            }
        }

//        判断location下是否有bin目录
        val binPath = location.resolve("bin")
        val toolsPath = location.resolve("tools")
        if (Files.exists(binPath) && Files.exists(toolsPath)) {
//            当前在主目录

            this.binPath = binPath
            this.toolsPath = toolsPath
        } else if (Files.exists(location.resolve("cjc".toSystemPath()))) {
//       当前在bin目录
            this.location = location.parent
            this.binPath = location
            this.toolsPath = this.location.resolve("tools")

        } else {

//            TODO 是否需要获取仓颉环境变量


//            this.location = Path.of(System.getenv("CANGJIE_HOME"))
//            this.binPath = this.location.resolve("bin")
//            this.toolsPath = this.location.resolve("tools")
            this.binPath = location
            this.toolsPath = location
        }

    }

    fun getSplit(): String {
        return if (SystemInfo.isWindows) ";" else ":"
    }

    fun addPath(directory: String, currentPath: String): String {
        if (!Files.exists(Paths.get(directory))) return currentPath

        val pathSeparator = getSplit()
        val entries = Files.list(Paths.get(directory)).map { it.fileName.toString() }.toList()

        var updatedPath = currentPath
        for (entry in entries) {
            val entryPath = Paths.get(directory, entry)
            if (Files.isDirectory(entryPath) && entry != ".cached") {
                updatedPath += "$entryPath$pathSeparator"
                updatedPath = addPath(entryPath.toString(), updatedPath)
            }
        }
        return updatedPath
    }


    fun getMacEnvironmentVariables(externalPath: String): Map<String, String> {
        val projectDir = project?.basePath ?: return emptyMap()

        var targetPath = Paths.get(projectDir, "target", "release").toString()
        if (!Files.exists(Paths.get(targetPath))) {
            targetPath = Paths.get(projectDir, "target", "debug").toString()
        }
        var modifiedPath = ""
        if (Files.exists(Paths.get(targetPath))) {
            modifiedPath = requirePath
            modifiedPath = addPath(targetPath, modifiedPath).dropLast(1)
        }
        val envPaths = getEnvPaths()
        val architecture = if (System.getProperty("os.arch") == "arm64") "aarch64" else "x86_64"
        var libraryPath = ""
        var pathVariable = ""
        envPaths.forEach { env ->
            if (env.contains("DYLD_LIBRARY_PATH")) {
                val parts = env.split("=")
                libraryPath += parts.last() + ":"
            }
            if (env.contains("PATH")) {
                val parts = env.split("=")
                pathVariable += parts.last() + ":"
            }
        }
        pathVariable += externalPath
        libraryPath = libraryPath.replace("\${hw_arch}", architecture)
        pathVariable = pathVariable.replace("\${hw_arch}", architecture)
        return if (System.getenv("CANGJIE_PATH") == null) {
            mapOf("DYLD_LIBRARY_PATH" to libraryPath + modifiedPath, "PATH" to pathVariable)
        } else {
            mapOf(
                "DYLD_LIBRARY_PATH" to libraryPath + modifiedPath,
                "CANGJIE_PATH" to System.getenv("CANGJIE_PATH"),
                "PATH" to pathVariable
            )
        }
    }

    // ... existing code ...
    // ... existing code ...
    fun getLinuxNeedEnv(env: String): Map<String, String> {
        val projectDir = project?.basePath ?: return emptyMap()

        var targetPath = Paths.get(projectDir, "target", "release").toString()
        if (!Files.exists(Paths.get(targetPath))) {
            targetPath = Paths.get(projectDir, "target", "debug").toString()
        }

        var updatedRequirePath = ""
        if (Files.exists(Paths.get(targetPath))) {
            updatedRequirePath = requirePath
            updatedRequirePath = addPath(targetPath, updatedRequirePath).dropLast(1)
        }

        val envPaths = getEnvPaths()
        var arch =
            try {
                Runtime.getRuntime().exec("arch").inputStream.bufferedReader().readText().trim()
            } catch (e: Exception) {
                ""
            }

        if (arch.isNotEmpty()) {
            arch = "x86_64"
        }

        var ldLibraryPath = ""
        var pathVariable = ""

        envPaths.forEach { entry ->
            if (entry.contains("LD_LIBRARY_PATH")) {
                val parts = entry.split("=")
                ldLibraryPath += parts.last() + ":"
            }
            if (entry.contains("PATH")) {
                val parts = entry.split("=")
                pathVariable += parts.last() + ":"
            }
        }

        pathVariable += env
        ldLibraryPath = ldLibraryPath.replace("\${hw_arch}", arch)
        pathVariable = pathVariable.replace("\${hw_arch}", arch)

        return if (System.getenv("CANGJIE_PATH") == null) {
            mapOf("LD_LIBRARY_PATH" to "$ldLibraryPath$updatedRequirePath", "PATH" to pathVariable)
        } else {
            mapOf(
                "LD_LIBRARY_PATH" to "$ldLibraryPath$updatedRequirePath",
                "CANGJIE_PATH" to System.getenv("CANGJIE_PATH"),
                "PATH" to pathVariable
            )
        }
    }

    //    项目依赖的外部库，在cjpm.toml中获取
    private var requirePath: String = ""

    // ... existing code ...
    fun getWindowsNeedEnv(env: String): Map<String, String> {
        val projectDir = project?.basePath ?: return emptyMap()

        if (Config.isLsp) {
            var targetPath = Paths.get(projectDir, "target", "release").toString()
//            var buildType = "release"

            if (!Files.exists(Paths.get(targetPath))) {
                targetPath = Paths.get(projectDir, "target", "debug").toString()
//                buildType = "debug"
            }

            val cachePath = Paths.get(projectDir, ".cache", "lsp").toString()
            var attemptCount = 0

            while (Files.exists(Paths.get(cachePath))) {
                val deleteCommand = "rd /s /q $cachePath"
                try {
                    Runtime.getRuntime().exec(deleteCommand)
                } catch (e: Exception) {
                    break
                }
                Thread.sleep(100) // Assuming h.delay(c.delay100) is a sleep
                attemptCount += 1
                if (attemptCount == 3) break
            }

            if (Files.exists(Paths.get(targetPath))) {
                val copyCommand = "xcopy $targetPath $cachePath /E /I /H"
                try {
                    Runtime.getRuntime().exec(copyCommand)
                } catch (e: Exception) {
                    // Handle exception if needed
                }
            }
        }

        val envPaths = getEnvPaths()
        var pathVariable = ""
        envPaths.forEach { entry ->
            if (entry.contains("PATH")) {
                val parts = entry.split("=")
                pathVariable = parts.last()
            }
        }

        if (pathVariable.isNotEmpty()) {
            pathVariable = pathVariable.dropLast(1) + ";"
        }

        return if (System.getenv("CANGJIE_PATH") == null) {
            mapOf("PATH" to "$pathVariable;$env")
        } else {
            mapOf("PATH" to pathVariable, "CANGJIE_PATH" to System.getenv("CANGJIE_PATH"))
        }
    }

//    fun getWindowsNeedEnv(e: String): Map<String, String> {
//
//        val projectDir = project.basePath ?: return emptyMap()
//
//        if (Config.isLsp) {
//
//
//            var t = Paths.get(projectDir, "target", "release").toString()
//            var i = "release"
//            if (!Files.exists(Paths.get(t))) {
//                t = Paths.get(projectDir, "target", "debug").toString()
//                i = "debug"
//            }
//            var n = ""
//            val s = Paths.get(projectDir, ".cache", "lsp").toString()
//            var a = 0
//            while (Files.exists(Paths.get(s))) {
//                val command = "rd /s /q $s"
//                try {
//
//                    Runtime.getRuntime().exec(command)
//                } catch (e: Exception) {
//                    break
//                }
//                Thread.sleep(100) // Assuming h.delay(c.delay100) is a sleep
//                a += 1
//                if (a == 3) break
//            }
//            if (Files.exists(Paths.get(t))) {
//                val command = "xcopy $t $s /E /I /H"
//                try {
//                    Runtime.getRuntime().exec(command)
//                } catch (e: Exception) {
//                }
//            }
//
////            val l = requirePath.split(";")
////            for (e in l) {
////                if (e.isEmpty()) continue
////                val normalizedPath = Paths.get(e).normalize().toString()
////                val fileName = Paths.get(e).fileName.toString()
////                val command = "xcopy $normalizedPath ${Paths.get(s, fileName)} /E /I /H"
////                try {
////                    Runtime.getRuntime().exec(command)
////                } catch (e: Exception) {
////                }
////                n = newRequirePath
////                n = addPath(s, n).dropLast(1)
////            }
//        }
//
//        val d = getEnvPaths()
//        var p = ""
//        d.forEach { e ->
//            if (e.contains("PATH")) {
//                val parts = e.split("=")
//                p = parts.last()
//            }
//        }
//        if (p.isNotEmpty()) {
//            p = p.dropLast(1) + ";"
//        }
//        return if (System.getenv("CANGJIE_PATH") == null) {
//            mapOf("PATH" to "$p$n;$e")
//        } else {
//            mapOf("PATH" to "$p$n", "CANGJIE_PATH" to System.getenv("CANGJIE_PATH"))
//        }
//    }

    fun getEnvPaths(): List<String> {
        val toolchainHome = location.toString()

        if (toolchainHome.isEmpty()) throw Error("The Cangjie SDK path has not been configured properly, please configure it first.")

        return try {
            val toolchainDir = File(toolchainHome)
            if (!toolchainDir.exists()) throw Error("The Cangjie SDK path has not been configured properly, please configure it first.")

            var envSetupContent = File("$toolchainHome/envsetup.sh").readText(Charsets.UTF_8)
            var scriptDirRegex = Regex("\\\$\\{script_dir}")
            var cangjieHomeRegex = Regex("\\\$\\{CANGJIE_HOME}")
            var exportRegex = Regex("export(?<id>.*)")

            if (SystemInfo.isWindows) {
                envSetupContent = File("$toolchainHome/envsetup.bat").readText(Charsets.UTF_8)
                scriptDirRegex = Regex("%~dp0")
                cangjieHomeRegex = Regex("%CANGJIE_HOME%")
                exportRegex = Regex("set(?<id>.*)")
            }

            if (envSetupContent.isEmpty()) throw Error("envsetup.sh is empty")
            envSetupContent = envSetupContent.replace(scriptDirRegex, toolchainHome.replace("\\", "\\\\"))
            envSetupContent = if (SystemInfo.isLinux || SystemInfo.isMac
            ) {
                envSetupContent.replace(cangjieHomeRegex, toolchainHome)
            } else {
                envSetupContent.replace(cangjieHomeRegex, "${toolchainHome.replace("\\", "\\\\")}\\\\")

            }


            exportRegex.findAll(envSetupContent).map {
                it.value
//                val arr = it.value.replace("set ", "").split("=")
//
//
//                arr[0] to arr[1]
            }.toList()


        } catch (e: Exception) {
            LOG.error("Failed to get env paths: ${e.message}")


            emptyList()
        }
    }

    fun getEnvironment(): Map<String, String> {


        val separator = if (SystemInfo.isWindows) ";" else ":"

        val runtimeLlvm = if (SystemInfo.isWindows) {
            "windows_x86_64_llvm"
        } else {
            "linux_x86_64_llvm"
        }
        val map = mutableMapOf<String, String>()

        val sdkHome = this.location.systemIndependentPath
        map["LD_LIBRARY_PATH"] =
            "${sdkHome}/runtime/lib/${runtimeLlvm}$separator${System.getenv("LD_LIBRARY_PATH") ?: ""}"
        map["cjcPath"] = "${sdkHome}/bin/"
        map["PATH"] =
            "${sdkHome}/runtime/lib/${runtimeLlvm}$separator${sdkHome}/bin$separator${sdkHome}/tools/bin$separator${
                System.getenv(
                    "PATH"
                )
            }"

        map["CANGJIE_HOME"] = sdkHome
        return map
    }

    //    val presentableLocation: String get() = pathToExecutable(CJPM.NAME).toString()
    abstract fun pathToExecutable(toolName: String): Path
    abstract val fileSeparator: String
    abstract val executionTimeoutInMilliseconds: Int
//    fun looksLikeValidToolchain(): Boolean = CjToolchainFlavor.getFlavor(location) != null

    abstract fun hasExecutable(exec: String): Boolean

    abstract fun hasCjpmExecutable(exec: String): Boolean

    abstract val platformType: String
    fun looksLikeValidToolchain(): Boolean = CjToolchainFlavor.getFlavor(location) != null

    abstract fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine

    abstract fun toLocalPath(remotePath: String): String

    abstract fun toRemotePath(localPath: String): String

    abstract fun expandUserHome(remotePath: String): String

    abstract fun getExecutableName(toolName: String): String

    override fun toString(): String {
        return "Platform: $platformType, Location: $location"
    }

    override fun hashCode(): Int {
        return location.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == this.hashCode()
    }

    fun pathToCjpmExecutable(toolName: String): Path {

        val exePath = pathToExecutable(toolName)
        if (exePath.exists()) return exePath
        val cjpmBin = expandUserHome("~/.cangjie/bin")
        val exeName = getExecutableName(toolName)
        return Paths.get(cjpmBin, exeName)
    }

    fun createGeneralCommandLine(
        executable: Path,
        workingDirectory: Path,
        redirectInputFrom: File?,

        environmentVariables: EnvironmentVariablesData,
        parameters: List<String>,
        emulateTerminal: Boolean,
        withSudo: Boolean,
        patchToRemote: Boolean = true,
        http: HttpConfigurable = HttpConfigurable.getInstance()
    ): GeneralCommandLine {

        val env =

            if (environmentVariables.envs.isEmpty()) {

                EnvironmentVariablesData.create(getEnvironment(), true)

            } else {
                environmentVariables
            }

        var commandLine = GeneralCommandLine(executable, withSudo)
            .withWorkDirectory(workingDirectory)
            .withInput(redirectInputFrom)
//            .withEnvironment("TERM", "ansi")
            .withParameters(parameters)
            .withCharset(Charsets.UTF_8)
            .withRedirectErrorStream(true)
        withProxyIfNeeded(commandLine, http)
        env.configureCommandLine(commandLine, true)

        if (emulateTerminal) {
//            commandLine.exePath = "C:\\Windows\\System32\\chcp.com"
//            commandLine.parametersList.clearAll()

//            if (SystemInfo.isWindows) {
////                commandLine.apply {
////                    parametersList.clearAll()
////                    exePath = "cmd"
////                    addParameter("&&")
////                    addParameter("chcp")
////                    addParameter("65001")
////                    addParameter("&&")
////                    addParameter(executable.toString()) // 添加要执行的命令
////                    addParameters(parameters) // 添加其他参数
////                }
//
//                // 创建一个新的命令行来执行 chcp 65001
//                val ptyCommandLine = PtyCommandLine(listOf("cmd.exe")).apply {
//                    // 设置环境变量以确保使用 UTF-8 编码
//                    environment["LANG"] = "en_US.UTF-8"
//                    environment["LC_ALL"] = "en_US.UTF-8"
//
//                    // 添加 chcp 65001 命令
//                    addParameter("/c")
//                    addParameter("chcp 65001")
//                }
//
//                // 创建进程处理器
//                val processHandler = OSProcessHandler(ptyCommandLine)
//                processHandler.startNotify() // 启动进程
//
//                // 等待 chcp 命令执行完成后再执行主命令
//                processHandler.waitFor() // 等待 chcp 命令完成
//
//            }

            commandLine = PtyCommandLine(commandLine).apply {
                // 设置环境变量以确保使用 UTF-8 编码
//                commandLine.environment["LANG"] = "en_US.UTF-8"
//                commandLine.environment["LC_ALL"] = "en_US.UTF-8"

//                commandLine.addParameter("cmd.exe")
//                commandLine.addParameter("/c")
//                commandLine.addParameter("chcp 65001")
//                commandLine.addParameter("&&")
//                commandLine.addParameter(executable.toString()) // 添加要执行的命令
//                commandLine.addParameters(parameters) // 添加其他参数

            }
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(true)
                .withCharset(Charsets.UTF_8)
        }
        if (patchToRemote) {
            commandLine = patchCommandLine(commandLine)
        }
        return commandLine

    }

    fun toSerializedString(): String {

        return "$platformType::::$location"

    }


    companion object {
        val LOG = Logger.getInstance(CjToolchainBase::class.java)
        val MIN_SUPPORTED_TOOLCHAIN = "0.53.4".parseSemVer()


        fun fromSerializedString(serializedString: String): CjToolchainBase? {

            val (platform, location) = serializedString.split("::::")

//            TODO 扩展点选择的平台是什么？
            return CjToolchainProvider.getToolchain(Paths.get(location))

//            return when (platform) {
//                "loacl" -> CjLocalToolchain(location.toPath())
//
//                else -> null
//            }
        }

        @JvmOverloads
        fun suggest(projectDir: Path? = null): CjToolchainBase? {
            val distribution = projectDir?.let { WslPath.getDistributionByWindowsUncPath(it.toString()) }
            val toolchain = distribution
                ?.getHomePathCandidates()
                ?.filter { CjToolchainFlavor.getFlavor(it) != null }
                ?.mapNotNull { CjToolchainProvider.getToolchain(it.toAbsolutePath()) }
                ?.firstOrNull()
            if (toolchain != null) return toolchain

            return CjToolchainFlavor.getApplicableFlavors()
                .asSequence()
                .flatMap { it.suggestHomePaths() }
                .mapNotNull { CjToolchainProvider.getToolchain(it.toAbsolutePath()) }
                .firstOrNull()
        }
    }
}

fun withProxyIfNeeded(cmdLine: GeneralCommandLine, http: HttpConfigurable) {
    if (http.USE_HTTP_PROXY && http.PROXY_HOST.isNotEmpty()) {
        cmdLine.withEnvironment("http_proxy", http.proxyUri.toString())
    }
}


private val HttpConfigurable.proxyUri: URI
    get() {
        var userInfo: String? = null
        if (PROXY_AUTHENTICATION && !proxyLogin.isNullOrEmpty() && plainProxyPassword != null) {
            val login = proxyLogin
            val password = plainProxyPassword!!
            userInfo = if (password.isNotEmpty()) "$login:$password" else login
        }
        return URI("http", userInfo, PROXY_HOST, PROXY_PORT, "/", null, null)
    }

fun String.parseSemVer(): SemVer = checkNotNull(SemVer.parseFromText(this)) { "Invalid version value: $this" }
