package com.huawei.cangjie.cjpm.toolchain

import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path


class CjLocalToolchainProvider : CjToolchainProvider {
    override fun getToolchain(homePath: Path): CjToolchainBase? {
        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) return null
        return CjLocalToolchain(homePath)
    }
}
