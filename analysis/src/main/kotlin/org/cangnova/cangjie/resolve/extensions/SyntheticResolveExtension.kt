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
 */

package org.cangnova.cangjie.resolve.extensions

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.types.CangJieType

/**
 * 合成解析扩展点，允许插件向解析器注入虚拟（合成）的声明描述符。
 *
 * 方法分为两组：
 * - **类成员级别**：向某个 [ClassDescriptor] 的成员作用域注入嵌套类、方法、属性等。
 * - **包级别**：向某个 [PackageFragmentDescriptor] 的顶层作用域注入类、枚举、函数、变量等。
 *
 * 当注册了多个实现时，由 [CompositeSyntheticResolveExtension] 自动合并所有结果。
 */
interface SyntheticResolveExtension {

    companion object {
        val EP_NAME = ExtensionPointName<SyntheticResolveExtension>(
            "org.cangnova.cangjie.syntheticResolveExtension"
        )

        fun getInstance(project: Project): SyntheticResolveExtension {
            val instances = EP_NAME.extensionList
            return if (instances.size == 1) instances.single()
            else CompositeSyntheticResolveExtension(instances)
        }
    }

    // =========================================================================
    // 类成员级别
    // =========================================================================

    /** 返回该类的合成嵌套类名字列表，供解析器提前知晓需要查询哪些名字。 */
    fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> = emptyList()

    /**
     * 返回合成嵌套类名字的超集，或 null（表示成本过高，回退到完整解析）。
     * 在递归解析时可覆盖此方法以避免无限递归。
     */
    fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? =
        getSyntheticNestedClassNames(thisDescriptor)

    /** 返回该类的合成成员函数名字列表。 */
    fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> = emptyList()

    /** 返回该类的合成属性名字列表。 */
    fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> = emptyList()

    /** 返回该类需要合成的 companion object 名字，无则返回 null。 */
    fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? = null

    /** 向该类的父类型列表注入合成超类型。 */
    fun addSyntheticSupertypes(thisDescriptor: ClassDescriptor, supertypes: MutableList<CangJieType>) {}

    /** 向该类注入合成嵌套类描述符。 */
    fun generateSyntheticNestedClasses(
        thisDescriptor: ClassDescriptor, name: Name,
        ctx: LazyClassContext, declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {}

    /** 向该类注入合成成员函数描述符。 */
    fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor, name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {}

    /** 向该类注入合成属性描述符。 */
    fun generateSyntheticProperties(
        thisDescriptor: ClassAndEnumDescriptor, name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {}

    /** 向该类注入合成次级构造函数描述符。 */
    fun generateSyntheticSecondaryConstructors(
        thisDescriptor: ClassDescriptor,
        bindingContext: BindingContext,
        result: MutableCollection<ClassConstructorDescriptor>
    ) {}

    // =========================================================================
    // 包级别
    // =========================================================================

    /**
     * 返回该包片段的所有合成顶层声明名字集合。
     *
     * 用于 `getContributedDescriptors` 全量枚举，调用者需提供 [declarationProvider]
     * 以便实现方获取源文件（[PackageFragmentDescriptor.source] 固定为 NO_SOURCE）。
     */
    fun getSyntheticPackageNames(
        thisDescriptor: PackageFragmentDescriptor,
        declarationProvider: PackageMemberDeclarationProvider
    ): PackageSyntheticNames = PackageSyntheticNames.EMPTY

    /** 向该包注入合成顶层 class/struct/interface 描述符。 */
    fun generateSyntheticTopLevelClasses(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {}

    /** 向该包注入合成顶层 enum 描述符。 */
    fun generateSyntheticEnums(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<EnumDescriptor>
    ) {}

    /** 向该包注入合成顶层函数描述符。 */
    fun generateSyntheticFunctions(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<SimpleFunctionDescriptor>
    ) {}

    /** 向该包注入合成顶层变量描述符（let/var）。 */
    fun generateSyntheticVariables(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<VariableDescriptor>
    ) {}

    // =========================================================================
    // 辅助类型
    // =========================================================================

    /** 包级别合成声明的名字集合，用于 [getSyntheticPackageNames] 返回值。 */
    data class PackageSyntheticNames(
        val functionNames: Set<Name>,
        val variableNames: Set<Name>,
        /** class / struct / interface / enum 统一放这里 */
        val classifierNames: Set<Name>
    ) {
        fun isEmpty(): Boolean =
            functionNames.isEmpty() && variableNames.isEmpty() && classifierNames.isEmpty()

        fun merge(other: PackageSyntheticNames): PackageSyntheticNames {
            if (other.isEmpty()) return this
            if (isEmpty()) return other
            return PackageSyntheticNames(
                functionNames = functionNames + other.functionNames,
                variableNames = variableNames + other.variableNames,
                classifierNames = classifierNames + other.classifierNames
            )
        }

        companion object {
            val EMPTY = PackageSyntheticNames(emptySet(), emptySet(), emptySet())
        }
    }
}