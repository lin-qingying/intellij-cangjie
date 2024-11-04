package com.linqingying.cangjie.ide.completion.handlers

import com.linqingying.cangjie.psi.CjCallExpression
import com.linqingying.cangjie.psi.CjDotQualifiedExpression
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager

class WithExpressionPrefixInsertHandler(val prefix: String) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        item.handleInsert(context)

        postHandleInsert(context)
    }

    fun postHandleInsert(context: InsertionContext) {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

        val offset = context.startOffset
        val token = context.file.findElementAt(offset)!!
        var expression = token.getStrictParentOfType<CjExpression>() ?: return
        if (expression is CjSimpleNameExpression) {
            var parent = expression.getParent()
            if (parent is CjCallExpression && expression == parent.calleeExpression) {
                expression = parent
                parent = parent.parent
            }
            if (parent is CjDotQualifiedExpression && expression == parent.selectorExpression) {
                expression = parent
            }
        }

        context.document.insertString(expression.textRange.startOffset, prefix)
    }
}
