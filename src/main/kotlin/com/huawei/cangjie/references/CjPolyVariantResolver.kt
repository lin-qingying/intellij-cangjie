package com.huawei.cangjie.references

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import java.util.*


object CjPolyVariantResolver : ResolveCache.PolyVariantResolver<CjReference> {

    class CangJieResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    val result = mutableListOf<CjElement>()


    override fun resolve(ref: CjReference, incompleteCode: Boolean): Array<ResolveResult> {
        val project = ref.element.project


        val virtualFiles =
            FileTypeIndex.getFiles(CangJieFileType, GlobalSearchScope.allScope(ref.element.project))
        for (virtualFile in virtualFiles) {
            val cjFile: CjFile? = PsiManager.getInstance(project).findFile(virtualFile!!) as CjFile?
            if (cjFile != null) {
                val properties = PsiTreeUtil.getChildrenOfType(
                    cjFile,
                    CjElement::class.java
                )
                if (properties != null) {
                    Collections.addAll(result, *properties)
                }
            }
        }

//        TODO()

//         val a = CangJieResolveResult(ref.element)

//        val resolveElement = result.filter { it.name == ref.resolvesByNames.toString() }.map {
//            CangJieResolveResult(it)
//        }

//        return resolveElement.toTypedArray()
//
return arrayOf()
//
//        val resolveToPsiElements = resolveToPsiElements(ref)
//        return resolveToPsiElements.map { CangJieResolveResult(it) }.toTypedArray()
    }

}
