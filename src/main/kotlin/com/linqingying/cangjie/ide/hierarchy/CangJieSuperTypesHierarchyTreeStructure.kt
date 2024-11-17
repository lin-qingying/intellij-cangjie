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

import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
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
