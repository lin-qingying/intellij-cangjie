package com.linqingying.cangjie.ide.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.openapi.components.service


interface CompletionDummyIdentifierProviderService {
    /**
     * 修正字符串模板中的转义符位置
     */
    fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean
    /**
     *如果插入符号位于参数声明处，则将替换偏移量设置为类型引用的末尾。
     *正确填写参数名称WITH TYPE是必需的。
     */
    fun correctPositionForParameter(context: CompletionInitializationContext)

    /**
     * 生成一个虚拟的标识符
     */
    fun provideDummyIdentifier(context: CompletionInitializationContext): String

    companion object {
        fun getInstance(): CompletionDummyIdentifierProviderService = service()
    }
}
