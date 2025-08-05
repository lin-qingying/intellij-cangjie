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

package cn.cangnova.cangjie.resolve.scopes

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.macro.MacroDescriptor
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.utils.Printer

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

    override fun getFunctionNames(): Set<Name> {
        return memberScope.getFunctionNames() // 返回所有函数名称

            .toSet()
    }

    override fun getVariableNames(): Set<Name> {
        return memberScope.getVariableNames() // 返回所有变量名称

            .toSet()
    }

    override fun getClassifierNames(): Set<Name>? {
        return memberScope.getClassifierNames() // 返回所有分类器名称
    }

    override fun getPropertyNames(): Set<Name> {
        return memberScope.getPropertyNames() // 返回所有属性名称

            .toSet()
    }

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

class StaticMemberScope(val memberScope: MemberScope) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return memberScope.getContributedVariables(name, location).filter { it.isStatic }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return memberScope.getContributedPropertys(name, location).filter { it.isStatic }
    }

    override fun getFunctionNames(): Set<Name> {
        return memberScope.getFunctionNames() // 返回所有函数名称

            .toSet()
    }

    override fun getVariableNames(): Set<Name> {
        return memberScope.getVariableNames() // 返回所有变量名称

            .toSet()
    }

    override fun getClassifierNames(): Set<Name>? {
        return memberScope.getClassifierNames() // 返回所有分类器名称
    }

    override fun getPropertyNames(): Set<Name> {
        return memberScope.getPropertyNames() // 返回所有属性名称

            .toSet()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return memberScope.getContributedFunctions(name, location).filter { it.isStatic } // 过滤非静态函数
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InstanceMemberScope:") // 打印作用域结构
        memberScope.printScopeStructure(p) // 打印基础成员作用域的结构
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return memberScope.getContributedClassifier(name, location)
            ?.takeIf { it.isStatic || DescriptorUtils.isEnumEntry(it) } // 过滤非静态分类器
    }

    override fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return memberScope.getContributedEnumEntrys(name, location).filter {
            it.isStatic || DescriptorUtils.isEnumEntry(it)
        }

    }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        return memberScope.getContributedClassifiers(name, location).filter {
            it.isStatic || DescriptorUtils.isEnumEntry(it)
        }

    }


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return memberScope.getContributedDescriptors(kindFilter, nameFilter).filter { it.isStatic } // 过滤非静态描述符
    }
}