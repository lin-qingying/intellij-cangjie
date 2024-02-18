package com.huawei.cangjie.cjpm.toolchain.flavors

import com.huawei.cangjie.cjpm.project.toPath
import com.huawei.cangjie.cjpm.project.toPathOrNull
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.EnvironmentUtil
import java.nio.file.Path
import kotlin.io.path.isDirectory


class CjpmToolchainFlavor : CjToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> {
        val cjpmHome = EnvironmentUtil.getValue("CANGJIE_HOME")?.toPathOrNull()
        val userHome = FileUtil.expandUserHome("~/.cangjie/").toPath()
        return sequenceOf(cjpmHome, userHome)
            .filterNotNull()
//            .map { it.resolve("tools") }
            .filter { it.isDirectory() }
    }
}