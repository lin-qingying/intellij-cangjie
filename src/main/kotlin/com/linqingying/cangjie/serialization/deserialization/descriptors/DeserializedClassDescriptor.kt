package com.linqingying.cangjie.serialization.deserialization.descriptors

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.AbstractClassDescriptor
import com.linqingying.cangjie.descriptors.impl.EnumEntrySyntheticClassDescriptor
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ReceiverParameterDescriptorImpl
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.incremental.record
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.*
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.descriptorUtil.classId
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.resolve.scopes.StaticScopeForCangJieEnum
import com.linqingying.cangjie.resolve.scopes.receivers.ContextClassReceiver
import com.linqingying.cangjie.serialization.deserialization.*
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.utils.flatMapToNullable

class DeserializedClassDescriptor(
    outerContext: DeserializationContext,
    val classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    val metadataVersion: BinaryVersion,
    override val source: SourceElement
) : AbstractClassDescriptor(
    outerContext.storageManager,
    nameResolver.getClassId(classProto.fqName).shortClassName
), DeserializedDescriptor {
    private val classId = nameResolver.getClassId(classProto.fqName)

    private val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(classProto.flags))
    override val visibility = ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(classProto.flags))
    private val kind = ProtoEnumFlags.classKind(Flags.CLASS_KIND.get(classProto.flags))

    val c = outerContext.childContext(
        this, classProto.typeParameterList, nameResolver, TypeTable(classProto.typeTable),
        VersionRequirementTable.create(classProto.versionRequirementTable), metadataVersion
    )

    val hasEnumEntriesMetadataFlag: Boolean = Flags.HAS_ENUM_ENTRIES.get(classProto.flags)

    private val staticScope =
        if (kind == ClassKind.ENUM) {
            val enumEntriesCanBeUsed = hasEnumEntriesMetadataFlag ||
                    c.components.enumEntriesDeserializationSupport.canSynthesizeEnumEntries() == true
            StaticScopeForCangJieEnum(c.storageManager, this, enumEntriesCanBeUsed)
        } else {
            MemberScope.Empty
        }

    private val typeConstructor = DeserializedClassTypeConstructor()

    private val memberScopeHolder =
        ScopesHolderForClass.create(
            this,
            c.storageManager,
            c.components.cangjieTypeChecker.cangjieTypeRefiner,
            this::DeserializedClassMemberScope
        )

    private val memberScope get() = memberScopeHolder.getScope(c.components.cangjieTypeChecker.cangjieTypeRefiner)
    private val enumEntries = if (kind == ClassKind.ENUM) EnumEntryClassDescriptors() else null

    override val containingDeclaration = outerContext.containingDeclaration
    private val primaryConstructor = c.storageManager.createNullableLazyValue { computePrimaryConstructor() }
    private val constructors = c.storageManager.createLazyValue { computeConstructors() }
    private val sealedSubclasses = c.storageManager.createLazyValue { computeSubclassesForSealedClass() }
