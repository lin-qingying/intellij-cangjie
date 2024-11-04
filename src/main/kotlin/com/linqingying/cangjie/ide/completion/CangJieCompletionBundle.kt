package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.CangJieCompletionBundle"

object CangJieCompletionBundle : AbstractCangJieBundle(BUNDLE) {

    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)


}
