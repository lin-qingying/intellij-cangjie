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

package com.linqingying.cangjie.resolve

import com.intellij.util.SmartList
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.BaseImportingScope
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.utils.CallOnceFunction
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.utils.addIfNotNull

class LazyExplicitImportScope(
    private val languageVersionSettings: LanguageVersionSettings,
    private val packageOrClassDescriptor: DeclarationDescriptor,
    private val packageFragmentForVisibilityCheck: PackageFragmentDescriptor?,
    private val declaredName: Name,
    private val aliasName: Name,
    private val storeReferences: CallOnceFunction<Collection<DeclarationDescriptor>, Unit>
) : BaseImportingScope(null) {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name != aliasName) return null

        return when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> packageOrClassDescriptor.memberScope.getContributedClassifier(
                declaredName,
                location
            )

            is ClassDescriptor -> packageOrClassDescriptor.unsubstitutedInnerClassesScope.getContributedClassifier(
                declaredName,
                location
            )

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
    }

//    override fun getThisDescriptor(): DeclarationDescriptor? {
//        return getContributedClassifier(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS)
//    }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return if (packageOrClassDescriptor is PackageViewDescriptor) {
            packageOrClassDescriptor.memberScope.getExtendClass(
                declaredName,

                )
        } else {
            emptyList()
        }
    }

    override fun getContributedEnumEntrys(name: Name, location: LookupLocation): List<ClassifierDescriptor> {

        return collectEnumEntryDeclarationMemberDescriptors(name,location, MemberScope::getContributedEnumEntrys).toList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedFunctions)
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedMacros)
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        if (name != aliasName) return emptyList()

        return collectCallableMemberDescriptors(location, MemberScope::getContributedVariables)
    }

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {

        return when (packageOrClassDescriptor) {
            is LazyClassDescriptor -> {
                packageOrClassDescriptor.containingDeclaration as? PackageViewDescriptor
            }

            is PackageViewDescriptor -> {

                val packageViewDescriptor = packageOrClassDescriptor.module.getPackage(
                    packageOrClassDescriptor.fqName.child(
                        aliasName
                    )
                )
                if (!packageViewDescriptor.isEmpty()) {
                    packageViewDescriptor
                } else {
                    null
                }


            }


            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        val descriptors = SmartList<DeclarationDescriptor>()

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            descriptors.addIfNotNull(getContributedClassifier(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            descriptors.addAll(getContributedFunctions(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            descriptors.addAll(getContributedVariables(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS))
        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.REEXPORT_MASK)) {
            if (packageOrClassDescriptor is PackageViewDescriptor) {
                descriptors.addAll(
                    packageOrClassDescriptor.getContributedDescriptorsByReexportDirective(
                        languageVersionSettings,
                        packageFragmentForVisibilityCheck,
                        declaredName,
                        aliasName
                    )
                )
            }

        }
        if (kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) {
            getContributedPackage(aliasName)?.let {
                descriptors.add(it)
            }


//            descriptors.addAll(getContributedPackages(aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS)))
        }




        if (changeNamesForAliased && aliasName != declaredName) {
            for (i in descriptors.indices) {
                val newDescriptor: DeclarationDescriptor = when (val descriptor = descriptors[i]) {
                    is ClassDescriptor -> {
                        object : ClassDescriptor by descriptor {

                            override val name: Name = aliasName
                        }
                    }

                    is TypeAliasDescriptor -> {
                        object : TypeAliasDescriptor by descriptor {
                            override val name: Name = aliasName
                        }
                    }

                    is CallableMemberDescriptor -> {
                        descriptor
                            .newCopyBuilder()
                            .setName(aliasName)
                            .setOriginal(descriptor)
                            .build()!!
                    }

                    else -> error("Unknown kind of descriptor in import alias: $descriptor")
                }
                descriptors[i] = newDescriptor
            }
        }

        return descriptors
    }

    override fun computeImportedNames() = setOf(aliasName)

    override fun printStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", aliasName)
    }


    // should be called only once
    internal fun storeReferencesToDescriptors() = getContributedDescriptors().apply(storeReferences)


    /**
     * 收集可调用成员描述符
     *
     * 该函数用于收集给定作用域中所有可见的可调用成员描述符（如函数或属性描述符）
     * 它根据[packageOrClassDescriptor]的类型（包或类描述符）来决定使用哪种作用域进行查找
     *
     * @param location 查找位置，用于调试信息
     * @param getDescriptors 一个高阶函数，用于从成员作用域中获取描述符集合
     * @return 返回收集到的可调用成员描述符集合
     */
    private fun <D : /*CallableMemberDescriptor*/CallableDescriptor> collectCallableMemberDescriptors(
        location: LookupLocation,
        getDescriptors: MemberScope.(Name, LookupLocation) -> Collection<D>
    ): Collection<D> {
        val descriptors = SmartList<D>()

        // 根据packageOrClassDescriptor的类型决定如何收集描述符
        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                // 如果是包描述符，使用成员作用域收集描述符
                val packageScope = packageOrClassDescriptor.memberScope
                descriptors.addAll(packageScope.getDescriptors(declaredName, location))
            }

            is ClassDescriptor -> {
                // 如果是类描述符，使用静态作用域收集描述符
                val staticClassScope = packageOrClassDescriptor.staticScope
                descriptors.addAll(staticClassScope.getDescriptors(declaredName, location))

                // 如果是对象类，还需要从成员作用域中收集描述符，并映射为导入描述符
                // 该功能目前被注释掉了
//            if (packageOrClassDescriptor.kind == ClassKind.OBJECT) {
//                descriptors.addAll(
//                    packageOrClassDescriptor.unsubstitutedMemberScope.getDescriptors(declaredName, location)
//                        .mapNotNull { it.asImportedFromObjectIfPossible() }
//                )
//            }
            }

            // 如果既不是包描述符也不是类描述符，则抛出异常
            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        // 返回所有收集到的描述符，经过可见性过滤
        return descriptors.choseOnlyVisibleOrAll()
    }

    private fun <D : /*CallableMemberDescriptor*/CallableDescriptor> collectDeclarationMemberDescriptors(
        location: LookupLocation,
        getDescriptors: MemberScope.(Name, LookupLocation) -> Collection<D>
    ): Collection<D> {
        val descriptors = SmartList<D>()

        // 根据packageOrClassDescriptor的类型决定如何收集描述符
        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                // 如果是包描述符，使用成员作用域收集描述符
                val packageScope = packageOrClassDescriptor.memberScope
                descriptors.addAll(packageScope.getDescriptors(declaredName, location))
            }

            is ClassDescriptor -> {
                // 如果是类描述符，使用静态作用域收集描述符
                val staticClassScope = packageOrClassDescriptor.staticScope
                descriptors.addAll(staticClassScope.getDescriptors(declaredName, location))

            }

            // 如果既不是包描述符也不是类描述符，则抛出异常
            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        // 返回所有收集到的描述符，经过可见性过滤
        return descriptors.choseOnlyVisibleOrAll()
    }

    private fun <D : /*CallableMemberDescriptor*/DeclarationDescriptor>
            collectEnumEntryDeclarationMemberDescriptors(
        entryName:Name,
        location: LookupLocation,
        getDescriptors: MemberScope.(Name, LookupLocation) -> Collection<D>
    ): Collection<D> {
        val descriptors = SmartList<D>()

        val enumClass =     when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                // 如果是包描述符，使用成员作用域收集描述符
                val packageScope = packageOrClassDescriptor.memberScope
                packageScope.getContributedClassifier(declaredName, location)
            }

            is ClassDescriptor -> {
                // 如果是类描述符，使用静态作用域收集描述符
           packageOrClassDescriptor
            }

            // 如果既不是包描述符也不是类描述符，则抛出异常
            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        // 根据packageOrClassDescriptor的类型决定如何收集描述符
        when (enumClass) {
            is PackageViewDescriptor -> {
                // 如果是包描述符，使用成员作用域收集描述符
                val packageScope = enumClass.memberScope
                descriptors.addAll(packageScope.getDescriptors(entryName, location))
            }

            is ClassDescriptor -> {
                // 如果是类描述符，使用静态作用域收集描述符
                val classScope = enumClass.unsubstitutedInnerClassesScope
                descriptors.addAll(classScope.getDescriptors(entryName, location))

            }

            // 如果既不是包描述符也不是类描述符，则抛出异常
            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }

        // 返回所有收集到的描述符，经过可见性过滤
        return descriptors.choseOnlyVisibleOrAll()
    }


