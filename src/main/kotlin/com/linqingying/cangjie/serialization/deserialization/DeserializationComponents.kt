package com.linqingying.cangjie.serialization.deserialization

import com.google.protobuf.ExtensionRegistryLite
import com.linqingying.cangjie.contracts.ContractDeserializer
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.incremental.components.LookupTracker
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.metadata.deserialization.VersionRequirementTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.resolve.sam.SamConversionResolver
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.DefaultTypeAttributeTranslator
import com.linqingying.cangjie.types.SimpleType
import com.linqingying.cangjie.types.TypeAttributeTranslator
import com.linqingying.cangjie.types.checker.NewCangJieTypeChecker

interface LocalClassifierTypeSettings {
    val replacementTypeForLocalClassifiers: SimpleType?

    object Default : LocalClassifierTypeSettings {
        override val replacementTypeForLocalClassifiers: SimpleType?
            get() = null
    }
}

class DeserializationComponents(
    val storageManager: StorageManager,
    val moduleDescriptor: ModuleDescriptor,
    val configuration: DeserializationConfiguration,
    val classDataFinder: ClassDataFinder,
    val annotationAndConstantLoader: AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>>,
    val packageFragmentProvider: PackageFragmentProvider,
    val localClassifierTypeSettings: LocalClassifierTypeSettings,
    val errorReporter: ErrorReporter,
    val lookupTracker: LookupTracker,
    val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    val fictitiousClassDescriptorFactories: Iterable<ClassDescriptorFactory>,
    val notFoundClasses: NotFoundClasses,
    val contractDeserializer: ContractDeserializer,
    val additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
    val platformDependentDeclarationFilter: PlatformDependentDeclarationFilter = PlatformDependentDeclarationFilter.All,
    val extensionRegistryLite: ExtensionRegistryLite,
    val cangjieTypeChecker: NewCangJieTypeChecker = NewCangJieTypeChecker.Default,
    val samConversionResolver: SamConversionResolver,
    val typeAttributeTranslators: List<TypeAttributeTranslator> = listOf(DefaultTypeAttributeTranslator),
    val enumEntriesDeserializationSupport: EnumEntriesDeserializationSupport = EnumEntriesDeserializationSupport.Default,
) {
    val classDeserializer: ClassDeserializer = ClassDeserializer(this)

    fun deserializeClass(classId: ClassId): ClassDescriptor? = classDeserializer.deserializeClass(classId)

    fun createContext(
        descriptor: PackageFragmentDescriptor,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        versionRequirementTable: VersionRequirementTable,
        metadataVersion: BinaryVersion,
        containerSource: DeserializedContainerSource?
    ): DeserializationContext =
        DeserializationContext(
            this, nameResolver, descriptor, typeTable, versionRequirementTable, metadataVersion, containerSource,
            parentTypeDeserializer = null, typeParameters = listOf()
        )
}


class DeserializationContext(
    val components: DeserializationComponents,
    val nameResolver: NameResolver,
    val containingDeclaration: DeclarationDescriptor,
    val typeTable: TypeTable,
    val versionRequirementTable: VersionRequirementTable,
    val metadataVersion: BinaryVersion,
    val containerSource: DeserializedContainerSource?,
    parentTypeDeserializer: TypeDeserializer?,
    typeParameters: List<ProtoBuf.TypeParameter>
) {
    val typeDeserializer: TypeDeserializer = TypeDeserializer(
        this, parentTypeDeserializer, typeParameters,
        "Deserializer for \"${containingDeclaration.name}\"",
        containerSource?.presentableString ?: "[container not found]"
    )

    val memberDeserializer: MemberDeserializer = MemberDeserializer(this)

    val storageManager: StorageManager get() = components.storageManager

    fun childContext(
        descriptor: DeclarationDescriptor,
        typeParameterProtos: List<ProtoBuf.TypeParameter>,
        nameResolver: NameResolver = this.nameResolver,
        typeTable: TypeTable = this.typeTable,
        versionRequirementTable: VersionRequirementTable = this.versionRequirementTable,
        metadataVersion: BinaryVersion = this.metadataVersion
    ): DeserializationContext = DeserializationContext(
        components, nameResolver, descriptor, typeTable,
        if (isVersionRequirementTableWrittenCorrectly(metadataVersion)) versionRequirementTable else this.versionRequirementTable,
        metadataVersion, this.containerSource,
        parentTypeDeserializer = this.typeDeserializer, typeParameters = typeParameterProtos
    )
}
// The purpose of utilities in this file is to support different behavior in deserialization according to the given binary file's version.
//
// For example, if we find a bug in serialization/deserialization and would like to fix it _remaining compatible_ with two latest versions
// of CangJie, we can use methods of this class to fix deserialization of the "future" binaries, and later (in the next major version)
// fix the bug in serialization when the binary version advances to the value supported in the first bug fix.

/**
 * Before metadata version 1.4, version requirements for nested classes were deserialized incorrectly: the version requirement table was
 * loaded from the outermost class and passed to the nested classes and their members, even though indices of their version requirements
 * were pointing to the other table stored in the nested class (which was not read by deserialization).
 */
fun isVersionRequirementTableWrittenCorrectly(version: BinaryVersion): Boolean =
    isCangJie1Dot4OrLater(version)

fun isCangJie1Dot4OrLater(version: BinaryVersion): Boolean =
    (version.major == 1 && version.minor >= 4) || version.major > 1
