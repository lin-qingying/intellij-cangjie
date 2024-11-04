package com.linqingying.cangjie.references

import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.PackageViewDescriptor
import com.linqingying.cangjie.psi.CangJiePsiFacade
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjReferenceExpression
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.BindingContext
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


        return if (targetDescriptor is PackageViewDescriptor) {
            val psiFacade = CangJiePsiFacade.getInstance(ref.element.project)
            val fqName = targetDescriptor.fqName
            listOfNotNull(psiFacade.findPackage(fqName))

        } else {
            DescriptorToSourceUtilsIde.getAllDeclarations(
                ref.element.project,
                targetDescriptor,
                ref.element.resolveScope
            )
        }
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
//        val bindingContext = try {
//            CjReferenceResolutionHelper.getInstance().partialAnalyze(ref.expression)
//        } catch (e: Exception) {
////            这里有个异常被抛出，所以直接返回空集合
//            return emptySet()
//        }

        val bindingContext = CjReferenceResolutionHelper.getInstance().partialAnalyze(ref.expression)
        if (bindingContext == BindingContext.EMPTY) return emptySet()
        return resolveToPsiElements(ref, bindingContext, ref.getTargetDescriptors(bindingContext))
    }

    override fun resolve(ref: CjReference, incompleteCode: Boolean): Array<ResolveResult> {

        if (!CangJieLanguageServerServices.getInstance().astConfig.isFeatureEnabled(Feature.REFERENCES)) {
            return emptyArray()
        }

        val resolveToPsiElements = resolveToPsiElements(ref)
        return resolveToPsiElements.map { CangJieResolveResult(it) }.toTypedArray()

    }

}
