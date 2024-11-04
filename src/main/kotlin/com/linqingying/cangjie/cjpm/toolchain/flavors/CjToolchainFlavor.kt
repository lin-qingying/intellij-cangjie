package com.linqingying.cangjie.cjpm.toolchain.flavors

import com.linqingying.cangjie.cjpm.toolchain.pathToExecutable
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjc
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm
import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun Path.hasExecutable(toolName: String): Boolean = pathToExecutable(toolName).isExecutable()
fun Path.isExecutable(): Boolean = Files.isExecutable(this)

abstract class CjToolchainFlavor {

    fun suggestHomePaths(): Sequence<Path> = getHomePathCandidates().filter { isValidToolchainPath(it) }
    protected abstract fun getHomePathCandidates(): Sequence<Path>


    protected open fun isValidToolchainPath(path: Path): Boolean {
        return path.isDirectory() &&
                hasExecutable(path, Cjc.NAME) &&
                hasExecutable(path, Cjpm.NAME)
    }
    protected open fun hasExecutable(path: Path, toolName: String): Boolean = path.hasExecutable(toolName)

    /**
     * Flavor is added to result in [getApplicableFlavors] if this method returns true.
     * @return whether this flavor is applicable.
     */
    protected open fun isApplicable(): Boolean = true

    companion object {
        private val EP_NAME: ExtensionPointName<CjToolchainFlavor> =
            ExtensionPointName.create("com.linqingying.cangjie.toolchainFlavor")

        fun getApplicableFlavors(): List<CjToolchainFlavor> =
            EP_NAME.extensionList.filter { it.isApplicable() }

        fun getFlavor(path: Path): CjToolchainFlavor? =
            getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
    }
}
