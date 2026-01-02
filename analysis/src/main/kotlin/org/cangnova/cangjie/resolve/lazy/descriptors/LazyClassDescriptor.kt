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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.diagnostics.infos.errors.CYCLIC_INHERITANCE_HIERARCHY
import org.cangnova.cangjie.diagnostics.infos.warnings.CYCLIC_SCOPES_WITH_COMPANION
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjSuperTypeListEntry
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveModalityFromModifiers
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.TYPE
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.LazyEntity
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.StaticScopeForCangJieEnum
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 延迟类描述符
 *
 * 采用延迟计算策略的类描述符实现，仅在需要时才解析类的各种信息。
 * 这种设计可以提高性能，避免不必要的解析工作。
 *
 * 支持的类种类包括：
 * - 普通类（CLASS）
 * - 接口（INTERFACE）
 * - 枚举（ENUM）
 * - 对象（OBJECT）
 * - 结构体（STRUCT）等
 *
 * @param c 延迟类上下文，提供解析所需的各种服务
 * @param containingDeclaration 包含该类的声明描述符
 * @param name 类名
 * @param classLikeInfo 类的信息数据
 * @param isExternal 是否是外部类
 */
open class LazyClassDescriptor(
    c: LazyClassContext,
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    classLikeInfo: CjClassLikeInfo,
    isExternal: Boolean
) : LazyClassDescriptorBase(c, containingDeclaration, name, classLikeInfo, isExternal), LazyEntity {

    private var typeStatement: CjTypeStatement? = classLikeInfo.correspondingClass
    private val declarationProvider: ClassMemberDeclarationProvider =
        c.declarationProviderFactory.getClassMemberDeclarationProvider(classLikeInfo)

    private val scopesHolderForClass: ScopesHolderForClass<LazyClassMemberScope> =
        createScopesHolderForClass(c, declarationProvider)

    /**
     * 类的种类（CLASS、INTERFACE、ENUM等）
     */
    override val kind: ClassKind = classLikeInfo.classKind

    /**
     * 类的修饰性（FINAL、OPEN、ABSTRACT、SEALED）
     *
     * 默认值与编译器保持一致：
     * - Interface: OPEN (接口天然可被实现，IsOpen() 返回 true)
     * - Class: FINAL (类默认不可被继承，需显式标记 open 或 abstract)
     */
    private val _modality = c.storageManager.createLazyValue {
        when {
            else -> {
                val defaultModality = if (kind == ClassKind.INTERFACE) Modality.OPEN else Modality.FINAL
                resolveModalityFromModifiers(
                    typeStatement,
                    defaultModality,
                    c.trace.bindingContext,
                    null,
                    allowSealed = true
                )
            }
        }
    }
    override val modality: Modality
        get() = _modality.invoke()

    override val staticScope: MemberScope
        get() = when (kind) {
            ClassKind.ENUM -> StaticScopeForCangJieEnum(c.storageManager, this, enumEntriesCanBeUsed = true)
            else -> MemberScope.Empty
        }

    private val _typeConstructor = LazyClassTypeConstructor()

    private val isLocal = typeStatement?.let { CjPsiUtil.isLocal(it) } ?: false
    override val visibility: DescriptorVisibility = when {
        isLocal -> DescriptorVisibilities.LOCAL
        else -> resolveVisibilityFromModifiers(classLikeInfo.modifierList, DescriptorVisibilities.INTERNAL)
    }

    override val annotations: Annotations
        get() = Annotations.EMPTY
    private val resolutionScopesSupport = ClassResolutionScopesSupport(
        this,
        c.storageManager,
        c.languageVersionSettings,
        ::getOuterScope
    )

    /**
     * 类声明的类型参数列表
     */
    private val _declaredTypeParameters = c.storageManager.createLazyValue {
        val typeParameterList = declarationProvider.ownerInfo?.typeParameterList
            ?: return@createLazyValue emptyList<TypeParameterDescriptor>()

        typeParameterList.parameters.takeIf { it.isNotEmpty() }?.mapIndexed { index, parameter ->
            LazyTypeParameterDescriptor(c, this, parameter, Annotations.EMPTY, index)
        } ?: emptyList()
    }
    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = _declaredTypeParameters.invoke()



    private val freedomForSealedInterfacesSupported = true

    private val _sealedSubclasses = c.storageManager.createLazyValue {
        when (modality) {
            Modality.SEALED -> c.sealedClassInheritorsProvider.computeSealedSubclasses(
                this,
                freedomForSealedInterfacesSupported
            )

            else -> emptyList()
        }
    }

    init {
        typeStatement?.let { c.trace.record(BindingContext.CLASS, it, this) }
        c.trace.record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this)
        init()
    }

    protected open fun init() {}

    protected fun getOuterScope(): LexicalScope =
        c.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)

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
        _typeConstructor.supertypes
        _typeConstructor.parameters.forEach { it.upperBounds }
        unsubstitutedPrimaryConstructor
        visibility

    }

    override val scopeForConstructorHeaderResolution: LexicalScope
        get() = resolutionScopesSupport.scopeForConstructorHeaderResolution()

    protected open fun createScopesHolderForClass(
        c: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider
    ): ScopesHolderForClass<LazyClassMemberScope> = ScopesHolderForClass.create(
        this,
        c.storageManager,
        c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner
    ) { cangjieTypeRefinerForDependentModule ->
        val scopeForDeclaredMembers = when {
            !cangjieTypeRefinerForDependentModule.isRefinementNeededForModule(c.moduleDescriptor) -> null
            else -> scopesHolderForClass.getScope(c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner)
        }

        LazyClassMemberScope(
            c, declarationProvider, this, c.trace, cangjieTypeRefinerForDependentModule,
            scopeForDeclaredMembers
        )
    }


    fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> =
        typeStatement?.superTypeListEntries ?: emptyList()

    private fun createInitializerScopeParent(): DeclarationDescriptor {
        unsubstitutedPrimaryConstructor?.let { return it }

        return object : FunctionDescriptorImpl(
            this@LazyClassDescriptor, null, Annotations.EMPTY, Name.special("<init-blocks>"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        ) {
            init {
                initialize(
                    null,  emptyList(), emptyList(),
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

    @Suppress("UNCHECKED_CAST")
    override val declaredCallableMembers: MutableCollection<CallableMemberDescriptor>
        get() {
            val allDescriptors = DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope)

            // TODO 扩展
            // extendClassDescriptors.forEach {
            //     allDescriptors.addAll(DescriptorUtils.getAllDescriptors(it.unsubstitutedMemberScope))
            // }

            return allDescriptors.filterTo(mutableListOf()) { descriptor ->
                when (descriptor) {
                    is CallableMemberDescriptor -> descriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                    is VariableDescriptor -> true
                    else -> false
                }
            } as MutableCollection<CallableMemberDescriptor>
        }

    override val scopeForInitializerResolution: LexicalScope
        get() = _scopeForInitializerResolution.invoke()
    private val _scopeForInitializerResolution = c.storageManager.createLazyValue {
        scopeForClassInitializerResolution(
            this,
            createInitializerScopeParent(),
            classLikeInfo.primaryConstructorParameters
        )
    }

    /**
     * 类的所有构造函数集合
     * 从未替换的成员作用域中获取构造函数列表
     */
    override val constructors: Collection<ClassConstructorDescriptor>
        get() = (unsubstitutedMemberScope as LazyClassMemberScope).getConstructors()

    /**
     * 类结尾的构造函数集合
     * 从未替换的成员作用域中获取结尾构造函数列表
     */
    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = (unsubstitutedMemberScope as LazyClassMemberScope).getEndConstructors()

    /**
     * 未替换的主构造函数
     * 从未替换的成员作用域中获取主构造函数
     */
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = (unsubstitutedMemberScope as LazyClassMemberScope).getPrimaryConstructor()

    /**
     * 密封类的子类集合
     * 如果不是密封类则返回空集合
     */
    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = _sealedSubclasses.invoke()

    /**
     * 类的类型构造器
     * 负责类型参数和超类型的管理
     */
    override val typeConstructor: TypeConstructor
        get() = _typeConstructor



    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        scopesHolderForClass.getScope(cangjieTypeRefiner)

    override fun forceResolveAllContents() {}

    override val scopeForMemberDeclarationResolution: LexicalScope
        get() = resolutionScopesSupport.scopeForMemberDeclarationResolution()

    override val scopeForClassHeaderResolution: LexicalScope
        get() = resolutionScopesSupport.scopeForClassHeaderResolution()

    override fun toString(): String = typeStatement?.toString() ?: super.toString()



    protected fun computeSupertypes(): Collection<CangJieType> {
        if (CangJieBuiltIns.isSpecialClassWithNoSupertypes(this)) {
            return emptyList()
        }

        val classOrObject = declarationProvider.ownerInfo!!.correspondingClass
            ?: return listOf(c.moduleDescriptor.builtIns.stdlibTypes.anyType)

        val allSupertypes = c.descriptorResolver.resolveSupertypes(
            scopeForClassHeaderResolution,
            this,
            classOrObject,
            c.trace
        )

        return allSupertypes.filter(VALID_SUPERTYPE)
    }

    private inner class LazyClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val _parameters = c.storageManager.createLazyValue {
            this@LazyClassDescriptor.computeConstructorTypeParameters()
        }

        override fun computeSupertypes(): Collection<CangJieType> =
            this@LazyClassDescriptor.computeSupertypes()

        override fun reportSupertypeLoopError(type: CangJieType) {
            val supertypeDescriptor = type.constructor.declarationDescriptor
            if (supertypeDescriptor is ClassDescriptor) {
                reportCyclicInheritanceHierarchyError(c.trace, this@LazyClassDescriptor, supertypeDescriptor)
            }
        }

        // TODO: 添加 LanguageFeature.ProhibitVisibilityOfNestedClassifiersFromSupertypes 后恢复
        override val shouldReportCyclicScopeWithCompanionWarning: Boolean = false

        override fun reportScopesLoopError(type: CangJieType) {
            val reportOn = type.constructor.declarationDescriptor?.let { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                ?.let { if (it is CjClass) it.nameIdentifier else it }

            reportOn?.let { c.trace.report(CYCLIC_SCOPES_WITH_COMPANION.on(it)) }
        }

        private fun reportCyclicInheritanceHierarchyError(
            trace: BindingTrace,
            classDescriptor: ClassDescriptor,
            superclass: ClassDescriptor
        ) {
            val psiElement = DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor)

            var elementToMark: PsiElement? = null
            if (psiElement is CjTypeStatement) {
                for (delegationSpecifier in psiElement.superTypeListEntries) {
                    val typeReference = delegationSpecifier.typeReference ?: continue
                    val supertype = trace[TYPE, typeReference]
                    if (supertype != null && supertype.constructor == superclass.typeConstructor) {
                        elementToMark = typeReference
                    }
                }
            }

            if (elementToMark == null && psiElement is PsiNameIdentifierOwner) {
                elementToMark = psiElement.nameIdentifier
            }

            elementToMark?.let { trace.report(CYCLIC_INHERITANCE_HIERARCHY.on(it)) }
        }

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = c.supertypeLoopChecker

        override val parameters: List<TypeParameterDescriptor>
            get() = _parameters.invoke()

        override val isDenotable: Boolean = true

        override val declarationDescriptor: ClassDescriptor
            get() = this@LazyClassDescriptor

        override fun toString(): String = this@LazyClassDescriptor.name.toString()
    }

    companion object {

    }
}
  val VALID_SUPERTYPE: (CangJieType) -> Boolean = { type ->
    require(!type.isError) { "Error types must be filtered out in DescriptorResolver" }
    TypeUtils.getClassDescriptor(type) != null
}