//    @Suppress("UNCHECKED_CAST")
//    private fun <D : CallableMemberDescriptor> D.asImportedFromObjectIfPossible(): D? = when (this) {
//        is PropertyDescriptor -> asImportedFromObject() as D
//        is FunctionDescriptor -> asImportedFromObject() as D
//        else -> null
//    }


    /**
     * 从集合中选择所有可见的元素，如果都没有可见的则返回原始集合
     *
     * 此函数用于过滤出集合中所有可见的元素如果不存在可见元素，则返回原始集合
     * 它主要用于处理可见性检查，在给定的上下文中判断哪些元素是可见的
     *
     * @param D 这是一个泛型参数，表示可调用描述符的类型，如函数或属性描述符
     * @return 返回过滤后的集合，包含所有可见的元素如果没有可见元素，则返回原始集合
     */
    private fun <D : DeclarationDescriptor> Collection<D>.choseOnlyVisibleOrAll(): Collection<D> =
        filter {
            isVisible(
                it,
                packageFragmentForVisibilityCheck,
                position = QualifierPosition.IMPORT,
                languageVersionSettings
            )
        }
            .takeIf { it.isNotEmpty() }
            ?: this
//@JvmOverloads
//    private fun <D : CallableDescriptor> Collection<D>.choseOnlyVisibleOrAll(): Collection<D> =
//        filter {
//            isVisible(
//                it,
//                packageFragmentForVisibilityCheck,
//                position = QualifierPosition.IMPORT,
//                languageVersionSettings
//            )
//        }
//            .takeIf { it.isNotEmpty() }
//            ?: this
}
