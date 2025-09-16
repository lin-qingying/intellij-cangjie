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

package org.cangnova.cangjie.builtins.builtinstype

import org.cangnova.cangjie.builtins.BuiltinsType
import org.cangnova.cangjie.builtins.StandardNames.STD_CORE_PACKAGE_FQ_NAME
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.AbstractClassDescriptor
import org.cangnova.cangjie.descriptors.impl.BuiltinsClassDescriptor
import org.cangnova.cangjie.descriptors.impl.CFunctionClassDescriptor
import org.cangnova.cangjie.descriptors.impl.PackageFragmentDescriptorImpl
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.utils.Printer

/**
 * BasicTypesPackageFragmentProvider 负责提供基本类型（原始类型）的包片段。
 * 这个类集中管理所有基本类型的创建和访问，将基本类型的逻辑从 CangJieBuiltIns 中分离出来。
 */
class BuiltinsTypesPackageFragmentProvider(
    private val storageManager: StorageManager,
    private val builtInsModule: ModuleDescriptor
) : PackageFragmentProvider {

    private val builtinsClassDescriptors = mutableMapOf<BuiltinsType, AbstractClassDescriptor>()

    // 基本包片段，用于承载所有的基本类型
    private val builtinsTypesPackageFragment: PackageFragmentDescriptor by lazy {
        createBuiltinsTypesPackageFragment()
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return if (fqName == STD_CORE_PACKAGE_FQ_NAME) {
            listOf(builtinsTypesPackageFragment)
        } else {
            emptyList()
        }
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return emptySet()
    }

    private fun createBuiltinsTypesPackageFragment(): PackageFragmentDescriptor {
        return BuiltinsTypesPackageFragmentDescriptor(builtInsModule, this)
    }

    fun createBuiltinsClassDescriptor(type: BuiltinsType): AbstractClassDescriptor {
        return builtinsClassDescriptors.getOrPut(type) {
            if (BuiltinsType.CFUNC == type) {
                CFunctionClassDescriptor(
                    storageManager, builtInsModule
                )
            } else
                BuiltinsClassDescriptor(type, storageManager, builtInsModule, builtInsModule.builtIns)
        }
    }

    // 基本类型访问器
    val cpointertype: SimpleType get() = createBuiltinsClassDescriptor(BuiltinsType.CPOINTER).defaultType
    val cstringtype: SimpleType get() = createBuiltinsClassDescriptor(BuiltinsType.CSTRING).defaultType
    val cunctype: SimpleType get() = createBuiltinsClassDescriptor(BuiltinsType.CFUNC).defaultType


}

/**
 * 基本类型的包片段描述符
 */
private class BuiltinsTypesPackageFragmentDescriptor(
    module: ModuleDescriptor,
    private val provider: BuiltinsTypesPackageFragmentProvider
) : PackageFragmentDescriptorImpl(module, STD_CORE_PACKAGE_FQ_NAME) {
    override fun getMemberScope(): MemberScope {
        return _memberScope
    }

    override val containingDeclaration: ModuleDescriptor = module

    private val _memberScope by lazy {
        BuiltinsTypesMemberScope(this, provider)
    }


    override val source: SourceElement = SourceElement.NO_SOURCE
    override val original: DeclarationDescriptorWithSource = this
    override val name: Name = Name.identifier("")
}

/**
 * 内置类型的成员作用域
 */
private class BuiltinsTypesMemberScope(
    private val packageFragment: PackageFragmentDescriptor,
    private val provider: BuiltinsTypesPackageFragmentProvider
) : MemberScope {

    private val allBuiltinsTypes = BuiltinsType.values().toList()


    override fun getContributedClassifier(
        name: Name,
        location: LookupLocation
    ): ClassifierDescriptor? {
        val builtinsType = allBuiltinsTypes.find { it.typeName.asString() == name.asString() }
        return builtinsType?.let { provider.createBuiltinsClassDescriptor(it) }
    }


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            return allBuiltinsTypes
                .filter { nameFilter(it.typeName) }
                .map { provider.createBuiltinsClassDescriptor(it) }
        }
        return emptyList()
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        return emptyList()
    }

    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PropertyDescriptor> {
        return emptyList()
    }

    override val functionNames: Set<Name> = emptySet()
    override val variableNames: Set<Name> = emptySet()
    override val classifierNames: Set<Name> = allBuiltinsTypes.map { it.typeName }.toSet()
    override val propertyNames: Set<Name> = emptySet()

    override fun getContributedFunctions(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard SimpleFunctionDescriptor> {
        return emptyList()
    }

    override fun printScopeStructure(p: Printer) {

    }
}