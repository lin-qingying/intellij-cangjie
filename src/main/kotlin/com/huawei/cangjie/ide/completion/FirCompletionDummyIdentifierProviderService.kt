package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.psi.CjMatchEntry
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.util.parentOfType



class FirCompletionDummyIdentifierProviderService : AbstractCompletionDummyIdentifierProviderService() {
    override fun handleDefaultCase(context: CompletionInitializationContext): String? {
        val elementAtOffset = context.file.findElementAt(context.startOffset) ?: return null
        return when {
            elementAtOffset.parentOfType<CjMatchEntry>() != null -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
            else -> null
        }
    }

    override fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean {
        return true

    }
}
