package com.huawei.cangjie.serialization.deserialization

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.NotFoundClasses
import com.huawei.cangjie.descriptors.PackageFragmentProvider
import com.huawei.cangjie.incremental.components.LookupTracker
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.DefaultTypeAttributeTranslator
import com.huawei.cangjie.types.TypeAttributeTranslator


class DeserializationComponents(
    val storageManager: StorageManager,
    val moduleDescriptor: ModuleDescriptor,
    val configuration: DeserializationConfiguration,
//    val classDataFinder: ClassDataFinder,
//    val annotationAndConstantLoader: AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>>,
    val packageFragmentProvider: PackageFragmentProvider,
//    val localClassifierTypeSettings: LocalClassifierTypeSettings,
//    val errorReporter: ErrorReporter,
    val lookupTracker: LookupTracker,
//    val flexibleTypeDeserializer: FlexibleTypeDeserializer,
//    val fictitiousClassDescriptorFactories: Iterable<ClassDescriptorFactory>,
    val notFoundClasses: NotFoundClasses,
//    val contractDeserializer: ContractDeserializer,
//    val additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
//    val platformDependentDeclarationFilter: PlatformDependentDeclarationFilter = PlatformDependentDeclarationFilter.All,
//    val extensionRegistryLite: ExtensionRegistryLite,
//    val kotlinTypeChecker: NewCangJieTypeChecker = NewCangJieTypeChecker.Default,
//    val samConversionResolver: SamConversionResolver,
    val typeAttributeTranslators: List<TypeAttributeTranslator> = listOf(DefaultTypeAttributeTranslator),
//    val enumEntriesDeserializationSupport: EnumEntriesDeserializationSupport = EnumEntriesDeserializationSupport.Default,
)
