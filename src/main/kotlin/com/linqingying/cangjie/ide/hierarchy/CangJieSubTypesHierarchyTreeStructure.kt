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

package com.linqingying.cangjie.ide.hierarchy

import com.linqingying.cangjie.ide.search.ClassInheritorsSearch
import com.linqingying.cangjie.psi.CjTypeStatement
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
