package com.huawei.cangjie.cjpm.toolchain

import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Path

interface CjToolchainProvider {
    fun getToolchain(homePath: Path): CjToolchainBase?
    fun getToolchains(): List<CjToolchainBase>

    companion object {
        private val EP_NAME: ExtensionPointName<CjToolchainProvider> =
            ExtensionPointName.create("com.huawei.cangjie.toolchainProvider")

        fun getToolchains(): List<CjToolchainBase> {
            return EP_NAME.extensionList.asSequence()
                .mapNotNull { it.getToolchains() }
                .first()
        }

        fun getToolchain(homePath: Path): CjToolchainBase? =
            EP_NAME.extensionList.asSequence()
                .mapNotNull { it.getToolchain(homePath) }
                .firstOrNull()
    }
}
