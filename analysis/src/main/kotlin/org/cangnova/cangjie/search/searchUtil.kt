/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.search
import org.cangnova.cangjie.messages.CangJieSearchBundle


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.concurrency.ThreadingAssertions
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ConstructorDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.caches.descriptor
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.types.CangJieType

import org.cangnova.cangjie.utils.CangJiePsiDeclarationRenderer
import org.cangnova.cangjie.utils.isExtension


fun ReferencesSearch.SearchParameters.effectiveSearchScope(element: PsiElement): SearchScope {
    if (element == elementToSearch) return effectiveSearchScope
    if (isIgnoreAccessScope) return scopeDeterminedByUser
    val accessScope = element.useScope
    return scopeDeterminedByUser.intersectWith(accessScope)
}

fun isOnlyCangJieSearch(searchScope: SearchScope): Boolean {
    return searchScope is LocalSearchScope && searchScope.scope.all { it.containingFile is CjFile }
}

private fun CjElement.getConstructorCallDescriptor(): DeclarationDescriptor? {
    val bindingContext = this.analyze()
    val constructorCalleeExpression = getNonStrictParentOfType<CjConstructorCalleeExpression>()
    if (constructorCalleeExpression != null) {
        constructorCalleeExpression.constructorReferenceExpression?.let{
            return bindingContext.get(BindingContext.REFERENCE_TARGET, it)

        }
    }

    val callExpression = getNonStrictParentOfType<CjCallElement>()
    if (callExpression != null) {
        val callee = callExpression.calleeExpression
        if (callee is CjReferenceExpression) {
            return bindingContext.get(BindingContext.REFERENCE_TARGET, callee)
        }
    }

    return null
}
fun PsiReference.isCangJieConstructorUsage(cjClassOrObject: CjTypeStatement): Boolean = with(element) {
    if (this !is CjElement) return false

    val descriptor = getConstructorCallDescriptor() as? ConstructorDescriptor ?: return false

    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.containingDeclaration)
    return declaration == cjClassOrObject || (declaration is CjConstructor<*> && declaration.containingTypeStatement == cjClassOrObject)
}
fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String? =
    CangJiePsiDeclarationRenderer.render(declaration) ?: calculateInModalWindow(
        declaration,
        CangJieSearchBundle.message("find.usages.prepare.dialog.progress")
    ) { declaration.descriptor?.let { DescriptorRenderer.COMPACT.render(it) } }

inline fun <R> calculateInModalWindow(
    contextElement: PsiElement,
    @NlsContexts.DialogTitle windowTitle: String,
    crossinline action: () -> R
): R {
    ThreadingAssertions.assertEventDispatchThread()
    val task = object : Task.WithResult<R, Exception>(contextElement.project, windowTitle, /*canBeCancelled*/ true) {
        override fun compute(indicator: ProgressIndicator): R =
            ApplicationManager.getApplication().runReadAction(Computable { action() })
    }
    task.queue()
    return task.result
}

fun PsiReference.isImportUsage(): Boolean =
    element.getNonStrictParentOfType<CjImportItem>() != null

fun PsiElement.getReceiverTypeSearcherInfo(): ReceiverTypeSearcherInfo? {
TODO()
}

fun CjFile.forceResolveReferences(elements: List<CjElement>) {
    getResolutionFacade().analyze(elements, BodyResolveMode.PARTIAL)
}




fun FunctionDescriptor.isValidOperator() = isOperator /*&& OperatorChecks.check(this).isSuccess*/
private fun PsiElement.resolveTargetToDescriptor( ): FunctionDescriptor? {



    return when {
        this is CjDeclaration -> resolveToDescriptorIfAny(BodyResolveMode.FULL)

        else -> null
    } as? FunctionDescriptor
}


