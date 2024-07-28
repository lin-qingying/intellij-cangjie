package com.huawei.cangjie.ide.completion.implCommon

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjBlockStringTemplateEntry
import com.huawei.cangjie.psi.CjDotQualifiedExpression
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.psi.psiUtil.endOffset
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionUtilCore


object StringTemplateCompletion {
    fun correctParametersForInStringTemplateCompletion(parameters: CompletionParameters): CompletionParameters? {
        val position = parameters.position
        if (position.node.elementType == CjTokens.LONG_TEMPLATE_ENTRY_START) {
            val expression = (position.parent as? CjBlockStringTemplateEntry)?.expression
            if (expression is CjDotQualifiedExpression) {
                val correctedPosition = (expression.selectorExpression as? CjNameReferenceExpression)?.firstChild
                if (correctedPosition != null) {

                    val correctedOffset = correctedPosition.endOffset - CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.length
                    return parameters.withPosition(correctedPosition, correctedOffset)
                }
            }
        }
        return null
    }

}
