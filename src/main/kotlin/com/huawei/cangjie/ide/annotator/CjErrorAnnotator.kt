package com.huawei.cangjie.ide.annotator

import com.huawei.cangjie.lang.core.psi.CjFile
import com.huawei.cangjie.lang.core.psi.CjFunction
import com.huawei.cangjie.lang.core.psi.CjVisitor
import com.huawei.cangjie.lang.core.psi.ext.CjInferenceContextOwner
import com.huawei.cangjie.lang.core.psi.ext.CjNameIdentifierOwner
import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CjErrorAnnotator : AnnotatorBase(), HighlightRangeExtension {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val cjHolder = CjAnnotationHolder(holder)
        val visitor = object : CjVisitor() {
            override fun visitFunction(o: CjFunction) = checkFunction(cjHolder, o)
        }

        element.accept(visitor)
    }
    override fun isForceHighlightParents(file: PsiFile): Boolean = file is CjFile

    private fun checkFunction(holder: CjAnnotationHolder, fn: CjFunction) {
//        collectDiagnostics(holder, fn)
        checkDuplicates(holder, fn.identifierName)
//        checkTypesAreSized(holder, fn)
//        checkEmptyFunctionReturnType(holder, fn)
//        checkRecursiveAsyncFunction(holder, fn)

//        fn.innerAttrList.forEach { checkStartAttribute(holder, it) }
//        fn.outerAttrList.forEach { checkStartAttribute(holder, it) }
    }
    private fun collectDiagnostics(holder: CjAnnotationHolder, element: CjInferenceContextOwner) {
//        for (it in element.selfInferenceResult.diagnostics) {
//            if (it.inspectionClass == javaClass) it.addToHolder(holder)
//        }
    }

    private fun checkDuplicates(
        holder: CjAnnotationHolder,
        element: CjNameIdentifierOwner,
        scope: PsiElement? = element.context,
        recursively: Boolean = false
    ) {

        val identifier = element.nameIdentifier ?: element
//        println(identifier)
    }

}