//    private val valueClassRepresentation = c.storageManager.createNullableLazyValue { computeValueClassRepresentation() }

    internal val thisAsProtoContainer: ProtoContainer.Class = ProtoContainer.Class(
        classProto, c.nameResolver, c.typeTable, source,
        (containingDeclaration as? DeserializedClassDescriptor)?.thisAsProtoContainer
    )

    val versionRequirements: List<VersionRequirement>
        get() = VersionRequirement.create(classProto, c.nameResolver, c.versionRequirementTable)

    override val annotations =
        if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) {
            Annotations.EMPTY
        } else NonEmptyDeserializedAnnotations(c.storageManager) {
            c.components.annotationAndConstantLoader.loadClassAnnotations(thisAsProtoContainer).toList()
        }


    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getKind() = kind

    override fun getModality() = modality


    override fun isInner() = Flags.IS_INNER.get(classProto.flags)


    override fun isExpect() = Flags.IS_EXPECT_CLASS.get(classProto.flags)

    override val isStatic: Boolean = Flags.IS_STATIC.get(classProto.flags)
    override fun isFun() = Flags.IS_FUN_INTERFACE.get(classProto.flags)

    override fun isValue() = Flags.IS_VALUE_CLASS.get(classProto.flags) && metadataVersion.isAtLeast(1, 4, 2)

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        memberScopeHolder.getScope(cangjieTypeRefiner)

    override fun getStaticScope() = staticScope


    private fun computePrimaryConstructor(): ClassConstructorDescriptor? {
        if (kind.isSingleton) {
            return DescriptorFactory.createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE).apply {
                returnType = getDefaultType()
            }
        }

        return classProto.constructorList.firstOrNull { !Flags.IS_SECONDARY.get(it.flags) }?.let { constructorProto ->
            c.memberDeserializer.loadConstructor(constructorProto, true)
        }
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = primaryConstructor()

    private fun computeConstructors(): Collection<ClassConstructorDescriptor> =
        computeSecondaryConstructors() + listOfNotNull(unsubstitutedPrimaryConstructor) +
                c.components.additionalClassPartsProvider.getConstructors(this)

    private fun computeSecondaryConstructors(): List<ClassConstructorDescriptor> =
        classProto.constructorList.filter { Flags.IS_SECONDARY.get(it.flags) }.map {
            c.memberDeserializer.loadConstructor(it, false)
        }

    override fun getConstructors() = constructors()
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> {
        return emptyList()
    }

    override fun getContextReceivers(): List<ReceiverParameterDescriptor> =
        classProto.contextReceiverTypes(c.typeTable).map {
            val contextReceiverType = c.typeDeserializer.type(it)
            ReceiverParameterDescriptorImpl(
                thisAsReceiverParameter,
                ContextClassReceiver(
                    this,
                    contextReceiverType,
                    /* customLabelName = */ null/*todo store custom label name in metadata?*/,
                    null
                ),
                Annotations.EMPTY
            )
        }

    private fun computeCompanionObjectDescriptor(): ClassDescriptor? {
        if (!classProto.hasCompanionObjectName()) return null

        val companionObjectName = c.nameResolver.getName(classProto.companionObjectName)
        return memberScope.getContributedClassifier(
            companionObjectName,
            NoLookupLocation.FROM_DESERIALIZATION
        ) as? ClassDescriptor
    }


    internal fun hasNestedClass(name: Name): Boolean =
        name in memberScope.classNames

    private fun computeSubclassesForSealedClass(): Collection<ClassDescriptor> {
        if (modality != Modality.SEALED) return emptyList()

        val fqNames = classProto.sealedSubclassFqNameList
        if (fqNames.isNotEmpty()) {
            return fqNames.mapNotNull { index ->
                c.components.deserializeClass(c.nameResolver.getClassId(index))
            }
        }

        // This is needed because classes compiled with CangJie 1.0 did not contain the sealed_subclass_fq_name field
        return CliSealedClassInheritorsProvider.computeSealedSubclasses(
            this,
            allowSealedInheritorsInDifferentFilesOfSamePackage = false
        )
    }

    override fun getSealedSubclasses() = sealedSubclasses()

