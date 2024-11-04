package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjTypeStatement
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
data class ReceiverTypeSearcherInfo(
    val psiClass: CjTypeStatement?,
    val containsTypeOrDerivedInside: ((CjDeclaration) -> Boolean)
)
interface CangJieSearchUsagesSupport {
    object SearchUtils {
        fun PsiElement.getReceiverTypeSearcherInfo( ): ReceiverTypeSearcherInfo? =
            getInstance(project).getReceiverTypeSearcherInfo(this )
        fun CjFile.forceResolveReferences(elements: List<CjElement>) =
            getInstance(project).forceResolveReferences(this, elements)
        fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement> =
            getInstance(method.project).findSuperMethodsNoWrapping(method, true)

    }
    fun forceResolveReferences(file: CjFile, elements: List<CjElement>)
    fun findSuperMethodsNoWrapping(method: PsiElement, deepest: Boolean): List<PsiElement>

    /**
     *
     * Extract the PSI class for the receiver type of [psiElement] assuming it is an _operator_.
     * Additionally compute an occurence check for uses of the type in another, used to
     * conservatively discard search candidates in which the type does not occur at all.
     *
     * TODO: rename to something more apt? The FE1.0 implementation requires that the target
     *       be an operator.
     */
    fun getReceiverTypeSearcherInfo(psiElement: PsiElement ): ReceiverTypeSearcherInfo?

    companion object {
        fun getInstance(project: Project): CangJieSearchUsagesSupport = project.service()
    }
}
