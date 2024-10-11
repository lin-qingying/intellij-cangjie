package com.huawei.cangjie.ide.hierarchy

import com.huawei.cangjie.ide.search.ClassInheritorsSearch
import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.util.ArrayUtil
import com.intellij.util.Query

open class CangJieSubTypesHierarchyTreeStructure : HierarchyTreeStructure {
    protected constructor(project: Project?, baseDescriptor: HierarchyNodeDescriptor?) : super(
        project!!, baseDescriptor
    )

    constructor(cl: CjTypeStatement) : super(cl.getProject(), CangJieHierarchyNodeDescriptor(null, cl, true))

    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
        val res: MutableList<CangJieHierarchyNodeDescriptor> = ArrayList<CangJieHierarchyNodeDescriptor>()
        val element = descriptor.psiElement
        if (element is CjTypeStatement) {
            val subClasses: Query<CjTypeStatement> = ClassInheritorsSearch.search(element, false)
            for (subClass in subClasses) {
                res.add(CangJieHierarchyNodeDescriptor(descriptor, subClass, false))
            }
        }

        return ArrayUtil.toObjectArray(res)
    }
}
