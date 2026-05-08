package org.cangnova.cangjie.ide.base.compilerPreferences

import org.cangnova.cangjie.messages.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.CangJieBaseCompilerConfigurationUiBundle"

/**
 * 仓颉编译配置 UI bundle。
 *
 * 对位 Kotlin `KotlinBaseCompilerConfigurationUiBundle`。
 * 即使当前 facet/compiler 配置 UI 还未完全展开，消息 bundle 的归属位也应先固定。
 */
object CangJieBaseCompilerConfigurationUiBundle : AbstractCangJieBundle(BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}
