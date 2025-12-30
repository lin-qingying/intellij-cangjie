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

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.smartcasts.SmartCastManager
import org.cangnova.cangjie.resolve.calls.util.getSmartCastVariantsWithLessSpecificExcluded
import org.cangnova.cangjie.resolve.calls.util.receiverTypes
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.utils.getImplicitReceiversWithInstance
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.getDataFlowInfoBefore
import org.cangnova.cangjie.utils.ShadowedDeclarationsFilter

/**
 * 查找声明描述符对应的 PSI 元素
 *
 * 对于带有源码的描述符，直接获取其 PSI。
 * 对于伪覆盖（FAKE_OVERRIDE）的可调用成员，递归查找被覆盖描述符的 PSI。
 *
 * @return 对应的 PSI 元素，如果找不到则返回 null
 */
fun DeclarationDescriptor.findPsi(): PsiElement? {
    val psi = (this as? DeclarationDescriptorWithSource)?.source?.getPsi()
    return if (psi == null && this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        overriddenDescriptors.mapNotNull { it.findPsi() }.firstOrNull()
    } else {
        psi
    }
}

/**
 * 引用变体辅助类
 *
 * 用于代码补全时收集当前上下文中可用的声明描述符。
 * 根据不同的调用类型（import 指令、类型引用、成员访问等）收集相应的补全候选项。
 *
 * ## 主要功能
 *
 * 1. **import/package 指令补全**: 返回当前模块的依赖模块列表
 * 2. **类型引用补全**: 收集作用域内可见的类型
 * 3. **成员访问补全**: 收集接收者类型的成员和扩展
 * 4. **隐式接收者补全**: 收集当前作用域内可用的声明
 *
 * ## 过滤机制
 *
 * - **可见性过滤**: 通过 [visibilityFilter] 过滤不可见的声明
 * - **弃用过滤**: 过滤在解析中隐藏的弃用声明
 * - **遮蔽过滤**: 可选过滤被遮蔽的声明
 * - **未初始化变量过滤**: 可选过滤初始化器中的变量自身
 *
 * @property bindingContext 绑定上下文，包含类型解析信息
 * @property resolutionFacade 解析门面，提供解析服务
 * @property moduleDescriptor 当前模块描述符
 * @property visibilityFilter 可见性过滤器
 * @property notProperties 不作为属性处理的完全限定名集合
 */
