/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */
package org.cangnova.cangjie.references

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.cangnova.cangjie.psi.CangJiePsiFacade
import org.cangnova.cangjie.resolve.binding.BindingContext
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

        val bindingContext = CjReferenceResolutionHelper.getInstance().partialAnalyze(ref.expression)
        if (bindingContext == BindingContext.EMPTY) return emptySet()
        return resolveToPsiElements(ref, bindingContext, ref.getTargetDescriptors(bindingContext))
    }

    override fun resolve(ref: CjReference, incompleteCode: Boolean): Array<ResolveResult> {


     val resolveToPsiElements = resolveToPsiElements(ref)
        return resolveToPsiElements.map { CangJieResolveResult(it) }.toTypedArray()

    }

}
