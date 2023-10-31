package com.huawei.cangjie.highlighter

import com.huawei.cangjie.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


@NonNls
private const val BUNDLE = "messages.CangJieBaseHighlightingBundle"

object CangJieBaseHighlightingBundle : AbstractCangJieBundle(BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}
