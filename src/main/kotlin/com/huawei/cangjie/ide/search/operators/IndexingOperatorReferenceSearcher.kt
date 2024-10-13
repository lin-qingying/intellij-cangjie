package com.huawei.cangjie.ide.search.operators

import com.huawei.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.huawei.cangjie.psi.CjArrayAccessExpression
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.references.CjArrayAccessReference
import com.huawei.cangjie.references.readWriteAccess
import com.huawei.cangjie.utils.firstIsInstance
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor


class IndexingOperatorReferenceSearcher(
    targetFunction: PsiElement,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions,
    private val isSet: Boolean
) : OperatorReferenceSearcher<CjArrayAccessExpression>(
    targetFunction,
    searchScope,
    consumer,
    optimizer,
    options,
    wordsToSearch = listOf("[")
) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val accessExpression = expression.parent as? CjArrayAccessExpression ?: return
        if (expression != accessExpression.arrayExpression) return
        if (!checkAccessExpression(accessExpression)) return
        processReferenceElement(accessExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference) =
        ref is CjArrayAccessReference && checkAccessExpression(ref.element)

    override fun extractReference(element: CjElement): PsiReference? {
        val accessExpression = element as? CjArrayAccessExpression ?: return null
        if (!checkAccessExpression(accessExpression)) return null
        return accessExpression.references.firstIsInstance<CjArrayAccessReference>()
    }

    private fun checkAccessExpression(accessExpression: CjArrayAccessExpression): Boolean {
        val readWriteAccess = accessExpression.readWriteAccess(useResolveForReadWrite = false)
        return if (isSet) readWriteAccess.isWrite else readWriteAccess.isRead
    }
}
