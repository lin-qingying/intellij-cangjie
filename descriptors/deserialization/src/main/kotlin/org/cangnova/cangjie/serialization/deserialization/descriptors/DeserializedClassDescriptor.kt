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

package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.AbstractClassDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.fb.FbDecl
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.serialization.deserialization.DeserializationContext
import org.cangnova.cangjie.types.AbstractClassTypeConstructor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor

import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.utils.flatMapToNullable


class DeserializedClassDescriptor(
    outerContext: DeserializationContext,
    val `class`: ClassDeclWrapper,

    val metadataVersion: BinaryVersion,
    override val source: SourceElement
) : AbstractClassDescriptor(
    outerContext.storageManager,
    `class`.name
), DeserializedDescriptor {
    private val classId = `class`.classId

    private val funcList = `class`.functions
    private val varList = `class`.variables
    private val propList = `class`.propertys
    private val secondaryConstructors: MutableList<FbDecl> = mutableListOf()


    val c = outerContext.childContext(
        this, `class`.typeParameters, outerContext.`package`,
        metadataVersion
    )
    private val memberScopeHolder =
        ScopesHolderForClass.create(
            this,
            c.storageManager,
            c.components.cangjieTypeChecker.cangjieTypeRefiner,
            this::DeserializedClassMemberScope
        )

    private inner class DeserializedClassMemberScope(private val cangjieTypeRefiner: CangJieTypeRefiner) :
        DeserializedMemberScope(c, funcList, varList, propList, emptyList(), emptyList()) {
        override fun createClassId(name: Name) = classId.createNestedClassId(name)
        private val classDescriptor: DeserializedClassDescriptor get() = this@DeserializedClassDescriptor
        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(
                DescriptorKindFilter.ALL,
                MemberScope.ALL_NAME_FILTER,
                NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
            )
        }
        private val refinedSupertypes = c.storageManager.createLazyValue {

            cangjieTypeRefiner.refineSupertypes(classDescriptor)
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> = allDescriptors()

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {
            recordLookup(name, location)
            return super.getContributedFunctions(name, location)
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
            recordLookup(name, location)
            return super.getContributedVariables(name, location)
        }

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            recordLookup(name, location)
            return super.getContributedPropertys(name, location)
        }

        override fun isDeclaredFunctionAvailable(function: SimpleFunctionDescriptor): Boolean {
            return c.components.platformDependentDeclarationFilter.isFunctionAvailable(
                this@DeserializedClassDescriptor,
                function
            )
        }

        override fun computeNonDeclaredFunctions(name: Name, functions: MutableList<SimpleFunctionDescriptor>) {
            val fromSupertypes = ArrayList<SimpleFunctionDescriptor>()
            for (supertype in refinedSupertypes()) {
                fromSupertypes.addAll(
                    supertype.memberScope.getContributedFunctions(
                        name,
                        NoLookupLocation.FOR_ALREADY_TRACKED
                    )
                )
            }

            functions.addAll(
                c.components.additionalClassPartsProvider.getFunctions(
                    name,
                    this@DeserializedClassDescriptor
                )
            )
            generateFakeOverrides(name, fromSupertypes, functions)
        }

        override fun computeNonDeclaredVariables(name: Name, descriptors: MutableList<VariableDescriptor>) {
            val fromSupertypes = ArrayList<VariableDescriptor>()
            for (supertype in refinedSupertypes()) {
                fromSupertypes.addAll(
                    supertype.memberScope.getContributedVariables(
                        name,
                        NoLookupLocation.FOR_ALREADY_TRACKED
                    )
                )
            }
//            generateFakeOverrides(name, fromSupertypes, descriptors)
        }

        override fun computeNonDeclaredProperties(name: Name, descriptors: MutableList<PropertyDescriptor>) {
            val fromSupertypes = ArrayList<PropertyDescriptor>()
            for (supertype in refinedSupertypes()) {
                fromSupertypes.addAll(
                    supertype.memberScope.getContributedPropertys(
                        name,
                        NoLookupLocation.FOR_ALREADY_TRACKED
                    )
                )
            }
            generateFakeOverrides(name, fromSupertypes, descriptors)
        }

        private fun <D : CallableMemberDescriptor> generateFakeOverrides(
            name: Name,
            fromSupertypes: Collection<D>,
            result: MutableList<D>
        ) {
            val fromCurrent = ArrayList<CallableMemberDescriptor>(result)
            c.components.cangjieTypeChecker.overridingUtil.generateOverridesInFunctionGroup(
                name,
                fromSupertypes,
                fromCurrent,
                classDescriptor,
                object : NonReportingOverrideStrategy() {
                    override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                        // TODO: report "cannot infer visibility"
                        OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                        @Suppress("UNCHECKED_CAST")
                        result.add(fakeOverride as D)
                    }

                    override fun conflict(
                        fromSuper: CallableMemberDescriptor,
                        fromCurrent: CallableMemberDescriptor
                    ) {
                        if (fromCurrent is FunctionDescriptorImpl) {
                            fromCurrent.putInUserDataMap(
                                DeserializedDeclarationsFromSupertypeConflictDataKey,
                                fromSuper
                            )
                        }
                    }
                })
        }

        override fun getNonDeclaredFunctionNames(): Set<Name> {

            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.functionNames
            }
                .apply { addAll(c.components.additionalClassPartsProvider.getFunctionsNames(this@DeserializedClassDescriptor)) }
        }

        override fun getNonDeclaredVariableNames(): Set<Name> {
            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.variableNames
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            recordLookup(name, location)
            return super.getContributedClassifier(name, location)
        }


        override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
            recordLookup(name, location)
            return super.getContributedClassifiers(name, location)
        }

        override fun getNonDeclaredPropertyNames(): Set<Name> {
            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.propertyNames
            }

        }

        override fun recordLookup(name: Name, location: LookupLocation) {
            c.components.lookupTracker.record(location, classDescriptor, name)
        }

        override fun getNonDeclaredClassifierNames(): Set<Name>? {
            return classDescriptor.typeConstructor.supertypes.flatMapToNullable(LinkedHashSet()) {
                it.memberScope.classifierNames
            }
        }

    }

    private val memberScope get() = memberScopeHolder.getScope(c.components.cangjieTypeChecker.cangjieTypeRefiner)

    internal fun hasNestedClass(name: Name): Boolean =
        name in memberScope.classNames

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        memberScopeHolder.getScope(cangjieTypeRefiner)

    override val staticScope: MemberScope
        get() = MemberScope.Empty
    private val constructorsValue = c.storageManager.createLazyValue { computeConstructors() }
    private fun computeConstructors(): Collection<ClassConstructorDescriptor> =
        computeSecondaryConstructors() + listOfNotNull(unsubstitutedPrimaryConstructor) +
                c.components.additionalClassPartsProvider.getConstructors(this)

    private fun computeSecondaryConstructors(): List<ClassConstructorDescriptor> =
        `class`.constructors.filter { !it.isPrimary }.map {
            c.declDeserializer.loadConstructor(it, false)
        }

    override val constructors: Collection<ClassConstructorDescriptor>
        get() = constructorsValue()
    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = emptyList()

    override val containingDeclaration = outerContext.containingDeclaration

    override val kind: ClassKind = `class`.kind

    override val modality: Modality = `class`.modality

    //    由于序列化文件中不区分主副构造函数
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = primaryConstructor()
    private val primaryConstructor = c.storageManager.createNullableLazyValue { computePrimaryConstructor() }

    private fun computePrimaryConstructor(): ClassConstructorDescriptor? {
        return `class`.constructors.firstOrNull { it.isPrimary }?.let { constructor ->
            c.declDeserializer.loadConstructor(constructor, true)
        }
    }

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = c.typeDeserializer.ownTypeParameters
    private val _sealedSubclasses = c.storageManager.createLazyValue { computeSubclassesForSealedClass() }

    private fun computeSubclassesForSealedClass(): Collection<ClassDescriptor> {
        return emptyList()
    }

    override fun toString() =
        "deserialized ${kind} $name" // not using descriptor renderer to preserve laziness


    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = _sealedSubclasses()


    override val typeConstructor: TypeConstructor = DeserializedClassTypeConstructor()

    private inner class DeserializedClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val _parameters = c.storageManager.createLazyValue {
            this@DeserializedClassDescriptor.computeConstructorTypeParameters()
        }



        override fun computeSupertypes(): Collection<CangJieType> {

            val result = `class`.superTypes.toSet().map { supertype ->
                c.typeDeserializer.type(supertype)
            } + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

            val unresolved = result.mapNotNull { supertype ->
                supertype.constructor.declarationDescriptor as? NotFoundClasses.MockClassDescriptor
            }

            if (unresolved.isNotEmpty()) {
                c.components.errorReporter.reportIncompleteHierarchy(
                    this@DeserializedClassDescriptor,
                    unresolved.map { it.classId?.asSingleFqName()?.asString() ?: it.name.asString() }
                )
            }

            return result.toList()
        }


        override val parameters: List<TypeParameterDescriptor>
            get() = _parameters()
        override val isDenotable: Boolean
            get() = true
        override val declarationDescriptor: ClassDescriptor = this@DeserializedClassDescriptor
        override fun toString() = name.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            // TODO: inject implementation
            get() = SupertypeLoopChecker.EMPTY
    }


}
