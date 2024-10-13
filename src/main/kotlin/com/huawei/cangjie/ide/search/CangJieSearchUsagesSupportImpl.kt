package com.huawei.cangjie.ide.search


import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
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
      com.huawei.cangjie.ide.search.declarationsSearch.findSuperMethodsNoWrapping(method, deepest)

}
