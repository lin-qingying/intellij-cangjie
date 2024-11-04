package com.linqingying.cangjie.ide.search.declarationsSearch

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.highlighter.unwrapped
import com.linqingying.cangjie.psi.CjCallableDeclaration
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.getDirectlyOverriddenDeclarations
import com.linqingying.cangjie.resolve.source.getPsi
import com.intellij.psi.PsiElement
import com.intellij.util.Query


fun findSuperMethodsNoWrapping(method: PsiElement, deepest: Boolean): List<PsiElement> {
    return when (val element = method.unwrapped) {

        is CjCallableDeclaration -> {
            val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return emptyList()
            val superDeclarations = if (deepest) descriptor.getDeepestSuperDeclarations(false) else descriptor.getDirectlyOverriddenDeclarations()
            superDeclarations.mapNotNull {
                it.source.getPsi() ?: DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it)
            }
        }

        else -> emptyList()
    }
}
fun <D : CallableMemberDescriptor> D.getDeepestSuperDeclarations(withThis: Boolean = true): Collection<D> {
    val overriddenDeclarations = DescriptorUtils.getAllOverriddenDeclarations(this)
    if (overriddenDeclarations.isEmpty() && withThis) {
        return setOf(this)
    }

    return overriddenDeclarations.filterNot(DescriptorUtils::isOverride)
}
