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

package com.linqingying.cangjie.resolve.lazy.descriptors

import com.linqingying.cangjie.builtins.CangJieBuiltIns.Companion.isSpecialClassWithNoSupertypes
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.ScopesHolderForClass.Companion.create
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ClassDescriptorBase
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.Name.Companion.special
import com.linqingying.cangjie.psi.CjExtend
import com.linqingying.cangjie.psi.CjSuperTypeListEntry
import com.linqingying.cangjie.psi.CjTypeParameterList
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.DescriptorUtils.getAllDescriptors
import com.linqingying.cangjie.resolve.DescriptorUtils.getContainingModule
import com.linqingying.cangjie.resolve.descriptorUtil.getCangJieTypeRefiner
import com.linqingying.cangjie.resolve.descriptorUtil.getSuperClassNotAny
import com.linqingying.cangjie.resolve.lazy.LazyClassContext
import com.linqingying.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.resolve.source.toSourceElement
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.util.TypeUtils.getClassDescriptor
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.storage.NullableLazyValue

abstract class LazyClassDescriptorBase
    (
    protected val c: LazyClassContext,


    containingDec: DeclarationDescriptor,
    name: Name,
    open val classLikeInfo: CjClassLikeInfo,
    isExternal: Boolean
) : ClassDescriptorBase(
    c.storageManager, containingDec, name, classLikeInfo.correspondingClass.toSourceElement(), isExternal
), ClassDescriptorWithResolutionScopes

