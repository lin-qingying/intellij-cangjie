package com.linqingying.cangjie.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.extensions.ExtensionPointName



abstract class CangJieCompletionExtension {
    abstract fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean

    companion object {
        val EP_NAME: ExtensionPointName<CangJieCompletionExtension> = ExtensionPointName.create("com.linqingying.cangjie.completionExtension")
    }
}
