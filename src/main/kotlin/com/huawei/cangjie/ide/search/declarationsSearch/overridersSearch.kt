package com.huawei.cangjie.ide.search.declarationsSearch

import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.highlighter.unwrapped
import com.huawei.cangjie.psi.CjCallableDeclaration
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.getDirectlyOverriddenDeclarations
import com.huawei.cangjie.resolve.source.getPsi
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