@OptIn(FrontendInternals::class)
class ReferenceVariantsHelper(
    private val bindingContext: BindingContext,
    private val resolutionFacade: ResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor,
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean,
    private val notProperties: Set<FqNameUnsafe> = setOf()
) {
    /**
     * 获取引用变体（简化版本）
     *
     * 自动检测表达式的调用类型，然后收集相应的补全候选项。
     *
     * @param expression 简单名称表达式
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param filterOutShadowed 是否过滤被遮蔽的声明
     * @param excludeNonInitializedVariable 是否排除未初始化的变量
     * @param useReceiverType 指定的接收者类型（可选）
     * @return 匹配的声明描述符集合
     */
    fun getReferenceVariants(
        expression: CjSimpleNameExpression,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,

        filterOutShadowed: Boolean = true,
        excludeNonInitializedVariable: Boolean = true,
        useReceiverType: CangJieType? = null
    ): Collection<DeclarationDescriptor> = getReferenceVariants(
        expression, CallTypeAndReceiver.detect(expression),
        kindFilter, nameFilter, filterOutShadowed, excludeNonInitializedVariable, useReceiverType
    )

    /**
     * 获取引用变体（完整版本）
     *
     * 根据指定的调用类型和接收者收集补全候选项，并应用各种过滤器。
     *
     * @param contextElement 上下文元素
     * @param callTypeAndReceiver 调用类型和接收者
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param filterOutShadowed 是否过滤被遮蔽的声明
     * @param excludeNonInitializedVariable 是否排除未初始化的变量
     * @param useReceiverType 指定的接收者类型（可选）
     * @return 匹配的声明描述符集合
     */
    fun getReferenceVariants(
        contextElement: PsiElement,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,

        filterOutShadowed: Boolean = true,
        excludeNonInitializedVariable: Boolean = true,
        useReceiverType: CangJieType? = null
    ): Collection<DeclarationDescriptor> {
        var variants: Collection<DeclarationDescriptor> =
            getReferenceVariantsNoVisibilityFilter(
                contextElement,
                kindFilter,
                nameFilter,
                callTypeAndReceiver,
                useReceiverType
            )
                .filter {
                    !resolutionFacade.frontendService<DeprecationResolver>()
                        .isHiddenInResolution(it) && visibilityFilter(it)
                }

        if (filterOutShadowed) {
            ShadowedDeclarationsFilter.create(bindingContext, resolutionFacade, contextElement, callTypeAndReceiver)
                ?.let {
                    variants = it.filter(variants)
                }
        }


        if (excludeNonInitializedVariable && kindFilter.kindMask.and(DescriptorKindFilter.VARIABLES_MASK) != 0) {
            variants = excludeNonInitializedVariable(variants, contextElement)
        }

        return variants
    }

    /**
     * 获取 import 或 package 指令的补全变体
     *
     * 仓颉的 import 语句格式为: `import <模块名>.<包名>`，如 `import std.core`
     *
     * - 如果有接收者表达式（如 `std.`），则返回该限定符作用域内的静态成员
     * - 如果没有接收者表达式（import 语句开头），则返回当前模块的所有依赖模块
     *
     * @param receiverExpression 接收者表达式（限定符部分）
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @return 匹配的声明描述符集合
     */
    private fun getVariantsForImportOrPackageDirective(
        receiverExpression: CjExpression?,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            val staticDescriptors = qualifier.staticScope.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)

//            val objectDescriptor =
//                (qualifier as? ClassQualifier)?.descriptor?.takeIf { it.kind == ClassKind.OBJECT } ?: return staticDescriptors

            return staticDescriptors /*+ objectDescriptor.defaultType.memberScope.getDescriptorsFiltered(kindFilter, nameFilter)*/
        } else {
            // 仓颉的 import 语句格式为: import <模块名>.<包名>，如 import std.core
            // 这里需要返回当前模块可以访问的所有依赖模块
            return moduleDescriptor.allDependencyModules
                .filter { kindFilter.accepts(it) && nameFilter(it.name) }
        }
    }


    /**
     * 获取引用变体（无可见性过滤）
     *
     * 根据调用类型分发到不同的处理逻辑：
     * - IMPORT_DIRECTIVE / PACKAGE_DIRECTIVE: 处理导入语句补全
     * - TYPE / ANNOTATION: 处理类型引用补全
     * - DOT / SAFE / SUPER_MEMBERS: 处理成员访问补全
     * - DEFAULT: 处理隐式接收者补全
     *
     * @param contextElement 上下文元素
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param callTypeAndReceiver 调用类型和接收者
     * @param useReceiverType 指定的接收者类型（可选）
     * @return 匹配的声明描述符集合
     */
    private fun getReferenceVariantsNoVisibilityFilter(
        contextElement: PsiElement,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        useReceiverType: CangJieType?
    ): Collection<DeclarationDescriptor> {
        val callType = callTypeAndReceiver.callType

        @Suppress("NAME_SHADOWING")
        val kindFilter = kindFilter.intersect(callType.descriptorKindFilter)

        val receiverExpression: CjExpression?
        when (callTypeAndReceiver) {
            is CallTypeAndReceiver.IMPORT_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.PACKAGE_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.TYPE -> {
                return getVariantsForUserType(callTypeAndReceiver.receiver, contextElement, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.ANNOTATION -> {
                return getVariantsForUserType(callTypeAndReceiver.receiver, contextElement, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
                return emptyList()
            }

            is CallTypeAndReceiver.DEFAULT -> receiverExpression = null
            is CallTypeAndReceiver.DOT -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SUPER_MEMBERS -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SAFE -> receiverExpression = callTypeAndReceiver.receiver

            is CallTypeAndReceiver.OPERATOR -> return emptyList()
            is CallTypeAndReceiver.UNKNOWN -> return emptyList()
            else -> throw RuntimeException()
        }

        val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(contextElement)
        val containingDeclaration = resolutionScope.ownerDescriptor

        val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
        val languageVersionSettings = resolutionFacade.frontendService<LanguageVersionSettings>()

        val implicitReceiverTypes = resolutionScope.getImplicitReceiversWithInstance(

        ).flatMap {
            smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(
                it.value,
                bindingContext,
                containingDeclaration,
                dataFlowInfo,
                languageVersionSettings,
                resolutionFacade.frontendService()
            )
        }.toSet()

        val descriptors = LinkedHashSet<DeclarationDescriptor>()

        val filterWithoutExtensions = kindFilter exclude DescriptorKindExclude.Extensions
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                descriptors.addAll(
                    qualifier.staticScope.collectStaticMembers(
                        resolutionFacade,
                        filterWithoutExtensions,
                        nameFilter
                    )
                )
            } else {

                val explicitReceiverTypes = if (useReceiverType != null) {
                    listOf(useReceiverType)
                } else {
                    callTypeAndReceiver.receiverTypes(
                        bindingContext,
                        contextElement,
                        moduleDescriptor,
                        resolutionFacade,
                        stableSmartCastsOnly = false
                    )!!
                }

                descriptors.processAll(
                    implicitReceiverTypes,
                    explicitReceiverTypes,
                    resolutionScope,
                    callType,
                    kindFilter,
                    nameFilter
                )
            }

        } else {
            assert(useReceiverType == null) { "'useReceiverType' parameter is not supported for implicit receiver" }

            descriptors.processAll(
                implicitReceiverTypes,
                implicitReceiverTypes,
                resolutionScope,
                callType,
                kindFilter,
                nameFilter
            )

            // add non-instance members
            descriptors.addAll(
                resolutionScope.collectDescriptorsFiltered(
                    filterWithoutExtensions,
                    nameFilter,
                    changeNamesForAliased = true
                )
            )
            descriptors.addAll(resolutionScope.collectAllFromMeAndParent { scope ->
                scope.collectSyntheticStaticMembersAndConstructors(resolutionFacade, kindFilter, nameFilter)
            })
        }

        if (callType == CallType.SUPER_MEMBERS) { // we need to unwrap fake overrides in case of "super." because ShadowedDeclarationsFilter does not work correctly
            return descriptors.flatMapTo(LinkedHashSet<DeclarationDescriptor>()) {
                if (it is CallableMemberDescriptor && it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                    it.overriddenDescriptors
                else
                    listOf(it)
            }
        }

        return descriptors
    }

    /**
     * 获取用户类型引用的补全变体
     *
     * 用于类型注解位置的补全，如变量类型、函数返回类型等。
     *
     * @param receiverExpression 接收者表达式（限定符部分）
     * @param contextElement 上下文元素
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @return 匹配的声明描述符集合
     */
    private fun getVariantsForUserType(
        receiverExpression: CjExpression?,
        contextElement: PsiElement,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            return qualifier.staticScope.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)
        } else {
            val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
            return scope.collectDescriptorsFiltered(kindFilter, nameFilter, changeNamesForAliased = true)
        }
    }

    /**
     * 添加作用域扩展和合成扩展
     *
     * 收集当前作用域内对接收者类型可用的扩展成员。
     * 在仓颉语言中，extend 成员作为普通类成员处理。
     *
     * @receiver 待添加描述符的可变集合
     * @param scope 词法作用域
     * @param receiverTypes 接收者类型集合
     * @param callType 调用类型
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     */
    private fun MutableSet<DeclarationDescriptor>.addScopeAndSyntheticExtensions(
        scope: LexicalScope,
        receiverTypes: Collection<CangJieType>,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) return
        if (receiverTypes.isEmpty()) return

        fun process(syntheticMember: CallableDescriptor) {
            // 在仓颉语言中，extend 成员就是普通的类成员，不需要特殊处理
            if (kindFilter.accepts(syntheticMember) && nameFilter(syntheticMember.name)) {
                add(syntheticMember)
            }
        }

        for (descriptor in scope.collectDescriptorsFiltered(
            kindFilter exclude DescriptorKindExclude.NonExtensions,
            nameFilter,
            changeNamesForAliased = true
        )) {
            // todo: sometimes resolution scope here is LazyJavaClassMemberScope. see ea.jetbrains.com/browser/ea_problems/72572
            process(descriptor as CallableDescriptor)
        }

        val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java).forceEnableSamAdapters()
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            val lookupLocation =
                (scope.ownerDescriptor.toSourceElement.getPsi() as? CjElement)?.let { CangJieLookupLocation(it) }
                    ?: NoLookupLocation.FROM_IDE

            for (extension in syntheticScopes.collectSyntheticExtensionProperties(receiverTypes, lookupLocation)) {
                process(extension)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            for (syntheticMember in syntheticScopes.collectSyntheticMemberFunctions(receiverTypes)) {
                process(syntheticMember)
            }
        }
    }

    /**
     * 综合处理所有成员收集逻辑
     *
     * 依次添加：
     * 1. 非扩展成员（接收者类型的直接成员）
     * 2. 成员扩展（隐式接收者类型中定义的扩展）
     * 3. 作用域扩展和合成扩展
     * 4. 最后过滤掉操作符函数
     *
     * @receiver 待添加描述符的可变集合
     * @param implicitReceiverTypes 隐式接收者类型集合
     * @param receiverTypes 接收者类型集合
     * @param resolutionScope 解析作用域
     * @param callType 调用类型
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     */
    private fun MutableSet<DeclarationDescriptor>.processAll(
        implicitReceiverTypes: Collection<CangJieType>,
        receiverTypes: Collection<CangJieType>,
        resolutionScope: LexicalScope,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        addNonExtensionMembers(receiverTypes, kindFilter, nameFilter, constructorFilter = { false })
        addMemberExtensions(implicitReceiverTypes, receiverTypes, callType, kindFilter, nameFilter)
        addScopeAndSyntheticExtensions(resolutionScope, receiverTypes, callType, kindFilter, nameFilter)

        filtration()

    }

    /**
     * 过滤结果集
     *
     * 移除操作符函数，因为操作符函数不应出现在普通代码补全中。
     */
    private fun MutableSet<DeclarationDescriptor>.filtration() {
        // 过滤掉操作符函数
        removeIf { it is FunctionDescriptor && it.isOperator }
    }

    /**
     * 添加非扩展可调用成员和构造函数
     *
     * 从作用域中收集可调用成员（函数、属性）和类的构造函数。
     * 对于抽象类和密封类，跳过其构造函数。
     *
     * @receiver 待添加描述符的可变集合
     * @param scope 层级作用域
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param constructorFilter 构造函数过滤器
     * @param classesOnly 是否只处理类（用于父类型处理）
     */
    private fun MutableSet<DeclarationDescriptor>.addNonExtensionCallablesAndConstructors(
        scope: HierarchicalScope,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean,
        classesOnly: Boolean
    ) {
        var filterToUse =
            DescriptorKindFilter(kindFilter.kindMask and DescriptorKindFilter.CALLABLES.kindMask).exclude(
                DescriptorKindExclude.Extensions
            )

        // should process classes if we need constructors
        if (filterToUse.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            filterToUse = filterToUse.withKinds(DescriptorKindFilter.CLASSES_MASK)
        }

        for (descriptor in scope.collectDescriptorsFiltered(filterToUse, nameFilter, changeNamesForAliased = true)) {
            if (descriptor is ClassDescriptor) {
                if (descriptor.modality == Modality.ABSTRACT || descriptor.modality == Modality.SEALED) continue
                if (!constructorFilter(descriptor)) continue
                descriptor.constructors.filterTo(this) { kindFilter.accepts(it) }
            } else if (!classesOnly && kindFilter.accepts(descriptor)) {
                this.add(descriptor)
            }
        }
    }

    /**
     * 添加成员作用域的非扩展成员
     *
     * 收集成员作用域中的成员，并递归处理父类型的成员。
     *
     * @receiver 待添加描述符的可变集合
     * @param memberScope 成员作用域
     * @param typeConstructor 类型构造器（用于获取父类型）
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param constructorFilter 构造函数过滤器
     */
    private fun MutableSet<DeclarationDescriptor>.addNonExtensionMembers(
        memberScope: MemberScope,
        typeConstructor: TypeConstructor,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        addNonExtensionCallablesAndConstructors(
            memberScope.memberScopeAsImportingScope(),
            kindFilter, nameFilter, constructorFilter,
            false
        )
        typeConstructor.supertypes.forEach {
            addNonExtensionCallablesAndConstructors(
                it.memberScope.memberScopeAsImportingScope(),
                kindFilter, nameFilter, constructorFilter,
                true
            )
        }
    }

    /**
     * 为多个接收者类型添加非扩展成员
     *
     * 遍历所有接收者类型，收集每个类型的成员。
     *
     * @receiver 待添加描述符的可变集合
     * @param receiverTypes 接收者类型集合
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param constructorFilter 构造函数过滤器
     */
    private fun MutableSet<DeclarationDescriptor>.addNonExtensionMembers(
        receiverTypes: Collection<CangJieType>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        for (receiverType in receiverTypes) {
            addNonExtensionMembers(
                receiverType.memberScope,
                receiverType.constructor,
                kindFilter,
                nameFilter,
                constructorFilter
            )
//           TODO 是否需要添加静态成员
//            addNonExtensionMembers(receiverType.staticMemberScope, receiverType.constructor, kindFilter, nameFilter, constructorFilter)


        }
    }

    /**
     * 添加成员扩展
     *
     * 在仓颉语言中，extend 成员作为普通类成员处理，
     * 不需要像 Kotlin 那样特殊处理 extension receiver。
     *
     * @receiver 待添加描述符的可变集合
     * @param dispatchReceiverTypes 分发接收者类型集合
     * @param extensionReceiverTypes 扩展接收者类型集合（在仓颉中未使用）
     * @param callType 调用类型
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     */
    private fun MutableSet<DeclarationDescriptor>.addMemberExtensions(
        dispatchReceiverTypes: Collection<CangJieType>,
        extensionReceiverTypes: Collection<CangJieType>,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        // 在仓颉语言中，extend 成员就是普通的类成员
        // 不需要像 Kotlin 那样处理 extension receiver
        val memberFilter = kindFilter exclude DescriptorKindExclude.NonExtensions
        for (dispatchReceiverType in dispatchReceiverTypes) {
            for (member in dispatchReceiverType.memberScope.getDescriptorsFiltered(memberFilter, nameFilter)) {
                add(member as CallableDescriptor)
            }
        }
    }


    /**
     * 排除未初始化的变量
     *
     * 过滤掉在变量初始化器内部引用自身的情况，以及在参数列表中引用后续参数的情况。
     *
     * 例如：
     * - `var x = x + 1` 中的 `x` 不应在补全中出现
     * - `fun test(a: Int = b, b: Int)` 中的 `b` 不应在 `a` 的默认值补全中出现
     *
     * @param variants 候选描述符集合
     * @param contextElement 上下文元素
     * @return 过滤后的描述符集合
     */
    fun excludeNonInitializedVariable(
        variants: Collection<DeclarationDescriptor>,
        contextElement: PsiElement
    ): Collection<DeclarationDescriptor> {
        for (element in contextElement.parentsWithSelf) {
            val parent = element.parent
            if (parent is CjVariableDeclaration && element == parent.initializer) {
                return variants.filter { it.findPsi() != parent }
            } else if (element is CjParameter) {
                // 过滤掉在当前参数之后初始化的参数
                // 例如: fun test(a: Int = <光标>, b: Int) {}
                // 此时 b 不应出现在补全列表中
                return variants.filter {
                    val candidatePsi = it.findPsi()
                    if (candidatePsi is CjParameter && candidatePsi.parent == parent) {
                        return@filter candidatePsi.startOffset < element.startOffset
                    }
                    true
                }
            }
            // 可以在 lambda 或匿名对象内部使用位于其初始化器中的变量
            if (element is CjDeclaration) break
        }
        return variants
    }

}

