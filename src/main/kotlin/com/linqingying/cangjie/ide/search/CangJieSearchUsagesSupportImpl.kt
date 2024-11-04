package com.linqingying.cangjie.ide.search


import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.intellij.psi.PsiElement

class CangJieSearchUsagesSupportImpl : CangJieSearchUsagesSupport {
    override fun getReceiverTypeSearcherInfo(
        psiElement: PsiElement,

        ): ReceiverTypeSearcherInfo? {

        return  psiElement.getReceiverTypeSearcherInfo()
    }
    override fun forceResolveReferences(file: CjFile, elements: List<CjElement>) =
        file.forceResolveReferences(elements)
    override fun findSuperMethodsNoWrapping(method: PsiElement, deepest: Boolean): List<PsiElement> =
      com.linqingying.cangjie.ide.search.declarationsSearch.findSuperMethodsNoWrapping(method, deepest)

}
