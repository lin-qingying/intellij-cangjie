package com.huawei.cangjie.idea.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.openapi.components.service


interface CompletionDummyIdentifierProviderService {
    fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean

    fun correctPositionForParameter(context: CompletionInitializationContext)
    fun provideDummyIdentifier(context: CompletionInitializationContext): String

    companion object {
        fun getInstance(): CompletionDummyIdentifierProviderService = service()
    }
}
