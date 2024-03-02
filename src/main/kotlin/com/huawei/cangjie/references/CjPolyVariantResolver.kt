//package com.huawei.cangjie.references
//
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiElementResolveResult
//import com.intellij.psi.ResolveResult
//import com.intellij.psi.impl.source.resolve.ResolveCache
//
//
//object CjPolyVariantResolver : ResolveCache.PolyVariantResolver<CjReference> {
//
//    class CangJieResolveResult(element: PsiElement) : PsiElementResolveResult(element)
//
//
//    override fun resolve(ref: CjReference, incompleteCode: Boolean): Array<ResolveResult> {
//        TODO()
//
////         val a = CangJieResolveResult(ref.element)
////
////return arrayOf(a)
////
////        val resolveToPsiElements = resolveToPsiElements(ref)
////        return resolveToPsiElements.map { CangJieResolveResult(it) }.toTypedArray()
//    }
//
//}
