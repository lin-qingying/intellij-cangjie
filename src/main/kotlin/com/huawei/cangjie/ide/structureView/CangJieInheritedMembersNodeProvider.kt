package com.huawei.cangjie.ide.structureView

import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.ide.util.InheritedMembersNodeProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.psi.NavigatablePsiElement


class CangJieInheritedMembersNodeProvider : InheritedMembersNodeProvider<TreeElement>() {
    override fun provideNodes(node: TreeElement): Collection<TreeElement> {
        if (node !is CangJieStructureViewElement) return listOf()

        val element = node.element as? CjTypeStatement ?: return listOf()

        val project = element.project

        val descriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return listOf()

        val children = ArrayList<TreeElement>()

        val defaultType = descriptor.defaultType
        for (memberDescriptor in defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor !is CallableMemberDescriptor) continue

            when (memberDescriptor.kind) {
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                CallableMemberDescriptor.Kind.DELEGATION -> {
                    val superTypeMember = DescriptorToSourceUtilsIde.getAnyDeclaration(project, memberDescriptor)
                    if (superTypeMember is NavigatablePsiElement) {
                        children.add(CangJieStructureViewElement(superTypeMember, memberDescriptor, true))
                    }
                }
                CallableMemberDescriptor.Kind.DECLARATION -> Unit /* Don't show */
                CallableMemberDescriptor.Kind.SYNTHESIZED -> Unit /* Don't show */
            }
        }

        return children
    }
}
