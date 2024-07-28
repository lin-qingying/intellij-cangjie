package com.huawei.cangjie.cjpm.toolchain

import com.huawei.cangjie.cjpm.toolchain.flavors.hasExecutable
import com.huawei.cangjie.cjpm.toolchain.flavors.isExecutable
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory


open class CjLocalToolchain(  location: Path) : CjToolchainBase(location) {
    companion object {
        fun create(homePath: Path): CjLocalToolchain? {
            val tool = CjLocalToolchain(homePath)

            if (tool.toolsPath.isDirectory()) {
                return tool

            }
            return null
        }
    }

    override fun pathToExecutable(toolName: String): Path = location.pathToExecutable(toolName)


    override val fileSeparator: String
        get() = File.separator
    override val executionTimeoutInMilliseconds: Int
        get() = 1000

    override fun hasExecutable(exec: String): Boolean {
        return location.hasExecutable(exec)
    }

    override fun hasCjpmExecutable(exec: String): Boolean = pathToCjpmExecutable(exec).isExecutable()
    override val platformType: String = "local"

    override fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine = commandLine
    override fun toLocalPath(remotePath: String): String {
        return remotePath
    }

    override fun toRemotePath(localPath: String): String = localPath

    override fun expandUserHome(remotePath: String): String = FileUtil.expandUserHome(remotePath)

    override fun getExecutableName(toolName: String): String = if (SystemInfo.isWindows) "$toolName.exe" else toolName
}

fun Path.pathToExecutable(toolName: String): Path {
    val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
    return resolve(exeName).toAbsolutePath()
}
