package com.linqingying.cangjie.ide.refactoring

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.impl.LocalVariableDescriptor
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.psi.psiUtil.isIdentifier
import com.linqingying.cangjie.psi.psiUtil.quoteIfNeeded
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.caches.unsafeResolveToDescriptor
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