class LazyExtendClassDescriptor(
    c: LazyClassContext,
    classLikeInfo: CjClassLikeInfo,

    containingDeclaration: DeclarationDescriptor,
    name: Name,
    isgetExtend: Boolean = true,
    searchscope: LexicalScope

) : LazyClassDescriptorBase(
    c,
    containingDeclaration, name,
    classLikeInfo, false
) {

    companion object {
        val VALID_SUPERTYPE: (CangJieType) -> Boolean = { type: CangJieType ->
            assert(!type.isError) { "Error types must be filtered out in DescriptorResolver" }
            getClassDescriptor(type) != null
        }
    }

    private val typeParameterDescriptors: List<TypeParameterDescriptor>
    var classDescriptor: ClassDescriptor  = ErrorUtils.errorClass
    var type: CangJieType  = ErrorUtils.invalidType

    //    val typeParameters: List<TypeParameterDescriptor>
    private var parameters: NullableLazyValue<List<TypeParameterDescriptor>>

    private val storageManager: StorageManager = c.storageManager
    private val typeConstructor = ExtendTypeConstructor()
      val declarationProvider = c.declarationProviderFactory.getClassMemberDeclarationProvider(classLikeInfo)

    private val scopesHolderForClass = createScopesHolderForClass(c, this.declarationProvider)
    private val scopeForInitializerResolution =
        storageManager.createLazyValue {
            scopeForInitializerResolution(
                this, createInitializerScopeParent(), classLikeInfo.primaryConstructorParameters
            )
        }
    val typeStatement: CjExtend = classLikeInfo.correspondingClass as CjExtend

    private val resolutionScopesSupport = ClassResolutionScopesSupport(
        this,
        storageManager,
        c.languageVersionSettings,
    ) { getOuterScope() }


    init {

        c.trace.record<PsiElement, ClassDescriptor>(
            BindingContext.CLASS, typeStatement,
            this
        )


        val typeReceiver = typeStatement.receiverTypeReceiver

        val headerScope = LexicalWritableScope(
            searchscope, this, true,
            TraceBasedLocalRedeclarationChecker(c.trace, c.overloadChecker), LexicalScopeKind.CLASS_HEADER
        )
        this.typeParameterDescriptors = c.descriptorResolver.resolveTypeParametersForDescriptor(
            this,
            headerScope,
            searchscope,
            typeStatement.typeParameters,
            c.trace
        ).apply {
            forEach {
                it.setInitialized()
            }
        }

        this.parameters = c.storageManager.createNullableLazyValue {
            val classInfo = declarationProvider.ownerInfo
            var typeParameterList: CjTypeParameterList? = null
            if (classInfo != null) {
                typeParameterList = classInfo.typeParameterList
            }
            if (typeParameterList == null) return@createNullableLazyValue emptyList<TypeParameterDescriptor>()

            val typeParameters = typeParameterList.parameters
            if (typeParameters.isEmpty()) return@createNullableLazyValue emptyList<TypeParameterDescriptor>()
//
//            boolean supportClassTypeParameterAnnotations = c.getLanguageVersionSettings().supportsFeature(LanguageFeature.ClassTypeParameterAnnotations);
            val parameters: MutableList<TypeParameterDescriptor> = ArrayList(typeParameters.size)

            for (i in typeParameters.indices) {
                val parameter = typeParameters[i]
                val lazyAnnotations = Annotations.EMPTY

                //                if (supportClassTypeParameterAnnotations) {
//                    lazyAnnotations = new LazyAnnotations(
//                            new LazyAnnotationsContext(
//                                    c.getAnnotationResolver(),
//                                    storageManager,
//                                    c.getTrace()
//                            ) {
//                                @NotNull
//                                @Override
//                                public LexicalScope getScope() {
//                                    return getOuterScope();
//                                }
//                            },
//                            parameter.getAnnotationEntries()
//                    );
//                } else {
//                    lazyAnnotations = Annotations.EMPTY;
//                }
                parameters.add(
                    LazyTypeParameterDescriptor(
                        c,
                        this, parameter, lazyAnnotations, i
                    )
                )
            }
            parameters
        }

        val type = typeReceiver?.let { c.typeResolver.resolveType(headerScope, it, c.trace, false, isgetExtend) }
if(type != null){
    this.type = type
}
        this.classDescriptor =  this.type.constructor .declarationDescriptor as? ClassDescriptor ?: ErrorUtils.errorClass


    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyExtendClassDescriptor) return false
        if (!super.equals(other)) return false

        if (typeStatement.getExtendId() != other.typeStatement.getExtendId()) return false

        return true
    }
    val extendId:String get() {
        return typeStatement.getExtendId()
    }

    override fun toString(): String {
        return "extend $classDescriptor"
    }
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> =emptySet()

    @OptIn(TypeRefinement::class)
    private fun createScopesHolderForClass(
        c: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider
    ): ScopesHolderForClass<LazyClassMemberScope> {
        return create(
            this,
            c.storageManager,
            c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner
        ) { cangjieTypeRefinerForDependentModule: CangJieTypeRefiner ->
            val scopeForDeclaredMembers =
                if (!cangjieTypeRefinerForDependentModule.isRefinementNeededForModule(c.moduleDescriptor)
                ) null
                else scopesHolderForClass.getScope(c.cangjieTypeCheckerOfOwnerModule.cangjieTypeRefiner) // essentially, a scope for owner-module
            LazyClassMemberScope(
                c, declarationProvider, this, c.trace, cangjieTypeRefinerForDependentModule,
                scopeForDeclaredMembers
            )
        }
    }

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
//        return getUnsubstitutedMemberScope(getContainingModule(this).getCangJieTypeRefiner())
        return scopesHolderForClass.getScope(cangjieTypeRefiner)

    }

    @OptIn(TypeRefinement::class)
    override fun getUnsubstitutedMemberScope(): MemberScope {
        return getUnsubstitutedMemberScope(getContainingModule(this).getCangJieTypeRefiner())

    }

    private inner class ExtendTypeConstructor : AbstractClassTypeConstructor(storageManager) {
        private val parameters =
            c.storageManager.createLazyValue { this@LazyExtendClassDescriptor.computeConstructorTypeParameters() }

        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
            return emptyList()
        }

        override fun computeSupertypes(): Collection<CangJieType> {
            return this@LazyExtendClassDescriptor.computeSupertypes()

        }

        override fun getDeclarationDescriptor(): ClassDescriptor = this@LazyExtendClassDescriptor

        override fun isDenotable(): Boolean = true

        override fun getParameters(): List<TypeParameterDescriptor> {
            return parameters()
        }

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = c.supertypeLoopChecker

    }

    private fun computeSupertypes(): List<CangJieType> {
        if (isSpecialClassWithNoSupertypes(this)) {
            return emptyList()
        }
        val trace: BindingTrace = c.trace
        val scope: LexicalScope = scopeForClassHeaderResolution
        val classOrObject = declarationProvider.ownerInfo!!.correspondingClass
            ?: return listOf(c.moduleDescriptor.builtIns.anyType)


        val allSupertypes =
            c.descriptorResolver.resolveSupertypes(scope, this, classOrObject, trace)

        return ArrayList(allSupertypes.filter(VALID_SUPERTYPE))
    }

    private fun getOuterScope(): LexicalScope {
        return c.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)
    }


    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getModality(): Modality {
        return Modality.FINAL
    }

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> {

        return parameters() ?: emptyList()
    }

    override fun getStaticScope(): MemberScope {
        return MemberScope.Empty
    }

    override fun getConstructors(): Collection<ClassConstructorDescriptor> {
        return (unsubstitutedMemberScope as LazyClassMemberScope).getConstructors()

    }

    fun getSourceClassElement(): CjTypeStatement? {
        return (classDescriptor as? LazyClassDescriptor)?.classLikeInfo?.correspondingClass
    }

    override fun getKind(): ClassKind {
        return ClassKind.EXTEND
    }

    fun getSourceClassKind(): ClassKind {
        return classDescriptor.kind
    }

    override fun isFun(): Boolean {
        return false
    }

    override fun isValue(): Boolean {
        return false
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        return null
    }

    override fun getSealedSubclasses(): Collection<ClassDescriptor> {
        return classDescriptor.sealedSubclasses
    }


    override fun getScopeForMemberDeclarationResolution(): LexicalScope {
        return resolutionScopesSupport.scopeForMemberDeclarationResolution.invoke()

    }

    override fun getDeclaredCallableMembers(): List<CallableMemberDescriptor> {
        return getAllDescriptors(unsubstitutedMemberScope)
            .filter {
                it is CallableMemberDescriptor && it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            } as List<CallableMemberDescriptor>
    }


    private fun createInitializerScopeParent(): DeclarationDescriptor {
        val primaryConstructor: ConstructorDescriptor? = unsubstitutedPrimaryConstructor
        if (primaryConstructor != null) return primaryConstructor

        return object : FunctionDescriptorImpl(
            this@LazyExtendClassDescriptor, null, Annotations.EMPTY, special("<init-blocks>"),
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

    override fun getScopeForInitializerResolution(): LexicalScope {
        return scopeForInitializerResolution()

    }

    override fun getScopeForClassHeaderResolution(): LexicalScope {
        return resolutionScopesSupport.scopeForClassHeaderResolution.invoke()

    }

    override fun getScopeForConstructorHeaderResolution(): LexicalScope {
        return resolutionScopesSupport.scopeForConstructorHeaderResolution.invoke()

    }


    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> {
        return classDescriptor.superTypeListEntries ?: emptyList()
    }

    fun resolveMemberHeaders() {
        //    ForceResolveUtil.forceResolveAllContents(getDanglingAnnotations());
        getSuperClassNotAny()
//        constructors
//        containingDeclaration
//        unsubstitutedMemberScope
    }

    override fun hashCode(): Int {
        var result = classDescriptor.hashCode()
        result = 31 * result + storageManager.hashCode()
        result = 31 * result + typeConstructor.hashCode()
        result = 31 * result + declarationProvider.hashCode()
        result = 31 * result + scopesHolderForClass.hashCode()
        result = 31 * result + scopeForInitializerResolution.hashCode()
        result = 31 * result + typeStatement.hashCode()
        result = 31 * result + resolutionScopesSupport.hashCode()
        return result
    }


}
