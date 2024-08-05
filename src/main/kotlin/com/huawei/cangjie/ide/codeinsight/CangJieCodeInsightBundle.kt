package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


@NonNls
private const val BUNDLE = "messages.CangJieCodeInsightBundle"

object CangJieCodeInsightBundle : AbstractCangJieBundle(BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)
}
