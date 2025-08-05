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

package cn.cangnova.cangjie.resolve.scopes

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.NonReportingOverrideStrategy
import cn.cangnova.cangjie.resolve.OverridingUtil
import cn.cangnova.cangjie.storage.StorageManager

import cn.cangnova.cangjie.storage.getValue
import cn.cangnova.cangjie.utils.compact
import cn.cangnova.cangjie.utils.Printer
import cn.cangnova.cangjie.utils.filterIsInstanceAnd

/**
 * 一个可能包含一些声明函数以及来自超类型的伪重写函数/属性的作用域。
 *
 * 此类为抽象类，负责定义一个作用域（Scope），
 * 该作用域既包括当前类声明的函数，还可以通过继承机制合成伪重写的成员。
 *
 * @constructor
 * @param storageManager 用于延迟计算和缓存的存储管理器。
 * @param containingClass 当前作用域所关联的类描述符（ClassDescriptor）。
 */
abstract class GivenFunctionsMemberScope(
    storageManager: StorageManager,
    protected val containingClass: ClassDescriptor
) : MemberScopeImpl() {

    /**
     * 所有成员描述符的集合，通过延迟计算和缓存实现。
     * 包含当前类的声明函数以及伪重写的成员。
     */
    private val allDescriptors by storageManager.createLazyValue {
        val fromCurrent = computeDeclaredFunctions() // 获取当前类声明的函数
        fromCurrent + createFakeOverrides(fromCurrent) // 合并伪重写的成员
    }

    /**
     * 抽象方法，用于计算当前类中声明的所有函数。
     * 子类需要实现此方法。
     *
     * @return 当前类中声明的函数列表。
     */
    protected abstract fun computeDeclaredFunctions(): List<FunctionDescriptor>

    /**
     * 获取作用域中贡献的描述符（成员），根据指定的过滤器和名称过滤器。
     *
     * @param kindFilter 描述符的种类过滤器（函数或变量等）。
     * @param nameFilter 名称过滤器，用于筛选目标成员。
     * @return 满足条件的成员描述符集合。
     */
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.CALLABLES.kindMask)) return listOf()
        return allDescriptors
    }

    /**
     * 根据名称和查找位置获取作用域中的函数。
     *
     * @param name 函数名称。
     * @param location 查找位置。
     * @return 满足条件的函数描述符集合。
     */
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return allDescriptors.filterIsInstanceAnd { it.name == name }
    }

    /**
     * 根据名称和查找位置获取作用域中的变量。
     *
     * @param name 变量名称。
     * @param location 查找位置。
     * @return 满足条件的变量描述符集合。
     */
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        return allDescriptors.filterIsInstanceAnd { it.name == name }
    }

    /**
     * 创建伪重写成员（Fake Overrides）。
     *
     * @param functionsFromCurrent 当前类中声明的函数。
     * @return 包含伪重写成员的描述符集合。
     */
    private fun createFakeOverrides(functionsFromCurrent: List<FunctionDescriptor>): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>(3)

        // 获取所有超类型的成员
        val allSuperDescriptors = containingClass.typeConstructor.supertypes
            .flatMap { it.memberScope.getContributedDescriptors() }
            .filterIsInstance<CallableMemberDescriptor>()

        // 按名称分组处理重写逻辑
        for ((name, group) in allSuperDescriptors.groupBy { it.name }) {
            for ((isFunction, descriptors) in group.groupBy { it is FunctionDescriptor }) {
                OverridingUtil.DEFAULT.generateOverridesInFunctionGroup(
                    name,
                    descriptors, // 超类型中的成员
                    if (isFunction) functionsFromCurrent.filter { it.name == name } else listOf(), // 当前类的函数
                    containingClass, // 当前类
                    object : NonReportingOverrideStrategy() {
                        override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                            // 为生成的伪重写成员解析未知可见性
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                            result.add(fakeOverride)
                        }

                        override fun conflict(
                            fromSuper: CallableMemberDescriptor,
                            fromCurrent: CallableMemberDescriptor
                        ) {
                            error("Conflict in scope of $containingClass: $fromSuper vs $fromCurrent")
                        }
                    }
                )
            }
        }

        return result.compact() // 压缩结果集合
    }

    /**
     * 打印作用域的结构，用于调试和日志记录。
     *
     * @param p 打印器对象。
     */
    override fun printScopeStructure(p: Printer) {
        p.println("类的作用域: $containingClass")
    }
}
