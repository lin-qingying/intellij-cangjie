package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjReferenceExpression
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import java.util.*


object CjPolyVariantResolver : ResolveCache.PolyVariantResolver<CjReference> {

    class CangJieResolveResult(element: PsiElement) : PsiElementResolveResult(element)

    val result = mutableListOf<CjElement>()

    private fun resolveToPsiElements(
        ref: CjReference,
        targetDescriptor: DeclarationDescriptor
    ): Collection<PsiElement> {
        TODO()
    }
    private fun resolveToPsiElements(
        ref: CjReference,
        context: BindingContext,
        targetDescriptors: Collection<DeclarationDescriptor>
    ): Collection<PsiElement> {
        if (targetDescriptors.isNotEmpty()) {
            return targetDescriptors.flatMap { target -> resolveToPsiElements(ref, target) }.toSet()
        }

        val labelTargets = getLabelTargets(ref, context)
        if (labelTargets != null) {
            return labelTargets
        }

        return Collections.emptySet()
    }
    private fun getLabelTargets(ref: CjReference, context: BindingContext): Collection<PsiElement>? {
        val reference = ref.element as? CjReferenceExpression ?: return null
        val labelTarget = context[BindingContext.LABEL_TARGET, reference]
        if (labelTarget != null) {
            return listOf(labelTarget)
        }

        return context[BindingContext.AMBIGUOUS_LABEL_TARGET, reference]
    }
    private fun resolveToPsiElements(ref: CjReference): Collection<PsiElement> {
        require(ref is AbstractCjReference<*>) { "reference should be AbstractCjReference, but was ${ref::class}" }
        val bindingContext = CjReferenceResolutionHelper.getInstance().partialAnalyze(ref.expression)
        if (bindingContext == BindingContext.EMPTY) return emptySet()
        return resolveToPsiElements(ref, bindingContext, ref.getTargetDescriptors(bindingContext))
    }

    override fun resolve(ref: CjReference, incompleteCode: Boolean): Array<ResolveResult> {
        val resolveToPsiElements = resolveToPsiElements(ref)
        return resolveToPsiElements.map { CangJieResolveResult(it) }.toTypedArray()
    }

}
