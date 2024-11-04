package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val BUNDLE = "messages.CangJieIndexingBundle"


object CangJieIndexingBundle : AbstractCangJieBundle(BUNDLE) {

    @Nls
    @JvmStatic
    fun message(
        @NonNls @PropertyKey(resourceBundle =  BUNDLE) key: String,
        vararg params: Any
    ): String = getMessage(key, *params)

}
