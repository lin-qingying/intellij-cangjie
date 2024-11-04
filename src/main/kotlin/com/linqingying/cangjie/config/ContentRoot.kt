package com.linqingying.cangjie.config

interface ContentRoot {

}
/**
 * @param isCommon whether this source root contains sources of a common module in a multi-platform project
 */
data class CangJieSourceRoot(val path: String, val isCommon: Boolean, val hmppModuleName: String?): ContentRoot
fun CompilerConfiguration.addCangJieSourceRoots(sources: List<String>): Unit =
    sources.forEach { addCangJieSourceRoot(it) }
@JvmOverloads
fun CompilerConfiguration.addCangJieSourceRoot(path: String, isCommon: Boolean = false, hmppModuleName: String? = null) {
    add(CLIConfigurationKeys.CONTENT_ROOTS, CangJieSourceRoot(path, isCommon, hmppModuleName))
}
val CompilerConfiguration.cangjieSourceRoots: List<CangJieSourceRoot>
    get() = get(CLIConfigurationKeys.CONTENT_ROOTS)?.filterIsInstance<CangJieSourceRoot>().orEmpty()
