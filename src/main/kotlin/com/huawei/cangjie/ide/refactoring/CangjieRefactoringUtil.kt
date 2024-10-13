package com.huawei.cangjie.ide.refactoring

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.impl.LocalVariableDescriptor
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.psiUtil.isIdentifier
import com.huawei.cangjie.psi.psiUtil.quoteIfNeeded
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.psi.PsiElement
import java.util.HashMap

fun FqName.hasIdentifiersOnly(): Boolean = pathSegments().all { it.asString().quoteIfNeeded().isIdentifier() }
fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?): List<PsiElement> {
    if (!declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) return listOf(declaration)
    val (_, overriddenElementsToDescriptor) = getSuperDescriptors(declaration, ignore)
    return if (overriddenElementsToDescriptor.isEmpty()) listOf(declaration) else overriddenElementsToDescriptor.keys.toList()
}

private fun getSuperDescriptors(
    declaration: CjDeclaration,
    ignore: Collection<PsiElement>?
): Pair<CallableDescriptor, Map<PsiElement, CallableDescriptor>> {
    val progressTitle = CangJieBundle.message("find.usages.progress.text.declaration.superMethods")
    return ActionUtil.underModalProgress(declaration.project, progressTitle) {
        val declarationDescriptor = declaration.unsafeResolveToDescriptor() as CallableDescriptor

        if (declarationDescriptor is LocalVariableDescriptor) {
            return@underModalProgress (declarationDescriptor to emptyMap<PsiElement, CallableDescriptor>())
        }

        val overriddenElementsToDescriptor = HashMap<PsiElement, CallableDescriptor>()
        for (overriddenDescriptor in DescriptorUtils.getAllOverriddenDescriptors(declarationDescriptor)) {
            val overriddenDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(
                declaration.project,
                overriddenDescriptor
            ) ?: continue
            if (overriddenDeclaration is CjNamedFunction
                || overriddenDeclaration is CjProperty

                || overriddenDeclaration is CjParameter
            ) {
                overriddenElementsToDescriptor[overriddenDeclaration] = overriddenDescriptor
            }
        }

        if (ignore != null) {
            overriddenElementsToDescriptor.keys.removeAll(ignore)
        }

        return@underModalProgress (declarationDescriptor to overriddenElementsToDescriptor)
    }
}
