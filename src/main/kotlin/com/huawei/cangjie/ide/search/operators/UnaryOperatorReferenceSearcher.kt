package com.huawei.cangjie.ide.search.operators

import com.huawei.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjUnaryExpression
import com.huawei.cangjie.references.CjSimpleNameReference
import com.huawei.cangjie.utils.firstIsInstance
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor

class UnaryOperatorReferenceSearcher(
    targetFunction: PsiElement,
    private val operationToken: CjSingleValueToken,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions
) : OperatorReferenceSearcher<CjUnaryExpression>(
    targetFunction,
    searchScope,
    consumer,
    optimizer,
    options,
    wordsToSearch = listOf(operationToken.value)
) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val unaryExpression = expression.parent as? CjUnaryExpression ?: return
        if (unaryExpression.operationToken != operationToken) return
        processReferenceElement(unaryExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        if (ref !is CjSimpleNameReference) return false
        val element = ref.element
        if (element.parent !is CjUnaryExpression) return false
        return element.getReferencedNameElementType() == operationToken
    }

    override fun extractReference(element: CjElement): PsiReference? {
        val unaryExpression = element as? CjUnaryExpression ?: return null
        if (unaryExpression.operationToken != operationToken) return null
        return unaryExpression.operationReference.references.firstIsInstance<CjSimpleNameReference>()
    }
}
