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

package org.cangnova.cangjie.resolve

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.utils.Printer

/**
 * 全通配导入作用域
 *
 * 处理 `import foo.bar.*` 形式的导入，提供对包或类中所有符号的访问。
 *
 * ## 支持的导入目标
 *
 * - **包**: `import std.core.*` - 导入包中的所有声明
 * - **类**: `import MyClass.*` - 导入类的静态成员
 *
 * ## 重导出支持
 *
 * 当导入一个包时，该包的重导出声明也会被包含。
 * 例如，如果 `foo` 包有 `public import std.core.String`，
 * 那么 `import foo.*` 也能访问到 `String`。
 *
 * @property scope1 主作用域（包的成员作用域或类的静态作用域）
 * @property scope2 次作用域（类的实例成员作用域，仅对类有效）
 *
 * @see PackageReexportScope 重导出作用域的实现
 */
class AllUnderImportScope private constructor(
    descriptor: DeclarationDescriptor,
    excludedImportNames: Collection<FqName>,
    private val scope1: MemberScope,
    private val scope2: MemberScope?,
) : BaseImportingScope(null) {
    private val excludedNames = if (excludedImportNames.isEmpty()) { // optimization
        emptySet()
    } else {
        val fqName = DescriptorUtils.getFqNameSafe(descriptor)
        // toSet() is used here instead mapNotNullTo(hashSetOf()) because it results in not keeping empty sets as separate instances
        excludedImportNames.mapNotNull { if (it.parent() == fqName) it.shortName() else null }.toSet()
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        val nameFilterToUse = if (excludedNames.isEmpty()) { // optimization
            nameFilter
        } else {
            { it !in excludedNames && nameFilter(it) }
        }

        val noPackagesKindFilter = kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
        val result = SmartList<DeclarationDescriptor>()
        forEachScope(scope1, scope2) { scope ->
            scope.getContributedDescriptors(noPackagesKindFilter, nameFilterToUse)
                .filterTo(result) { it !is PackageViewDescriptor }
        }

        return result
    }

    override fun computeImportedNames(): Set<Name>? {
        val names1 = scope1.computeAllNames()
        val names2 = scope2?.computeAllNames()

        return when {
            names1 == null -> null
            scope2 == null   -> names1
            else -> {
                val result = names1.toMutableSet()
                names2?.let { result.addAll(it) }
                result
            }
        }
    }


    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        if (name in excludedNames) return emptyList()
        val classifier1 = scope1.getContributedClassifiers(name, location)
        val classifier2 = scope2?.getContributedClassifiers(name, location)
        return classifier1 + (classifier2 ?: emptyList())
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name in excludedNames) return null
        val classifier1 = scope1.getContributedClassifier(name, location)
        val classifier2 = scope2?.getContributedClassifier(name, location)

        val nonNullClassifiers = listOfNotNull(classifier1, classifier2)
        return when {
            nonNullClassifiers.isEmpty() -> null
            nonNullClassifiers.size == 1 -> nonNullClassifiers.first()
            else -> null // ambiguity
        }
    }


    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        if (name in excludedNames) return emptyList()
        val result = flatMapScopes(scope1, scope2) { it.getContributedVariables(name, location) }
        return result
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name in excludedNames) return emptyList()
        val result = flatMapScopes(scope1, scope2) { it.getContributedFunctions(name, location) }
        return result
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        if (name in excludedNames) return emptyList()
        val result = flatMapScopes(scope1, scope2) { it.getContributedMacros(name, location) }
        return result
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (name in excludedNames) return emptyList()
        val result = flatMapScopes(scope1, scope2) { it.getContributedPropertys(name, location) }
        return result
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        scope1.recordLookup(name, location)
        scope2?.recordLookup(name, location)
    }

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName)
    }


    companion object {


        /**
         * 创建包含重导出支持的全通配导入作用域
         *
         * @param descriptor 导入目标（包或类）
         * @param excludedImportNames 要排除的导入名称
         * @return 导入作用域
         */
        fun create(
            descriptor: DeclarationDescriptor,
            excludedImportNames: Collection<FqName>,

        ): ImportingScope {
            val scope1 =
                if (descriptor is ClassDescriptor) {
                    descriptor.staticScope
                } else {
                    assert(descriptor is PackageViewDescriptor) {
                        "Must be class or package view descriptor: $descriptor"
                    }
                    (descriptor as PackageViewDescriptor).memberScope
                }

            val scope2 =
                if (descriptor is ClassDescriptor) {
                    descriptor.unsubstitutedMemberScope.takeIf { it !== MemberScope.Empty }
                } else null


            return if (scope1 === MemberScope.Empty) {
                if (scope2 == null || scope2 === MemberScope.Empty) {

                    ImportingScope.Empty
                } else {
                    AllUnderImportScope(descriptor, excludedImportNames, scope2, null)
                }
            } else {
                AllUnderImportScope(descriptor, excludedImportNames, scope1, scope2)
            }
        }
    }
}