/**
 * 收集合成静态成员和构造函数
 *
 * 从解析作用域中收集合成的静态函数和构造函数。
 *
 * @param resolutionFacade 解析门面
 * @param kindFilter 描述符类型过滤器
 * @param nameFilter 名称过滤器
 * @return 匹配的函数描述符列表
 */
@OptIn(FrontendInternals::class)
fun ResolutionScope.collectSyntheticStaticMembersAndConstructors(
    resolutionFacade: ResolutionFacade,
    kindFilter: DescriptorKindFilter,
    nameFilter: (Name) -> Boolean
): List<FunctionDescriptor> {
    val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java)

    val functionDescriptors =
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK))
            getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, nameFilter)
        else
            emptyList()

    val classifierDescriptors =
        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK))
            getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, nameFilter)
        else
            emptyList()

    return (syntheticScopes.forceEnableSamAdapters().collectSyntheticStaticFunctions(functionDescriptors) +
            syntheticScopes.collectSyntheticConstructors(classifierDescriptors))
        .filter { kindFilter.accepts(it) && nameFilter(it.name) }
}

/**
 * 强制启用 SAM 适配器
 *
 * 返回启用 SAM 适配器的合成作用域。
 * 在仓颉语言中直接返回原始作用域。
 */
fun SyntheticScopes.forceEnableSamAdapters(): SyntheticScopes {
    return this

}

/**
 * 收集静态成员
 *
 * 从成员作用域中收集静态成员，包括合成的静态成员和构造函数。
 *
 * @param resolutionFacade 解析门面
 * @param kindFilter 描述符类型过滤器
 * @param nameFilter 名称过滤器
 * @return 匹配的声明描述符集合
 */
private fun MemberScope.collectStaticMembers(
    resolutionFacade: ResolutionFacade,
    kindFilter: DescriptorKindFilter,
    nameFilter: (Name) -> Boolean
): Collection<DeclarationDescriptor> {
    return getDescriptorsFiltered(kindFilter, nameFilter) + collectSyntheticStaticMembersAndConstructors(
        resolutionFacade,
        kindFilter,
        nameFilter
    )
}

/**
 * 获取声明描述符的源元素
 *
 * 如果描述符带有源码信息，返回其源元素；否则返回 NO_SOURCE。
 */
val DeclarationDescriptor.toSourceElement: SourceElement
    get() = if (this is DeclarationDescriptorWithSource) source else SourceElement.NO_SOURCE
