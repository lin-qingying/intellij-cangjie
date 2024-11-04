package com.linqingying.cangjie.ide.lineMarkers

import com.linqingying.cangjie.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val BUNDLE = "messages.CangJieLineMarkersSharedBundle"

internal object CangJieLineMarkersSharedBundle : AbstractCangJieBundle(BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }

}
