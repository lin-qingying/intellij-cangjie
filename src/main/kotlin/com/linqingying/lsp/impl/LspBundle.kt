package com.linqingying.lsp.impl

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier


object LspBundle {
    private const val BUNDLE = "messages.LspBundle"
    private val INSTANCE = DynamicBundle(LspBundle::class.java, "messages.LspBundle")

    @Nls
    fun message(@PropertyKey(resourceBundle = "messages.LspBundle") key: String, vararg params: Any): String {
        return INSTANCE.getMessage(key, *params)
    }

    fun messagePointer(
        @PropertyKey(resourceBundle = "messages.LspBundle") key: String,
        vararg params: Any
    ): Supplier<@Nls String> {
        return INSTANCE.getLazyMessage(key, *params)
    }
}
