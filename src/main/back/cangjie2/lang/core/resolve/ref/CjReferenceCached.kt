package com.huawei.cangjie.lang.core.resolve.ref

import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.huawei.cangjie.lang.core.psi.ext.CjReferenceElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult


abstract class CjReferenceCached<T : CjReferenceElement>(
    element: T
) : CjReferenceBase<T>(element) {

    protected abstract fun resolveInner(): List<CjElement>

    final override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        cachedMultiResolve().toTypedArray()

    final override fun multiResolve(): List<CjElement> =
        cachedMultiResolve().mapNotNull { it.element as? CjElement }

    private fun cachedMultiResolve(): List<PsiElementResolveResult> {
        return CjResolveCache.getInstance(element.project)
            .resolveWithCaching(element, cacheDependency, Resolver).orEmpty()
    }

    protected open val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_CANGJIE_STRUCTURE

    private object Resolver : (CjReferenceElement) -> List<PsiElementResolveResult> {
        override fun invoke(ref: CjReferenceElement): List<PsiElementResolveResult> {
            return (ref.reference as CjReferenceCached<*>).resolveInner().map { PsiElementResolveResult(it) }
        }
    }
}
