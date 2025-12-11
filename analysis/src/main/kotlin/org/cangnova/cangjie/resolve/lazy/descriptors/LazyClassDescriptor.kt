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
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveModalityFromModifiers
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.LazyEntity
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.StaticScopeForCangJieEnum
import org.cangnova.cangjie.types.AbstractClassTypeConstructor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.isError

class LazyClassDescriptor(
    private val c: LazyClassContext,
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

    private val kind: ClassKind = classLikeInfo.classKind

    private val staticScope: MemberScope = when (kind) {
        ClassKind.ENUM -> StaticScopeForCangJieEnum(c.storageManager, this, enumEntriesCanBeUsed = true)
        else -> MemberScope.Empty
    }

    private val typeConstructor = LazyClassTypeConstructor()

    private val modality by c.storageManager.createLazyValue {
        when {
            kind.isObject -> Modality.FINAL
            else -> {
                val defaultModality = if (kind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
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

    private val parameters by c.storageManager.createLazyValue {
        val classInfo = declarationProvider.ownerInfo
        val typeParameterList =
            classInfo?.typeParameterList ?: return@createLazyValue emptyList<TypeParameterDescriptor>()

        val typeParameters = typeParameterList.parameters
        if (typeParameters.isEmpty()) return@createLazyValue emptyList<TypeParameterDescriptor>()

        typeParameters.mapIndexed { index, parameter ->
            LazyTypeParameterDescriptor(c, this, parameter, Annotations.EMPTY, index)
        }
    }

    private val scopeForInitializerResolution by c.storageManager.createLazyValue {
        ClassResolutionScopesSupportKt.scopeForInitializerResolution(
            this,
            createInitializerScopeParent(),
            classLikeInfo.primaryConstructorParameters
        )
    }

    private val freedomForSealedInterfacesSupported =
        c.languageVersionSettings.supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage)

    private val sealedSubclasses by c.storageManager.createLazyValue {
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
        c.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo.scopeAnchor)

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
        typeConstructor.supertypes
        typeConstructor.parameters.forEach { it.upperBounds }
        unsubstitutedPrimaryConstructor
        visibility
        contextReceivers
    }

    override fun getScopeForConstructorHeaderResolution(): LexicalScope =
        resolutionScopesSupport.scopeForConstructorHeaderResolution()

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


    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> =
        typeStatement?.superTypeListEntries ?: emptyList()

    override fun getUnsubstitutedInnerClassesScope(): MemberScope =
        super.getUnsubstitutedInnerClassesScope()

    private fun createInitializerScopeParent(): DeclarationDescriptor {
        unsubstitutedPrimaryConstructor?.let { return it }

        return object : FunctionDescriptorImpl(
            this@LazyClassDescriptor, null, Annotations.EMPTY, Name.special("<init-blocks>"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        ) {
            init {
                initialize(
                    null, null, emptyList(), emptyList(), emptyList(),
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
    override fun getDeclaredCallableMembers(): Collection<CallableMemberDescriptor> {
        val list = ArrayList(DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope))

        // TODO 扩展
        // extendClassDescriptors.forEach {
        //     list.addAll(DescriptorUtils.getAllDescriptors(it.unsubstitutedMemberScope))
        // }

        return list.filter { descriptor ->
            (descriptor is CallableMemberDescriptor && descriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                    || descriptor is VariableDescriptor
        } as Collection<CallableMemberDescriptor>
    }

    override fun getScopeForInitializerResolution(): LexicalScope = scopeForInitializerResolution

    override fun getUnsubstitutedMemberScope(): MemberScope =
        getUnsubstitutedMemberScope(DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())

    override fun getStaticScope(): MemberScope = staticScope

    override fun getConstructors(): Collection<ClassConstructorDescriptor> =
        (unsubstitutedMemberScope as LazyClassMemberScope).getConstructors()

    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> =
        (unsubstitutedMemberScope as LazyClassMemberScope).getEndConstructors()

    override fun getKind(): ClassKind = kind

    override fun getModality(): Modality = modality

    override fun isFun(): Boolean = false

    override fun isValue(): Boolean = false

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? =
        (unsubstitutedMemberScope as LazyClassMemberScope).primaryConstructor

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = parameters

    override fun getSealedSubclasses(): Collection<ClassDescriptor> = sealedSubclasses

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    @Deprecated("Use setExtendData with proper parameters")
    fun setExtendData(typeStatement: CjTypeStatement, extendTrace: BindingTrace, extendScope: LexicalScope) {
        this.typeStatement = typeStatement
    }

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        scopesHolderForClass.getScope(cangjieTypeRefiner)

    override fun forceResolveAllContents() {}

    override fun getScopeForMemberDeclarationResolution(): LexicalScope =
        resolutionScopesSupport.scopeForMemberDeclarationResolution()

    override fun getScopeForClassHeaderResolution(): LexicalScope =
        resolutionScopesSupport.scopeForClassHeaderResolution()

    override fun toString(): String = typeStatement?.toString() ?: super.toString()

    protected fun computeExtendSuperTypes(extendId: String): Collection<CangJieType> =
        extendClassDescriptors.flatMap { extendDescriptor ->
            when {
                extendDescriptor.typeStatement.extendId != extendId ->
                    extendDescriptor.typeConstructor.supertypes

                else -> emptyList()
            }
        }

    protected fun computeSupertypes(): Collection<CangJieType> {
        if (CangJieBuiltIns.isSpecialClassWithNoSupertypes(this)) {
            return emptyList()
        }

        val classOrObject = declarationProvider.ownerInfo.correspondingClass
            ?: return listOf(c.moduleDescriptor.builtIns.anyType)

        val allSupertypes = c.descriptorResolver.resolveSupertypes(
            scopeForClassHeaderResolution,
            this,
            classOrObject,
            c.trace
        )

        return allSupertypes.filter(VALID_SUPERTYPE)
    }

    private inner class LazyClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val parameters by c.storageManager.createLazyValue {
            TypeParameterUtilsKt.computeConstructorTypeParameters(this@LazyClassDescriptor)
        }

        override fun computeSupertypes(): Collection<CangJieType> =
            this@LazyClassDescriptor.computeSupertypes()


        override fun reportSupertypeLoopError(type: CangJieType) {
            val supertypeDescriptor = type.constructor.declarationDescriptor
            if (supertypeDescriptor is ClassDescriptor) {
                reportCyclicInheritanceHierarchyError(c.trace, this@LazyClassDescriptor, supertypeDescriptor)
            }
        }

        override fun getShouldReportCyclicScopeWithCompanionWarning(): Boolean =
            !c.languageVersionSettings.supportsFeature(
                LanguageFeature.ProhibitVisibilityOfNestedClassifiersFromSupertypes
            )

        override fun reportScopesLoopError(type: CangJieType) {
            var reportOn = DescriptorToSourceUtils.getSourceFromDescriptor(type.constructor.declarationDescriptor)

            if (reportOn is CjClass) {
                reportOn = reportOn.nameIdentifier
            }

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

        override fun getSupertypeLoopChecker(): SupertypeLoopChecker = c.supertypeLoopChecker

        override fun getParameters(): List<TypeParameterDescriptor> = parameters

        override fun isDenotable(): Boolean = true

        override fun getDeclarationDescriptor(): ClassDescriptor = this@LazyClassDescriptor

        override fun toString(): String = this@LazyClassDescriptor.name.toString()
    }

    companion object {
        private val VALID_SUPERTYPE: (CangJieType) -> Boolean = { type ->
            require(!type.isError()) { "Error types must be filtered out in DescriptorResolver" }
            TypeUtils.getClassDescriptor(type) != null
        }
    }
}
