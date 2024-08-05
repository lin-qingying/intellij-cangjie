package com.huawei.cangjie.cjpm.toolchain

import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path


class CjLocalToolchainProvider : CjToolchainProvider {

    val map = mutableMapOf<Path, CjLocalToolchain>()

    override fun getToolchains(): List<CjToolchainBase> {
        return map.values.toList()
    }


    override fun getToolchain(homePath: Path): CjToolchainBase? {
        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) return null
        return CjLocalToolchain(homePath)
    }
//    override fun getToolchain(homePath: Path): CjToolchainBase? {
//        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) return null
//
//
//
//
//        if (map.containsKey(homePath)) return map[homePath]
//
////        val toolchain = CjLocalToolchain(homePath)
//
//        val toolchain = CjLocalToolchain.create(homePath)
//
//        map[homePath] = toolchain ?: return null
//        return toolchain
//
//
////        return CjLocalToolchain(homePath)
//    }
}
