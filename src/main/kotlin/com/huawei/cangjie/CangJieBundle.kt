package com.huawei.cangjie

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey



@NonNls
const val BUNDLE = "messages.CangJieBundle"

object CangJieBundle : AbstractCangJieBundle(BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)

    @Nls
    @JvmStatic
    fun htmlMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params).withHtml()

    @Nls
    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): () -> String = { getMessage(key, *params) }
}
