package com.huawei.cangjie.ide.search.operators

import com.huawei.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.psi.CjBinaryExpression
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.references.CjSimpleNameReference
import com.huawei.cangjie.utils.firstIsInstance
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor


class BinaryOperatorReferenceSearcher(
    targetFunction: PsiElement,
    private val operationTokens: List<CjSingleValueToken>,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions
) : OperatorReferenceSearcher<CjBinaryExpression>(
    targetFunction,
    searchScope,
    consumer,
    optimizer,
    options,
    wordsToSearch = operationTokens.map { it.value }) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val binaryExpression = expression.parent as? CjBinaryExpression ?: return
        if (binaryExpression.operationToken !in operationTokens) return
        if (expression != binaryExpression.left) return
        processReferenceElement(binaryExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean {
        if (ref !is CjSimpleNameReference) return false
        val element = ref.element
        if (element.parent !is CjBinaryExpression) return false
        return element.getReferencedNameElementType() in operationTokens
    }

    override fun extractReference(element: CjElement): PsiReference? {
        val binaryExpression = element as? CjBinaryExpression ?: return null
        if (binaryExpression.operationToken !in operationTokens) return null
        return binaryExpression.operationReference.references.firstIsInstance<CjSimpleNameReference>()
    }
}
