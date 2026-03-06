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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.descriptors.impl.AbstractEnumDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ModifiersChecker
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.LazyEntity
import org.cangnova.cangjie.resolve.scopes.InstanceMemberScope
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.StaticMemberScope
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.resolve.extend.ExtendManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.EnumTypeConstructorImpl
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 延迟枚举描述符
 *
 * 采用延迟计算策略的枚举描述符实现，仅在需要时才解析枚举的各种信息。
 * 继承自 AbstractEnumDescriptor 并实现 EnumDescriptor 接口。
 *
 * 支持：
 * - 简单枚举（无关联值）
 * - 泛型枚举
 * - 带关联值的枚举
 * - 非穷尽性枚举（带 ... 标记）
 *
 * @param c 延迟类上下文，提供解析所需的各种服务
 * @param containingDeclaration 包含该枚举的声明描述符
 * @param name 枚举名
 * @param classLikeInfo 枚举的信息数据
 */
class LazyEnumDescriptor(
    private val c: LazyClassContext,
    override val containingDeclaration: DeclarationDescriptor,
    name: Name,
    private val classLikeInfo: CjClassLikeInfo,


    ) : AbstractEnumDescriptor(c.storageManager, name), EnumDescriptorWithResolutionScopes, LazyEntity {

    private val enumPsi: CjEnum = classLikeInfo.correspondingClass as CjEnum

    override val source: SourceElement =   enumPsi.toSourceElement()

    private val declarationProvider: ClassMemberDeclarationProvider =
        c.declarationProviderFactory.getClassMemberDeclarationProvider(classLikeInfo)

    private val scopesHolderForClass: ScopesHolderForClass<LazyEnumMemberScope> = ScopesHolderForClass.create(
        this,
        c.storageManager,
        c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner
    ) { cangjieTypeRefinerForDependentModule ->
        val scopeForDeclaredMembers = when {
            !cangjieTypeRefinerForDependentModule.isRefinementNeededForModule(c.moduleDescriptor) -> null
            else -> scopesHolderForClass.getScope(c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner)
        }

        LazyEnumMemberScope(
            c, declarationProvider, this, c.trace, cangjieTypeRefinerForDependentModule,
            scopeForDeclaredMembers
        )
    }

    /**
     * 类解析作用域支持
     */
    private val resolutionScopesSupport = ClassResolutionScopesSupport(
        this,
        c.storageManager,
        c.languageVersionSettings,
        ::getOuterScope
    )

    fun resolveMemberHeaders() {
        ForceResolveUtil.forceResolveAllContents(annotations)
        constructors
        containingDeclaration
        thisAsReceiverParameter
        kind
        modality
        name
        original
        scopeForClassHeaderResolution
        scopeForMemberDeclarationResolution
        DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope)
        scopeForInitializerResolution
        unsubstitutedMemberScope

        visibility
    }

    /**
     * 获取外部作用域
     */
    private fun getOuterScope(): LexicalScope =
        c.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)



    /**
     * 枚举构造器列表
     *
     * 使用 storageManager 确保线程安全的延迟初始化
     * 虽然枚举构造器是核心部分，但需要延迟加载以避免在类初始化时的循环依赖
     */
    override val constructors: Collection<EnumConstructorDescriptor>
        get() = _constructors()

    private val _constructors = c.storageManager.createLazyValue {
        val enumConstructorsPsi = enumPsi.constructor

        enumConstructorsPsi.mapNotNull { enumConstructorPsi ->
            // 使用 enumDescriptorResolver 解析枚举构造器
            val descriptor = c.enumDescriptorResolver.resolveEnumConstructorConstructorDescriptor(
                scopeForMemberDeclarationResolution,
                this,
                enumConstructorPsi,
                c.trace,
                c.languageVersionSettings,
                null
            )
            // 记录到 BindingContext（使用 ENUM_CONSTRUCTOR 专用 key）
            c.trace.record(BindingContext.ENUM_CONSTRUCTOR, enumConstructorPsi, descriptor)
            descriptor
        }
    }

    /**
     * 是否有关联值
     * 如果任何一个枚举构造器有参数，则返回 true
     */
    override val hasArguments: Boolean
        get() = constructors.any { it.valueParameters.isNotEmpty() }

    /**
     * 是否为非穷尽性枚举
     * 检查枚举是否包含 ELLIPSIS (...) 标记
     */
    override val isNonExhaustive: Boolean = enumPsi.isNonExhaustive

    /**
     * 枚举类型
     */
    override val enumKind: EnumKind
        get() = if (isNonExhaustive) EnumKind.NON_EXHAUSTIVE else EnumKind.ENUM

    /**
     * 修饰性
     */
    override val modality: Modality by lazy {
        ModifiersChecker.resolveModalityFromModifiers(
            enumPsi,
            Modality.FINAL,
            c.trace.bindingContext,
            null,
            allowSealed = false
        )
    }

    /**
     * 可见性
     */
    override val visibility: DescriptorVisibility by lazy {
        ModifiersChecker.resolveVisibilityFromModifiers(
            enumPsi,
            DescriptorVisibilities.PUBLIC
        )
    }

    override val annotations: Annotations
        get() = Annotations.EMPTY

    /**
     * 声明的类型参数
     */
    override val declaredTypeParameters: List<TypeParameterDescriptor> by lazy {
        val typeParameterList = declarationProvider.ownerInfo?.typeParameterList
            ?: return@lazy emptyList<TypeParameterDescriptor>()

        typeParameterList.parameters.takeIf { it.isNotEmpty() }?.mapIndexed { index, parameter ->
            LazyTypeParameterDescriptor(c, this, parameter, Annotations.EMPTY, index)
        } ?: emptyList()
    }

    /**
     * 用于成员声明解析的词法作用域
     */
    override val scopeForMemberDeclarationResolution: LexicalScope
        get() = resolutionScopesSupport.scopeForMemberDeclarationResolution()

    /**
     * 已声明的可调用成员
     */
    @Suppress("UNCHECKED_CAST")
    override val declaredCallableMembers: MutableCollection<CallableMemberDescriptor>
        get() {
            val allDescriptors = DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope)

            return allDescriptors.filterTo(mutableListOf()) { descriptor ->
                when (descriptor) {
                    is CallableMemberDescriptor -> descriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                    is VariableDescriptor -> true
                    else -> false
                }
            } as MutableCollection<CallableMemberDescriptor>
        }

    /**
     * 创建初始化作用域的父描述符
     */
    private fun createInitializerScopeParent(): DeclarationDescriptor {
        // 枚举没有主构造函数的情况下，创建一个合成的函数描述符作为初始化作用域的父描述符
        return object : FunctionDescriptorImpl(
            this@LazyEnumDescriptor, null, Annotations.EMPTY, Name.special("<init-blocks>"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        ) {
            init {
                initialize(
                    null, emptyList(), emptyList(),
                    null, Modality.FINAL, DescriptorVisibilities.PRIVATE
                )
            }

            override fun createSubstitutedCopy(
                newOwner: DeclarationDescriptor,
                original: FunctionDescriptor?,
                kind: CallableMemberDescriptor.Kind,
                newName: Name?,
                annotations: Annotations,
                source: SourceElement
            ): FunctionDescriptorImpl {
                throw UnsupportedOperationException()
            }
        }
    }

    /**
     * 用于初始化块解析的词法作用域
     */
    override val scopeForInitializerResolution: LexicalScope
        get() = _scopeForInitializerResolution.invoke()
    private val _scopeForInitializerResolution = c.storageManager.createLazyValue {
        scopeForEnumInitializerResolution(
            this,
            createInitializerScopeParent(),

            )
    }

    /**
     * 用于类头解析的词法作用域
     */
    override val scopeForClassHeaderResolution: LexicalScope
        get() = resolutionScopesSupport.scopeForClassHeaderResolution()

    /**
     * 用于构造函数头解析的词法作用域
     */
    override val scopeForConstructorHeaderResolution: LexicalScope
        get() = resolutionScopesSupport.scopeForConstructorHeaderResolution()

    /**
     * 类型构造器
     */
    override val typeConstructor: TypeConstructor by lazy {
        EnumTypeConstructorImpl(
            this,
            declaredTypeParameters,
            computeSupertypes(),
            c.storageManager
        )
    }

    /**
     * 计算超类型
     */
    private fun computeSupertypes(): List<CangJieType> {


        if (CangJieBuiltIns.isSpecialClassWithNoSupertypes(this)) {
            return emptyList()
        }

        val classOrObject = declarationProvider.ownerInfo!!.correspondingClass
            ?: return listOf(c.moduleDescriptor.builtIns.stdlibTypes.anyType)

        // 获取枚举声明中的超类型
        val declaredSupertypes = c.descriptorResolver.resolveSupertypes(
            scopeForClassHeaderResolution,
            this,
            classOrObject,
            c.trace
        )

        // 获取通过 extend 声明添加的超类型
        val extendManager = c.moduleDescriptor.projectDescriptor.extendManager
        val extendSupertypes = extendManager?.getExtendSupertypes(
            forConstructor = this.typeConstructor,
            forTypeArgs = emptyList()
        ) ?: emptyList()

        // 合并所有超类型
        val allSupertypes = declaredSupertypes + extendSupertypes

        return allSupertypes.filter(VALID_SUPERTYPE)
    }


    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
        return scopesHolderForClass.getScope(cangjieTypeRefiner)
    }

    /**
     * 静态作用域
     */
    override val staticScope: MemberScope
        get() = StaticMemberScope(unsubstitutedMemberScope)

    /**
     * 实例作用域
     */
    override val instanceScope: MemberScope
        get() = InstanceMemberScope(unsubstitutedMemberScope)


    /**
     * 创建替换后的成员作用域
     */
    override fun createSubstitutedMemberScope(substitutor: ComposableTypeSubstitutor): MemberScope {
        return unsubstitutedMemberScope
    }

    /**
     * 获取所有枚举构造器（包括继承的）
     *
     * 目前仓颉语言枚举不支持继承，所以直接返回本枚举的构造器
     */
    override fun getAllConstructors(): Collection<EnumConstructorDescriptor> {
        return constructors
    }

    /**
     * 检查是否为非穷尽性枚举
     */
    override fun isNonExhaustiveEnum(): Boolean = isNonExhaustive

    override fun forceResolveAllContents() {

    }
    init {
        c.trace.record(BindingContext.CLASS, enumPsi, this)
        c.trace.record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this)
        constructors
    }

}
