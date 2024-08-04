package com.linqingying.cangjie.ide.project.tools.projectWizard

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE: String = "messages.CangJieUiBundle"

object CangJieUiBundle : DynamicBundle(  BUNDLE) {


    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: String): @Nls String {
        return getMessage(key, *params)
    }

    fun messagePointer(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: String): Supplier<String> {
        return getLazyMessage(key, *params)
    }
}