//    override fun getValueClassRepresentation(): ValueClassRepresentation<SimpleType>? = valueClassRepresentation()
//
//    private fun computeValueClassRepresentation(): ValueClassRepresentation<SimpleType>? {
//        if (!isInline && !isValue) return null
//        classProto.loadValueClassRepresentation(c.nameResolver, c.typeTable, c.typeDeserializer::simpleType, ::getValueClassPropertyType)
//            ?.let { return it }
//        if (!metadataVersion.isAtLeast(1, 5, 1)) {
//            // Before 1.5, inline classes did not have underlying property name & type in the metadata.
//            // However, they were experimental, so supposedly this logic can be removed at some point in the future.
//            val constructor = unsubstitutedPrimaryConstructor ?: error("Inline class has no primary constructor: $this")
//            val propertyName = constructor.valueParameters.first().name
//            val propertyType = getValueClassPropertyType(propertyName) ?: error("Value class has no underlying property: $this")
//            return InlineClassRepresentation(propertyName, propertyType)
//        }
//        return null
//    }

    private fun getValueClassPropertyType(propertyName: Name): SimpleType? =
        memberScope.getContributedVariables(propertyName, NoLookupLocation.FROM_DESERIALIZATION)
            .singleOrNull { it.extensionReceiverParameter == null }?.type as SimpleType?

    override fun toString() =
        "deserialized ${if (isExpect) "expect " else ""}class $name" // not using descriptor renderer to preserve laziness


    override fun getDeclaredTypeParameters() = c.typeDeserializer.ownTypeParameters

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? {
        return c.components.samConversionResolver.resolveFunctionTypeIfSamInterface(this)
    }

    override fun isDefinitelyNotSamInterface() = !isFun

    private inner class DeserializedClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val parameters = c.storageManager.createLazyValue {
            this@DeserializedClassDescriptor.computeConstructorTypeParameters()
        }

        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
            return emptyList()
        }

        override fun computeSupertypes(): Collection<CangJieType> {
            val result = classProto.supertypes(c.typeTable).map { supertypeProto ->
                c.typeDeserializer.type(supertypeProto)
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

        override fun getParameters() = parameters()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@DeserializedClassDescriptor

        override fun toString() = name.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            // TODO: inject implementation
            get() = SupertypeLoopChecker.EMPTY
    }

    private inner class DeserializedClassMemberScope(private val cangjieTypeRefiner: CangJieTypeRefiner) :
        DeserializedMemberScope(
            c, classProto.functionList, classProto.variableList, classProto.propertyList, classProto.typeAliasList,
            classProto.nestedClassNameList.map(c.nameResolver::getName).let { { it } }
        ) {
        private val classDescriptor: DeserializedClassDescriptor get() = this@DeserializedClassDescriptor

        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(
                DescriptorKindFilter.ALL,
                MemberScope.ALL_NAME_FILTER,
                NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
            )
        }

        private val refinedSupertypes = c.storageManager.createLazyValue {
            @OptIn(TypeRefinement::class)
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
                it.memberScope.getFunctionNames()
            }
                .apply { addAll(c.components.additionalClassPartsProvider.getFunctionsNames(this@DeserializedClassDescriptor)) }
        }

        override fun getNonDeclaredVariableNames(): Set<Name> {
            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.getVariableNames()
            }
        }

        override fun getNonDeclaredPropertyNames(): Set<Name> {
            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.getPropertyNames()
            }

        }

        override fun getNonDeclaredClassifierNames(): Set<Name>? {
            return classDescriptor.typeConstructor.supertypes.flatMapToNullable(LinkedHashSet()) {
                it.memberScope.getClassifierNames()
            }
        }

        override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
            recordLookup(name, location)
            classDescriptor.enumEntries?.findEnumEntrys(name)?.let { return it.toList() }
            return super.getContributedClassifiers(name, location)
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            recordLookup(name, location)
            classDescriptor.enumEntries?.findEnumEntry(name)?.let { return it }
            return super.getContributedClassifier(name, location)
        }

        override fun createClassId(name: Name) = classId.createNestedClassId(name)

        override fun addEnumEntryDescriptors(
            result: MutableCollection<DeclarationDescriptor>,
            nameFilter: (Name) -> Boolean
        ) {
            result.addAll(classDescriptor.enumEntries?.all().orEmpty())
        }


        override fun recordLookup(name: Name, location: LookupLocation) {
            c.components.lookupTracker.record(location, classDescriptor, name)
        }
    }

    private inner class EnumEntryClassDescriptors {
        private val enumEntryProtos = classProto.enumEntryList.groupBy { c.nameResolver.getName(it.name) }

        private val enumEntryByName =
            c.storageManager.createMemoizedFunctionWithNullableValues<Name, Collection<ClassDescriptor>> { name ->

                enumEntryProtos[name]?.let { protoList ->

                    protoList.map {
                        EnumEntrySyntheticClassDescriptor.create(
                            it.typeList.map { ptype->
                                c.typeDeserializer.type(ptype)
                            },

                            c.storageManager, this@DeserializedClassDescriptor, name, enumMemberNames,
                            DeserializedAnnotations(c.storageManager) {
                                c.components.annotationAndConstantLoader.loadEnumEntryAnnotations(
                                    thisAsProtoContainer,
                                    it
                                ).toList()
                            },
                            SourceElement.NO_SOURCE
                        )
                    }


                }
            }

        private val enumMemberNames = c.storageManager.createLazyValue { computeEnumMemberNames() }
        fun findEnumEntrys(name: Name): Collection<ClassDescriptor> = enumEntryByName(name) ?: emptyList()

        fun findEnumEntry(name: Name): ClassDescriptor? = findEnumEntrys(name).firstOrNull()

        private fun computeEnumMemberNames(): Set<Name> {
            // NOTE: order of enum entry members should be irrelevant
            // because enum entries are effectively invisible to user (as classes)
            val result = HashSet<Name>()

            for (supertype in getTypeConstructor().supertypes) {
                for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                    if (descriptor is SimpleFunctionDescriptor || descriptor is PropertyDescriptor) {
                        result.add(descriptor.name)
                    }
                }
            }

            return classProto.functionList.mapTo(result) { c.nameResolver.getName(it.name) } +
                    classProto.variableList.mapTo(result) { c.nameResolver.getName(it.name) } +
                    classProto.propertyList.mapTo(result) { c.nameResolver.getName(it.name) }
        }

        fun all(): Collection<ClassDescriptor> =
            enumEntryProtos.keys.flatMap { name ->  findEnumEntrys(name)  }
    }
}
