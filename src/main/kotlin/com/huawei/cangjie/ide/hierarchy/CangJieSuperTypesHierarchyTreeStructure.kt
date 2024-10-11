package com.huawei.cangjie.ide.hierarchy

import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure

class CangJieSuperTypesHierarchyTreeStructure(cl: CjTypeStatement) :
    HierarchyTreeStructure(cl.getProject(), CangJieHierarchyNodeDescriptor(null, cl, true)) {
    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
        val res: MutableList<CangJieHierarchyNodeDescriptor> = ArrayList<CangJieHierarchyNodeDescriptor>()
        if (descriptor is CangJieHierarchyNodeDescriptor) {
            val element = descriptor.psiElement
            if (element is CjTypeStatement) {
        val classDescriptor = element.resolveToDescriptorIfAny(BodyResolveMode.FULL)
//                for (superClass in element.getSuperTypeList()) {
//                    res.add(CangJieHierarchyNodeDescriptor(descriptor, superClass, false))
//                }

                classDescriptor
            }
        }
        return res.toTypedArray()
    }
}
