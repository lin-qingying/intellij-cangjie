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

package org.cangnova.cangjie.resolve.lazy.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_INHERITED_MEMBERS
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_OVERLOADS
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_STATIC
import org.cangnova.cangjie.diagnostics.infos.warnings.CONFLICTING_INHERITED_MEMBERS_WARNING
import org.cangnova.cangjie.diagnostics.reportOnDeclarationOrFail
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.NewCangJieTypeCheckerImpl
import org.cangnova.cangjie.utils.reportOnDeclarationAs

/**
 * 枚举成员作用域
 *
 * 为枚举类型提供延迟解析的成员作用域。枚举的成员作用域与类的成员作用域类似，
 * 但有一些重要区别：
 * - 枚举不支持主构造函数参数转属性
 * - 枚举的构造器是枚举构造器（EnumConstructorDescriptor）而非类构造器
 * - 枚举可以继承接口，但不能继承类（除了内置的 Enum 基类）
 */
class LazyEnumMemberScope(
    c: LazyClassContext,
    declarationProvider: ClassMemberDeclarationProvider,
    private val thisEnum: EnumDescriptorWithResolutionScopes,
    trace: BindingTrace,
    private val cangjieTypeRefiner: CangJieTypeRefiner = c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner,
    scopeForDeclaredMembers: LazyEnumMemberScope? = null
) : AbstractLazyMemberScope<EnumDescriptorWithResolutionScopes, ClassMemberDeclarationProvider>(
    c, declarationProvider, thisEnum, trace, scopeForDeclaredMembers
) {
    override fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope =
        thisEnum.scopeForInitializerResolution

    override fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>) {
        // 枚举不支持主构造函数参数转属性，所以这里不添加任何内容
    }

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        // 从超类型（接口）获取成员
        val fromSupertypes = ArrayList<PropertyDescriptor>()
        for (supertype in supertypes) {
            fromSupertypes.addAll(
                supertype.memberScope.getContributedPropertys(
                    name,
                    NoLookupLocation.FOR_ALREADY_TRACKED
                )
            )
        }

        c.syntheticResolveExtension.generateSyntheticProperties(
            thisEnum,
            name,
            trace.bindingContext,
            fromSupertypes,
            result
        )
        generateFakeOverrides(name, fromSupertypes, result, PropertyDescriptor::class.java)
    }

    private val allClassifierDescriptors = storageManager.createLazyValue {
        doClassifierDescriptors(ALL_NAME_FILTER)
    }

    private val allDescriptors = storageManager.createLazyValue {
        doDescriptors(ALL_NAME_FILTER)
    }

    val supertypes by storageManager.createLazyValue {
        cangjieTypeRefiner.refineSupertypes(thisEnum)
    }

    private fun doClassifierDescriptors(nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val result = computeDescriptorsFromDeclaredElements(
            DescriptorKindFilter.CLASSIFIERS,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )
        return result.toList()
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return when (kindFilter) {
            DescriptorKindFilter.CLASSIFIERS ->
                if (nameFilter == ALL_NAME_FILTER || allClassifierDescriptors.isComputed() || allClassifierDescriptors.isComputing()) {
                    allClassifierDescriptors()
                } else {
                    storageManager.compute {
                        doClassifierDescriptors(nameFilter)
                    }
                }

            else ->
                if (nameFilter == ALL_NAME_FILTER || allDescriptors.isComputed() || allDescriptors.isComputing()) {
                    allDescriptors()
                } else {
                    storageManager.compute {
                        doDescriptors(nameFilter)
                    }
                }
        }
    }

    private fun doDescriptors(nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val result = computeDescriptorsFromDeclaredElements(
            DescriptorKindFilter.ALL,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )
        computeExtraDescriptors(result, NoLookupLocation.FOR_ALREADY_TRACKED)
        return result.toList()
    }

    protected open fun computeExtraDescriptors(
        result: MutableCollection<DeclarationDescriptor>,
        location: LookupLocation
    ) {
        for (supertype in supertypes) {
            for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                if (descriptor is FunctionDescriptor) {
                    result.addAll(getContributedFunctions(descriptor.name, location))
                } else if (descriptor is PropertyDescriptor) {
                    result.addAll(getContributedPropertys(descriptor.name, location))
                }
            }
        }
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker.record(location, thisEnum, name)
    }

    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration): LexicalScope =
        thisEnum.scopeForMemberDeclarationResolution

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
        val location = NoLookupLocation.FOR_ALREADY_TRACKED

        val fromSupertypes = arrayListOf<SimpleFunctionDescriptor>()
        for (supertype in supertypes) {
            fromSupertypes.addAll(supertype.memberScope.getContributedFunctions(name, location))
        }

        generateFakeOverrides(name, fromSupertypes, result, SimpleFunctionDescriptor::class.java)
    }

    override fun getNonDeclaredMacros(name: Name, result: MutableSet<MacroDescriptor>) {
        // 枚举不支持宏
    }

    private fun <D : CallableMemberDescriptor> generateFakeOverrides(
        name: Name,
        fromSupertypes: Collection<D>,
        result: MutableCollection<D>,
        exactDescriptorClass: Class<out D>
    ) {
        NewCangJieTypeCheckerImpl(cangjieTypeRefiner).overridingUtil.generateOverridesInFunctionGroup(
            name,
            fromSupertypes,
            ArrayList(result),
            thisEnum,
            object : OverridingStrategy() {
                override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                    assert(exactDescriptorClass.isInstance(fakeOverride)) {
                        "Wrong descriptor type in an override: $fakeOverride while expecting ${exactDescriptorClass.simpleName}"
                    }
                    @Suppress("UNCHECKED_CAST")
                    result.add(fakeOverride as D)
                }

                override fun staticConflict(
                    fromSuper: CallableMemberDescriptor,
                    fromCurrent: CallableMemberDescriptor,
                    message: String
                ) {
                    reportOnDeclarationOrFail(
                        trace,
                        fromCurrent
                    ) { CONFLICTING_STATIC.on(it, listOf(fromCurrent, fromSuper), message) }
                }

                override fun overrideConflict(
                    fromSuper: CallableMemberDescriptor,
                    fromCurrent: CallableMemberDescriptor
                ) {
                    reportOnDeclarationOrFail(
                        trace,
                        fromCurrent
                    ) { CONFLICTING_OVERLOADS.on(it, listOf(fromCurrent, fromSuper)) }
                }

                override fun inheritanceConflict(
                    first: CallableMemberDescriptor,
                    second: CallableMemberDescriptor
                ) {
                    reportOnDeclarationAs<CjTypeStatement>(
                        trace,
                        thisEnum
                    ) { type ->
                        CONFLICTING_INHERITED_MEMBERS.on(
                            type,
                            thisEnum,
                            listOf(first, second)
                        )
                    }
                }
            })

        for (descriptor in result) {
            if (descriptor !is FunctionDescriptorImpl) continue
            for (overriddenFunction in descriptor.overriddenDescriptors) {
                if (overriddenFunction !is FunctionDescriptorImpl) continue
                val conflictedDescriptor = overriddenFunction.getUserData(
                    DeserializedDeclarationsFromSupertypeConflictDataKey
                ) ?: continue
                reportOnDeclarationAs<CjTypeStatement>(
                    trace,
                    thisEnum
                ) { type ->
                    CONFLICTING_INHERITED_MEMBERS_WARNING.on(
                        type,
                        thisEnum,
                        listOf(overriddenFunction, conflictedDescriptor)
                    )
                }
            }
        }
        OverrideResolver.resolveUnknownVisibilities(result, trace)
    }

    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassAndEnumDescriptor>) {
        // 枚举可以有嵌套类型
    }

    override fun toString() = "lazy scope for enum ${thisEnum.name}"
}
