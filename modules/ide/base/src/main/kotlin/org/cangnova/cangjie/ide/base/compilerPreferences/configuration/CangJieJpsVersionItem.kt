package org.cangnova.cangjie.ide.base.compilerPreferences.configuration

import org.cangnova.cangjie.ide.base.compilerPreferences.CangJieBaseCompilerConfigurationUiBundle
import org.cangnova.cangjie.toolchain.api.CjSdk

/**
 * 仓颉工具链配置项。
 *
 * 对位 Kotlin `JpsVersionItem` 的文件位置。
 * Kotlin 这里承载 JPS 编译器版本项；仓颉没有独立 JPS 版本矩阵，
 * 因此这里显式承载可选工具链项，而不是伪造平台或 JPS 版本源。
 */
class CangJieJpsVersionItem private constructor(
    val sdk: CjSdk?,
    private val text: String?,
    val enabled: Boolean,
) {
    constructor(sdk: CjSdk) : this(sdk, null, true)

    companion object {
        fun createLabel(text: String): CangJieJpsVersionItem = CangJieJpsVersionItem(null, text, false)
    }

    fun getDescription(): String {
        text?.let { return it }

        val sdk = requireNotNull(sdk)
        val version = sdk.version
        return when {
            !sdk.isValid -> CangJieBaseCompilerConfigurationUiBundle.message(
                "configuration.invalid.toolchain",
                sdk.name,
            )
            version == null -> CangJieBaseCompilerConfigurationUiBundle.message(
                "configuration.unknown.toolchain",
                sdk.name,
            )
            else -> CangJieBaseCompilerConfigurationUiBundle.message(
                "configuration.toolchain.entry",
                sdk.name,
                version.semver.parsedVersion,
            )
        }
    }

    fun getRawVersion(): String = sdk?.version?.semver?.parsedVersion ?: (text ?: "")

    override fun toString(): String = getDescription()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangJieJpsVersionItem) return false
        return sdk?.id == other.sdk?.id && text == other.text
    }

    override fun hashCode(): Int = 31 * (sdk?.id?.hashCode() ?: 0) + (text?.hashCode() ?: 0)
}
