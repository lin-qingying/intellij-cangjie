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
import org.cangnova.cangjie.descriptors.extend.AbstractExtendDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ExtendWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DeserializedDeclarationsFromSupertypeConflictDataKey
import org.cangnova.cangjie.resolve.NonReportingOverrideStrategy
import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.serialization.deserialization.DeserializationContext
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.flatMapToNullable

class DeserializedExtendDescriptor(
    outerContext: DeserializationContext,
    val `extend`: ExtendWrapper,

    val metadataVersion: BinaryVersion,
    override val source: SourceElement
) : AbstractExtendDescriptor(
    outerContext.storageManager,

    ) {
    private val funcList = `extend`.functions
    private val varList = `extend`.variables
    private val propList = `extend`.propertys
    val c = outerContext.childContext(
        this, `extend`.typeParameters, outerContext.`package`,
        metadataVersion
    )
    override val unsubstitutedMemberScope: MemberScope = DeserializedExtendMemberScope()

    private inner class DeserializedExtendMemberScope() :
        DeserializedMemberScope(c, funcList, varList, propList, emptyList(), emptyList()) {
        override fun createClassId(name: Name) = ClassId(
            c.`package`.packageName, name
        )

        private val extendDescriptor: DeserializedExtendDescriptor get() = this@DeserializedExtendDescriptor
        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(
                DescriptorKindFilter.ALL,
                MemberScope.ALL_NAME_FILTER,
                NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
            )
        }
        private val refinedSupertypes = c.storageManager.createLazyValue {

            this@DeserializedExtendDescriptor.superTypes
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
                this@DeserializedExtendDescriptor,
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
                    this@DeserializedExtendDescriptor
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
                extendDescriptor,
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

            return extendDescriptor.superTypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.functionNames
            }
                .apply { addAll(c.components.additionalClassPartsProvider.getFunctionsNames(this@DeserializedExtendDescriptor)) }
        }

        override fun getNonDeclaredVariableNames(): Set<Name> {
            return extendDescriptor.superTypes.flatMapTo(LinkedHashSet()) {
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
            return extendDescriptor.superTypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.propertyNames
            }

        }

        override fun recordLookup(name: Name, location: LookupLocation) {
            c.components.lookupTracker.record(location, extendDescriptor, name)
        }

        override fun getNonDeclaredClassifierNames(): Set<Name>? {
            return extendDescriptor.superTypes.flatMapToNullable(LinkedHashSet()) {
                it.memberScope.classifierNames
            }
        }

    }

    override val extendId: String
        get() = extend.id
    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = c.typeDeserializer.ownTypeParameters
    override val extendType: CangJieType
        get() = c.typeDeserializer.type(extend.type)

    override val superTypes: List<CangJieType>
        get() = extend.superTypes.map { c.typeDeserializer.type(it) }

    override val containingDeclaration = outerContext.containingDeclaration

    override val declaredCallableMembers: Collection<CallableMemberDescriptor>
        get() = unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.DECLARATION }

}