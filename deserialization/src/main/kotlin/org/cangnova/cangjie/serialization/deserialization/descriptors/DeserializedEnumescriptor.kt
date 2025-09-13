package org.cangnova.cangjie.serialization.deserialization.descriptors


import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.ClassConstructorDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithKind
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.ConstructorDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.EnumKind
import org.cangnova.cangjie.descriptors.EnumMember
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.NotFoundClasses
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.ScopesHolderForClass
import org.cangnova.cangjie.descriptors.SimpleFunctionDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.computeConstructorTypeParameters
import org.cangnova.cangjie.descriptors.impl.AbstractClassDescriptor
import org.cangnova.cangjie.descriptors.impl.AbstractEnumDescriptor
import org.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.EnumConstructorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.ConstructorWrapper
import org.cangnova.cangjie.metadata.model.wrapper.EnumEntryWrapper
import org.cangnova.cangjie.metadata.model.wrapper.EnumWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DeserializedDeclarationsFromSupertypeConflictDataKey
import org.cangnova.cangjie.resolve.NonReportingOverrideStrategy
import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.resolve.classId
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.serialization.deserialization.DeserializationContext
import org.cangnova.cangjie.types.AbstractClassTypeConstructor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeRefinement
import org.cangnova.cangjie.types.TypeSubstitutor
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.utils.flatMapToNullable
import java.util.LinkedHashSet
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import kotlin.collections.toList

class DeserializedEnumescriptor(
    outerContext: DeserializationContext,
    val `enum`: EnumWrapper,

    val metadataVersion: BinaryVersion,
    override val source: SourceElement
) : AbstractEnumDescriptor(
    outerContext.storageManager,
    `enum`.name
) {

    private val classId = `enum`.classId

    private val funcList = `enum`.functions
    private val varList = `enum`.variables
    private val propList = `enum`.propertys
    override val hasArguments: Boolean = enum.hasArguments
    override val isNonExhaustive: Boolean = enum.isNonExhaustive
    override val containingDeclaration = outerContext.containingDeclaration

    override val modality: Modality = enum.modality
    override val visibility: DescriptorVisibility = enum.visibility
    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = c.typeDeserializer.ownTypeParameters

    override fun toString(): String {
        return "deserialized " + super.toString()
    }

    val c = outerContext.childContext(
        this, `enum`.typeParameters, outerContext.`package`,
        metadataVersion
    )
    private val constructorsValue = c.storageManager.createLazyValue { computeConstructors() }
    fun computeConstructors() = enum.entrys.map { c.declDeserializer.loadEnumConstructor(it) }

    //    枚举项
    override val constructors: Collection<EnumConstructorDescriptor>
        get() = constructorsValue.invoke()


    private val memberScopeHolder =
        ScopesHolderForClass.create(
            this,
            c.storageManager,
            c.components.cangjieTypeChecker.cangjieTypeRefiner,
            this::DeserializedEnumMemberScope
        )

    private inner class DeserializedEnumMemberScope(private val cangjieTypeRefiner: CangJieTypeRefiner) :
        DeserializedMemberScope(c, funcList, varList, propList, emptyList(), emptyList()) {

        private val enumDescriptor: DeserializedEnumescriptor get() = this@DeserializedEnumescriptor

        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(
                DescriptorKindFilter.ALL,
                MemberScope.ALL_NAME_FILTER,
                NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
            )
        }
        private val refinedSupertypes = c.storageManager.createLazyValue {
            @OptIn(TypeRefinement::class)
            cangjieTypeRefiner.refineSupertypes(enumDescriptor)
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
                this@DeserializedEnumescriptor,
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
                    this@DeserializedEnumescriptor
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

        override fun createClassId(name: Name) = classId.createNestedClassId(name)


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
                enumDescriptor,
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

            return enumDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.functionNames
            }
                .apply { addAll(c.components.additionalClassPartsProvider.getFunctionsNames(this@DeserializedEnumescriptor)) }
        }

        override fun getNonDeclaredVariableNames(): Set<Name> {
            return enumDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
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
            return enumDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.propertyNames
            }

        }

        override fun recordLookup(name: Name, location: LookupLocation) {
            c.components.lookupTracker.record(location, enumDescriptor, name)
        }

        override fun getNonDeclaredClassifierNames(): Set<Name>? {
            return enumDescriptor.typeConstructor.supertypes.flatMapToNullable(LinkedHashSet()) {
                it.memberScope.classifierNames
            }
        }

    }

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        memberScopeHolder.getScope(cangjieTypeRefiner)

    private val memberScope get() = memberScopeHolder.getScope(c.components.cangjieTypeChecker.cangjieTypeRefiner)

    override val typeConstructor: TypeConstructor = DeserializedEnumTypeConstructor()

    private inner class DeserializedEnumTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val _parameters = c.storageManager.createLazyValue {
            this@DeserializedEnumescriptor.computeConstructorTypeParameters()
        }

        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
            return emptyList()
        }

        override fun computeSupertypes(): Collection<CangJieType> {

            val result = `enum`.superTypes.toSet().map { supertype ->
                c.typeDeserializer.type(supertype)
            } + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedEnumescriptor)

            val unresolved = result.mapNotNull { supertype ->
                supertype.constructor.declarationDescriptor as? NotFoundClasses.MockClassDescriptor
            }

            if (unresolved.isNotEmpty()) {
                c.components.errorReporter.reportIncompleteHierarchy(
                    this@DeserializedEnumescriptor,
                    unresolved.map { it.classId?.asSingleFqName()?.asString() ?: it.name.asString() }
                )
            }

            return result.toList()
        }


        override val parameters: List<TypeParameterDescriptor>
            get() = _parameters()
        override val isDenotable: Boolean
            get() = true
        override val declarationDescriptor: ClassifierDescriptorWithKind = this@DeserializedEnumescriptor
        override fun toString() = name.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            // TODO: inject implementation
            get() = SupertypeLoopChecker.EMPTY
    }

}


class DeserializedEnumConstructorDescriptor(
    containingDeclaration: EnumDescriptor,
    original: EnumConstructorDescriptor?,
    annotations: Annotations,


    override val decl: EnumEntryWrapper,

    override val containerSource: DeserializedContainerSource?,
    source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
    EnumConstructorDescriptorImpl(
        decl.name,
        containingDeclaration,
        original,
        annotations,
        source ?: SourceElement.NO_SOURCE
    ) {


    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: EnumConstructorDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name,
        annotations: Annotations,
        source: SourceElement
    ): DeserializedEnumConstructorDescriptor {
        return DeserializedEnumConstructorDescriptor(
            newOwner as EnumDescriptor, original, annotations,
            decl, containerSource, source
        )
    }
}
