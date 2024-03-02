package com.huawei.cangjie.cjpm.toolchain

import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path


class CjLocalToolchainProvider : CjToolchainProvider {

    val map = mutableMapOf<Path,CjLocalToolchain>()
    override fun getToolchain(homePath: Path): CjToolchainBase? {
        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) return null




        if (map.containsKey(homePath)) return map[homePath]

        val toolchain = CjLocalToolchain(homePath)
        map.put(homePath,toolchain)
        return toolchain


//        return CjLocalToolchain(homePath)
    }
}
