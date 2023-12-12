//package com.huawei.cangjie.idea.run.cjpm.runconfig.toolchain
//
//import com.huawei.cangjie.idea.run.cjpm.back.CjToolchainProvider
//import com.huawei.cangjie.idea.run.cjpm.back.hasExecutable
//import com.huawei.cangjie.idea.run.cjpm.back.pathToExecutable
//import com.huawei.cangjie.idea.run.cjpm.CjToolchainBase
//import com.huawei.cangjie.idea.run.cjpm.runconfig.toPath
//import com.intellij.execution.configurations.GeneralCommandLine
//import com.intellij.execution.wsl.WSLCommandLineOptions
//import com.intellij.execution.wsl.WSLDistribution
//import com.intellij.execution.wsl.WSLUtil
//import com.intellij.execution.wsl.WslPath
//import com.intellij.openapi.util.SystemInfo
//import com.intellij.openapi.util.io.FileUtil
//import com.intellij.util.io.isFile
//import com.intellij.util.io.systemIndependentPath
//import java.io.File
//import java.nio.file.Path
//
//
//class CjLocalToolchainProvider : CjToolchainProvider {
//    override fun getToolchain(homePath: Path): CjToolchainBase? {
//        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) return null
//        return CjLocalToolchain(homePath)
//    }
//}
//
//open class CjLocalToolchain(location: Path) : CjToolchainBase(location) {
//    override val fileSeparator: String get() = File.separator
//    override fun toLocalPath(remotePath: String): String = remotePath
//
//    override fun toRemotePath(localPath: String): String = localPath
//
//    override val executionTimeoutInMilliseconds: Int = 1000
//
//    override fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine = commandLine
//
//
//    override fun pathToExecutable(toolName: String): Path = location.pathToExecutable(toolName)
//
//    override fun hasExecutable(exec: String): Boolean = location.hasExecutable(exec)
//
//
//}
//
//class CjWslToolchainProvider : CjToolchainProvider {
//    override fun getToolchain(homePath: Path): CjToolchainBase? {
//
//        val wslPath = WslPath.parseWindowsUncPath(homePath.toString()) ?: return null
//        return CjWslToolchain(wslPath)
//    }
//}
//
//class CjWslToolchain(
//    val wslPath: WslPath
//) : CjToolchainBase(wslPath.distribution.getWindowsPathWithFix(wslPath.linuxPath).toPath()) {
//    private val distribution: WSLDistribution get() = wslPath.distribution
//    private val linuxPath: Path = wslPath.linuxPath.toPath()
//    override val fileSeparator: String = "/"
//
//
//    override val executionTimeoutInMilliseconds: Int = 5000
//    override fun toRemotePath(localPath: String): String =
//        distribution.getWslPath(localPath) ?: localPath
//
//    override fun toLocalPath(remotePath: String): String =
//        distribution.getWindowsPathWithFix(remotePath)
//
//    override fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine {
//        commandLine.exePath = toRemotePath(commandLine.exePath)
//
//        val parameters = commandLine.parametersList.list.map { toRemotePath(it) }
//        commandLine.parametersList.clearAll()
//        commandLine.parametersList.addAll(parameters)
//
//        commandLine.environment.forEach { (k, v) ->
//            val paths = v.split(File.pathSeparatorChar)
//            commandLine.environment[k] = paths.joinToString(":") { toRemotePath(it) }
//        }
//
//        commandLine.workDirectory?.let {
//            if (it.path.startsWith(fileSeparator)) {
//                commandLine.workDirectory = File(toLocalPath(it.path))
//            }
//        }
//
//        val remoteWorkDir = commandLine.workDirectory?.absolutePath
//            ?.let { toRemotePath(it) }
//        val options = WSLCommandLineOptions()
//            .setRemoteWorkingDirectory(remoteWorkDir)
//            .addInitCommand("export PATH=\"${linuxPath.systemIndependentPath}:\$PATH\"")
//        return distribution.patchCommandLine(commandLine, null, options)
//    }
//
//
//    override fun pathToExecutable(toolName: String): Path = linuxPath.pathToExecutableOnWsl(toolName)
//
//    override fun hasExecutable(exec: String): Boolean =
//        distribution.getWindowsPath(pathToExecutable(exec)).isFile()
//
//
//    companion object {
//
//        private fun WSLDistribution.getWindowsPathWithFix(wslPath: String): String {
//            val systemIndependentPath = FileUtil.toSystemIndependentName(wslPath)
//
//            @Suppress("UnstableApiUsage", "UsePropertyAccessSyntax")
//            val uncRoot = getUNCRootPath().systemIndependentPath
//            return when {
//                systemIndependentPath.startsWith(uncRoot) || !systemIndependentPath.startsWith("/") -> systemIndependentPath
//                systemIndependentPath.startsWith(mntRoot) -> WSLUtil.getWindowsPath(systemIndependentPath, mntRoot)
//                else -> getWindowsPath(systemIndependentPath)
//            } ?: systemIndependentPath
//        }
//
//        private fun WSLDistribution.getWindowsPath(wslPath: Path): Path =
//            getWindowsPathWithFix(wslPath.toString()).toPath()
//    }
//}
//
//fun Path.pathToExecutableOnWsl(toolName: String): Path = resolve(toolName)
