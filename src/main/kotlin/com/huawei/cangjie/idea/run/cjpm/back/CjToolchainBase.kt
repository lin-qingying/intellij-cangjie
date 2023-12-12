//package com.huawei.cangjie.idea.run.cjpm
//
//import com.huawei.cangjie.CangJieBundle
//import com.huawei.cangjie.idea.run.cjpm.back.CjToolchainFlavor
//import com.huawei.cangjie.idea.run.cjpm.back.CjToolchainProvider
//import com.huawei.cangjie.idea.run.cjpm.back.Cjc
//import com.huawei.cangjie.idea.run.cjpm.runconfig.isUnitTestMode
//import com.intellij.execution.configuration.EnvironmentVariablesData
//import com.intellij.execution.configurations.GeneralCommandLine
//import com.intellij.execution.process.ElevationService
//import com.intellij.execution.wsl.WSLDistribution
//import com.intellij.execution.wsl.WslPath
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.progress.ProgressManager
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.project.ProjectManager
//import com.intellij.openapi.util.NlsContexts
//import com.intellij.util.io.isDirectory
//import com.intellij.util.io.systemIndependentPath
//import com.intellij.util.net.HttpConfigurable
//import java.io.File
//import java.net.URI
//import java.nio.file.InvalidPathException
//import java.nio.file.Path
//
//
//fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)
//
//@Suppress("FunctionName", "UnstableApiUsage")
//fun GeneralCommandLine(path: Path, withSudo: Boolean = false, vararg args: String) =
//    object : GeneralCommandLine(path.systemIndependentPath, *args) {
//        override fun createProcess(): Process = if (withSudo) {
//            ElevationService.getInstance().createProcess(this)
//        } else {
//            super.createProcess()
//        }
//    }
//abstract class CjToolchainBase(val location: Path) {
//    abstract fun hasExecutable(exec: String): Boolean
//    abstract fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine
//    abstract val executionTimeoutInMilliseconds: Int
//    abstract val fileSeparator: String
//
//    abstract fun toLocalPath(remotePath: String): String
//
//    abstract fun toRemotePath(localPath: String): String
//
//    fun createGeneralCommandLine(
//        executable: Path,
//        workingDirectory: Path,
//        redirectInputFrom: File?,
//
//        environmentVariables: EnvironmentVariablesData,
//        parameters: List<String>,
//
//        http: HttpConfigurable = HttpConfigurable.getInstance()
//
//    ): GeneralCommandLine {
//        var commandLine = GeneralCommandLine(executable, false)
//            .withWorkDirectory(workingDirectory)
//            .withInput(redirectInputFrom)
//            .withEnvironment("TERM", "ansi")
//            .withParameters(parameters)
//            .withCharset(Charsets.UTF_8)
//            .withRedirectErrorStream(true)
//
//        withProxyIfNeeded(commandLine, http)
//
//        environmentVariables.configureCommandLine(commandLine, true)
//        return commandLine
//    }
//
//
//    companion object{
//        @JvmOverloads
//        fun suggest(projectDir: Path? = null): CjToolchainBase? {
//            val distribution = projectDir?.let { WslPath.getDistributionByWindowsUncPath(it.toString()) }
//            val toolchain = distribution
//                ?.getHomePathCandidates()
//                ?.filter { CjToolchainFlavor.getFlavor(it) != null }
//                ?.mapNotNull { CjToolchainProvider.getToolchain(it.toAbsolutePath()) }
//                ?.firstOrNull()
//            if (toolchain != null) return toolchain
//
//            return CjToolchainFlavor.getApplicableFlavors()
//                .asSequence()
//                .flatMap { it.suggestHomePaths() }
//                .mapNotNull { CjToolchainProvider.getToolchain(it.toAbsolutePath()) }
//                .firstOrNull()
//        }
//    }
//
//    abstract fun pathToExecutable(toolName: String): Path
//    fun  cjc(): Cjc = Cjc(this)
//}
//fun withProxyIfNeeded(cmdLine: GeneralCommandLine, http: HttpConfigurable) {
//    if (http.USE_HTTP_PROXY && http.PROXY_HOST.isNotEmpty()) {
//        cmdLine.withEnvironment("http_proxy", http.proxyUri.toString())
//    }
//}
//private val HttpConfigurable.proxyUri: URI
//    get() {
//        var userInfo: String? = null
//        if (PROXY_AUTHENTICATION && !proxyLogin.isNullOrEmpty() && plainProxyPassword != null) {
//            val login = proxyLogin
//            val password = plainProxyPassword!!
//            userInfo = if (password.isNotEmpty()) "$login:$password" else login
//        }
//        return URI("http", userInfo, PROXY_HOST, PROXY_PORT, "/", null, null)
//    }
//fun WSLDistribution.getHomePathCandidates(): Sequence<Path> = sequence {
//    @Suppress("UnstableApiUsage", "UsePropertyAccessSyntax")
//    val root = getUNCRootPath()
//    val environment = compute(CangJieBundle.message("progress.title.getting.environment.variables")) { environment }
//    if (environment != null) {
//        val home = environment["HOME"]
//        val remoteCargoPath = home?.let { "$it/.cargo/bin" }
//        val localCargoPath = remoteCargoPath?.let { root.resolve(it) }
//        if (localCargoPath?.isDirectory() == true) {
//            yield(localCargoPath)
//        }
//
//        val sysPath = environment["PATH"]
//        for (remotePath in sysPath.orEmpty().split(":")) {
//            if (remotePath.isEmpty()) continue
//            val localPath = root.resolveOrNull(remotePath) ?: continue
//            if (!localPath.isDirectory()) continue
//            yield(localPath)
//        }
//    }
//
//    for (remotePath in listOf("/usr/local/bin", "/usr/bin")) {
//        val localPath = root.resolve(remotePath)
//        if (!localPath.isDirectory()) continue
//        yield(localPath)
//    }
//}
//
//
//
//
//val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread
//private fun <T> compute(
//    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
//    getter: () -> T
//): T = if (isDispatchThread) {
//    val project = ProjectManager.getInstance().defaultProject
//    project.computeWithCancelableProgress(title, getter)
//} else {
//    getter()
//}
//fun <T> Project.computeWithCancelableProgress(
//    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
//    supplier: () -> T
//): T {
//    if (isUnitTestMode) {
//        return supplier()
//    }
//    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
//}
//fun Path.resolveOrNull(other: String): Path? = pathOrNull { resolve(other) }
//private inline fun pathOrNull(block: () -> Path): Path? {
//    return try {
//        block()
//    } catch (e: InvalidPathException) {
//
//        null
//    }
//}
