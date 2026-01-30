/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
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
import org.cangnova.cangjie.resolve.DeserializedDeclarationsFromSupertypeConflictDataKey
import org.cangnova.cangjie.resolve.OverrideResolver
import org.cangnova.cangjie.resolve.OverridingStrategy
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.cangnova.cangjie.types.checker.DefaultCangJieTypeChecker

import org.cangnova.cangjie.utils.reportOnDeclarationAs

/**
 * 延迟扩展成员作用域
 *
 * 管理扩展声明中的成员（函数、属性等）的解析和查找。
 * 支持从接口继承成员（fake overrides）。
 *
 * @param c 延迟类上下文
 * @param declarationProvider 声明提供者
 * @param thisExtend 当前扩展描述符
 * @param trace 绑定追踪器
 */
class LazyExtendMemberScope(
    c: LazyClassContext,
    declarationProvider: ClassMemberDeclarationProvider,
    private val thisExtend: ExtendDescriptor,
    trace: BindingTrace
) : AbstractLazyMemberScope<ExtendDescriptor, ClassMemberDeclarationProvider>(
    c, declarationProvider, thisExtend, trace, null
) {

    /**
     * 获取初始化器解析作用域
     *
     * 扩展没有构造器和类级别的初始化器作用域，
     * 但扩展内可以声明带初始值的属性/变量，因此复用声明解析作用域。
     */
    override fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope =
        getScopeForMemberDeclarationResolution(declaration)

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker.record(location, thisExtend, name)

    }

    /**
     * 所有描述符的延迟缓存（包括声明的和继承的 fake overrides）
     */
    private val allDescriptors = storageManager.createLazyValue {
        doDescriptors(ALL_NAME_FILTER)
    }

    /**
     * 获取贡献的描述符（包括声明的和从接口继承的 fake overrides）
     */
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return if (nameFilter == ALL_NAME_FILTER || allDescriptors.isComputed() || allDescriptors.isComputing()) {
            allDescriptors()
        } else {
            storageManager.compute {
                doDescriptors(nameFilter)
            }
        }
    }

    /**
     * 计算所有描述符（声明的和继承的）
     */
    private fun doDescriptors(nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val result = computeDescriptorsFromDeclaredElements(
            DescriptorKindFilter.ALL,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )
        computeExtraDescriptors(result, NoLookupLocation.FOR_ALREADY_TRACKED)
        return result.toList()
    }

    /**
     * 计算额外的描述符（从接口继承的 fake overrides）
     */
    private fun computeExtraDescriptors(
        result: MutableCollection<DeclarationDescriptor>,
        location: LookupLocation
    ) {
        for (supertype in thisExtend.superTypes) {
            for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                if (descriptor is FunctionDescriptor) {
                    result.addAll(getContributedFunctions(descriptor.name, location))
                } else if (descriptor is PropertyDescriptor) {
                    result.addAll(getContributedPropertys(descriptor.name, location))
                }
                // Nothing else is inherited
            }
        }
    }

    /**
     * 获取非声明的函数
     *
     * 从扩展实现的接口中继承函数（生成 fake overrides）
     */
    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
        val fromSupertypes = ArrayList<SimpleFunctionDescriptor>()

        // 从实现的接口收集函数
        for (supertype in thisExtend.superTypes) {
            fromSupertypes.addAll(
                supertype.memberScope.getContributedFunctions(
                    name,
                    NoLookupLocation.FOR_ALREADY_TRACKED
                )
            )
        }

        // 生成 fake overrides
        if (fromSupertypes.isNotEmpty()) {
            generateFakeOverrides(name, fromSupertypes, result, SimpleFunctionDescriptor::class.java)
        }
    }

    override fun getNonDeclaredClasses(
        name: Name,
        result: MutableSet<ClassAndEnumDescriptor>
    ) {

    }

    /**
     * 获取非声明的属性
     *
     * 从扩展实现的接口中继承属性（生成 fake overrides）
     */
    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        val fromSupertypes = ArrayList<PropertyDescriptor>()

        // 从实现的接口收集属性
        for (supertype in thisExtend.superTypes) {
            fromSupertypes.addAll(
                supertype.memberScope.getContributedPropertys(
                    name,
                    NoLookupLocation.FOR_ALREADY_TRACKED
                )
            )
        }

        // 生成 fake overrides
        if (fromSupertypes.isNotEmpty()) {
            generateFakeOverrides(name, fromSupertypes, result, PropertyDescriptor::class.java)
        }
    }

    /**
     * 获取非声明的变量
     *
     * 从扩展实现的接口中继承变量
     */
    override fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>) {
        // 从实现的接口收集变量
        for (supertype in thisExtend.superTypes) {
            result.addAll(
                supertype.memberScope.getContributedVariables(
                    name,
                    NoLookupLocation.FOR_ALREADY_TRACKED
                )
            )
        }
    }

    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration): LexicalScope =
        c.declarationScopeProvider.getResolutionScopeForDeclaration(declaration)

    /**
     * 获取非声明的宏
     *
     * 扩展不支持宏
     */
    override fun getNonDeclaredMacros(name: Name, result: MutableSet<org.cangnova.cangjie.descriptors.macro.MacroDescriptor>) {
        // 扩展不支持宏
    }

    /**
     * 为可调用成员生成伪重写
     *
     * 从实现的接口中继承成员，处理重写冲突
     */
    private fun <D : CallableMemberDescriptor> generateFakeOverrides(
        name: Name,
        fromSupertypes: Collection<D>,
        result: MutableCollection<D>,
        exactDescriptorClass: Class<out D>
    ) {
        DefaultCangJieTypeChecker(c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner).overridingUtil.generateOverridesInFunctionGroup(
            name,
            fromSupertypes,
            ArrayList(result),
            thisExtend,
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
                        thisExtend
                    ) { type ->
                        CONFLICTING_INHERITED_MEMBERS.on(
                            type,
                            thisExtend,
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
                    thisExtend
                ) { type ->
                    CONFLICTING_INHERITED_MEMBERS_WARNING.on(
                        type,
                        thisExtend,
                        listOf(overriddenFunction, conflictedDescriptor)
                    )
                }
            }
        }
        OverrideResolver.resolveUnknownVisibilities(result, trace)
    }
}
