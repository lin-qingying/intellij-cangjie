/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.utils.Printer

class InstanceMemberScope(private val memberScope: MemberScope) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return memberScope.getContributedVariables(name, location).filter { !it.isStatic }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return memberScope.getContributedPropertys(name, location).filter { !it.isStatic }
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
    }

    override val functionNames: Set<Name>
        get() = memberScope.functionNames

    override val variableNames: Set<Name>
        get() = memberScope.variableNames

    override val classifierNames: Set<Name>?
        get() = memberScope.classifierNames

    override val propertyNames: Set<Name>
        get() = memberScope.propertyNames

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return memberScope.getContributedFunctions(name, location).filter { !it.isStatic } // 过滤非静态函数
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InstanceMemberScope:") // 打印作用域结构
        memberScope.printScopeStructure(p) // 打印基础成员作用域的结构
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return memberScope.getContributedClassifier(name, location)?.takeIf { !it.isStatic } // 过滤非静态分类器
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return memberScope.getContributedDescriptors(kindFilter, nameFilter).filter { !it.isStatic } // 过滤非静态描述符
    }
}

