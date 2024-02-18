package com.huawei.cangjie.cjpm.toolchain.flavors

import com.huawei.cangjie.cjpm.project.toPath
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.streams.asSequence

class CjWinToolchainFlavor : CjToolchainFlavor() {

    override fun getHomePathCandidates(): Sequence<Path> {
        val programFiles = System.getenv("ProgramFiles")?.toPath() ?: return emptySequence()
        if (!programFiles.exists() || !programFiles.isDirectory()) return emptySequence()
        return programFiles.list()
            .filter { it.isDirectory() }
            .filter {
                val name = FileUtil.getNameWithoutExtension(it.fileName.toString())
                name.lowercase().startsWith("cangjie")
            }
            .map { it.resolve("bin") }
            .filter { it.isDirectory() }
    }

    override fun isApplicable(): Boolean = SystemInfo.isWindows
}

fun Path.list(): Sequence<Path> = Files.list(this).asSequence()
