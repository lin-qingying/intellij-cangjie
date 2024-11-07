package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.AbstractCangJieBundle
import com.linqingying.cangjie.diagnostics.DiagnosticFactoryWithPsiElement
import com.linqingying.cangjie.highlighter.CangJieHighlightingBundle.withHtml
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


@NonNls
private const val BUNDLE = "messages.CangJieDiagnosisBundle"

object CangJieDiagnosisBundle : AbstractCangJieBundle(BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
    @Nls
    @JvmStatic
    fun rawMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: DiagnosticFactoryWithPsiElement<*,*>): String =
        getMessage(key.name) // 不传递任何参数
    // 新增的方法来获取原始内容
//    @Nls
//    @JvmStatic
//    fun rawMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String): String =
//        getMessage(key) // 不传递任何参数
    @Nls
    @JvmStatic
    fun htmlMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params).withHtml()
}
