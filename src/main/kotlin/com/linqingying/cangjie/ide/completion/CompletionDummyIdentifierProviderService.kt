package com.linqingying.cangjie.ide.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.openapi.components.service


interface CompletionDummyIdentifierProviderService {
    fun correctPositionForStringTemplateEntry(context: CompletionInitializationContext): Boolean
    /**
     * If caret is at the parameter declaration, sets replacement offset to the end of the type reference.
     * It is required for correct completion of parameter name with type.
     */
    fun correctPositionForParameter(context: CompletionInitializationContext)
    fun provideDummyIdentifier(context: CompletionInitializationContext): String

    companion object {
        fun getInstance(): CompletionDummyIdentifierProviderService = service()
    }
}
