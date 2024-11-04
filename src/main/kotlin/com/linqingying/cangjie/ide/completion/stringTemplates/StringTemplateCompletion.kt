package com.linqingying.cangjie.ide.completion.stringTemplates

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjBlockStringTemplateEntry
import com.linqingying.cangjie.psi.CjDotQualifiedExpression
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.linqingying.cangjie.psi.psiUtil.endOffset
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
                    // ex:
                    // expression: some.IntellijIdeaRulezzz
                    // correctedOffset: ^
                    // expression: some.funcIntellijIdeaRulezzz
                    // correctedOffset      ^
                    val correctedOffset = correctedPosition.endOffset - CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.length
                    return parameters.withPosition(correctedPosition, correctedOffset)
                }
            }
        }
        return null
    }

}
