package com.huawei.cangjie.ide.search.operators

import com.huawei.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.psiUtil.isExtensionDeclaration
import com.huawei.cangjie.references.CjInvokeFunctionReference
import com.huawei.cangjie.utils.firstIsInstance
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor

class InvokeOperatorReferenceSearcher(
    targetFunction: PsiElement,
    searchScope: SearchScope,
    consumer: Processor<in PsiReference>,
    optimizer: SearchRequestCollector,
    options: CangJieReferencesSearchOptions
) : OperatorReferenceSearcher<CjCallExpression>(targetFunction, searchScope, consumer, optimizer, options, wordsToSearch = emptyList()) {

    override fun processPossibleReceiverExpression(expression: CjExpression) {
        val callExpression = expression.parent as? CjCallExpression ?: return
        processReferenceElement(callExpression)
    }

    override fun isReferenceToCheck(ref: PsiReference): Boolean  = ref is CjInvokeFunctionReference

    override fun extractReference(element: CjElement): PsiReference? {
        val callExpression = element as? CjCallExpression ?: return null


        return callExpression.references.firstIsInstance<CjInvokeFunctionReference>()
    }

}
