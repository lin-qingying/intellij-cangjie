package com.linqingying.cangjie.ide.search.operators

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjForExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.util.Processor

//
//class IteratorOperatorReferenceSearcher(
//    targetFunction: PsiElement,
//    searchScope: SearchScope,
//    consumer: Processor<in PsiReference>,
//    optimizer: SearchRequestCollector,
//    options: CangJieReferencesSearchOptions
//) : OperatorReferenceSearcher<CjForExpression>(targetFunction, searchScope, consumer, optimizer, options, wordsToSearch = listOf("in")) {
//
//    override fun processPossibleReceiverExpression(expression: CjExpression) {
//        val parent = expression.parent
//        if (parent.node.elementType == CjNodeTypes.LOOP_RANGE) {
//            processReferenceElement(parent.parent as CjForExpression)
//        }
//    }
//
//    override fun isReferenceToCheck(ref: PsiReference): Boolean {
//        return ref is CjForLoopInReference
//    }
//
//    override fun extractReference(element: CjElement): PsiReference? {
//        return (element as? CjForExpression)?.references?.firstIsInstance<CjForLoopInReference>()
//    }
//}
