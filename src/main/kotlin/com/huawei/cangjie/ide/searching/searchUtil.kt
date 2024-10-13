package com.huawei.cangjie.ide.searching

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.ConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.ide.useScope
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.containingTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.caches.descriptor
import com.huawei.cangjie.utils.CangJiePsiDeclarationRenderer
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
