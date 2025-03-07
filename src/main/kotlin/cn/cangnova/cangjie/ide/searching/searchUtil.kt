/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.ide.searching

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.descriptors.ConstructorDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.ide.useScope
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.DescriptorToSourceUtils
import cn.cangnova.cangjie.resolve.caches.analyze
import cn.cangnova.cangjie.resolve.caches.descriptor
import cn.cangnova.cangjie.utils.CangJiePsiDeclarationRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.concurrency.ThreadingAssertions

fun ReferencesSearch.SearchParameters.effectiveSearchScope(element: PsiElement): SearchScope {
    if (element == elementToSearch) return effectiveSearchScope
    if (isIgnoreAccessScope) return scopeDeterminedByUser
    val accessScope = element.useScope()
    return scopeDeterminedByUser.intersectWith(accessScope)
}

fun isOnlyCangJieSearch(searchScope: SearchScope): Boolean {
    return searchScope is LocalSearchScope && searchScope.scope.all { it.containingFile is CjFile }
}

private fun CjElement.getConstructorCallDescriptor(): DeclarationDescriptor? {
    val bindingContext = this.analyze()
    val constructorCalleeExpression = getNonStrictParentOfType<CjConstructorCalleeExpression>()
    if (constructorCalleeExpression != null) {
        return bindingContext.get(BindingContext.REFERENCE_TARGET, constructorCalleeExpression.constructorReferenceExpression)
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
        CangJieBundle.message("find.usages.prepare.dialog.progress")
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
