package com.linqingying.cangjie.serialization.deserialization.descriptors

import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.metadata.deserialization.VersionRequirementTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.serialization.deserialization.DeserializationComponents
import com.linqingying.cangjie.utils.Printer
import com.linqingying.cangjie.incremental.record

open class DeserializedPackageMemberScope(
    private val packageDescriptor: PackageFragmentDescriptor,
    proto: ProtoBuf.Package,
    nameResolver: NameResolver,
    metadataVersion: BinaryVersion,
    containerSource: DeserializedContainerSource?,
    components: DeserializationComponents,
    private val debugName: String,
    classNames: () -> Collection<Name>,
) : DeserializedMemberScope(
    components.createContext(
        packageDescriptor, nameResolver, TypeTable(proto.typeTable),
        VersionRequirementTable.create(proto.versionRequirementTable), metadataVersion, containerSource
    ),
    proto.functionList, proto.variableList, emptyList(), proto.typeAliasList, classNames
) {
    private val packageFqName = packageDescriptor.fqName

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        computeDescriptors(kindFilter, nameFilter, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS) +
                c.components.fictitiousClassDescriptorFactories.flatMap { it.getAllContributedClassesIfPossible(packageFqName) }

    override fun hasClass(name: Name) =
        super.hasClass(name) || c.components.fictitiousClassDescriptorFactories.any { it.shouldCreateClass(packageFqName, name) }

    override fun createClassId(name: Name) = ClassId(packageFqName, name)


    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)
        return super.getContributedClassifier(name, location)
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.components.lookupTracker.record(location, packageDescriptor, name)
    }

    override fun getNonDeclaredFunctionNames(): Set<Name> = emptySet()
    override fun getNonDeclaredVariableNames(): Set<Name> = emptySet()


    override fun getNonDeclaredPropertyNames(): Set<Name>  = emptySet()

    override fun getNonDeclaredClassifierNames(): Set<Name>? = emptySet()

    override fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
        // Do nothing
    }

    override fun toString(): String = debugName
}
