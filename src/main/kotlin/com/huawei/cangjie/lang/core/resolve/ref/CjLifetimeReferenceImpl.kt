package com.huawei.cangjie.lang.core.resolve.ref


import com.huawei.cangjie.lang.core.psi.CjLifetime
import com.huawei.cangjie.lang.core.psi.CjLifetimeParameter
import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.huawei.cangjie.lang.core.resolve.collectResolveVariants
import com.huawei.cangjie.lang.core.resolve.processLifetimeResolveVariants
import com.intellij.psi.PsiElement

class CjLifetimeReferenceImpl(
    element: CjLifetime
) : CjReferenceCached<CjLifetime>(element) {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_CANGJIE_STRUCTURE

    override fun resolveInner(): List<CjElement> =
        collectResolveVariants(element.referenceName) { processLifetimeResolveVariants(element, it) }

    override fun isReferenceTo(element: PsiElement): Boolean =
        (element is CjLifetimeParameter || element is CjLifetime) && super.isReferenceTo(element)
}
