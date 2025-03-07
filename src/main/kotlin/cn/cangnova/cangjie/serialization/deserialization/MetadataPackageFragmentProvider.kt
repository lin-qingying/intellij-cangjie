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

package cn.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.contracts.ContractDeserializer
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.NotFoundClasses
import cn.cangnova.cangjie.incremental.components.LookupTracker
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import cn.cangnova.cangjie.metadata.decompiler.CangJieClassFinder
import cn.cangnova.cangjie.metadata.decompiler.CangJieMetadataFinder
import cn.cangnova.cangjie.metadata.deserialization.NameResolverImpl
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.sam.SamConversionResolver
import cn.cangnova.cangjie.resolve.scopes.ChainedMemberScope
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.serialization.deserialization.builtins.BuiltInsPackageFragmentImpl
import cn.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.checker.NewCangJieTypeChecker
import cn.cangnova.cangjie.types.expressions.TypeAttributeTranslators

class MetadataPackageFragmentProvider(
    storageManager: StorageManager,
    finder: CangJieMetadataFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    private val metadataPartProvider: MetadataPartProvider,
    contractDeserializer: ContractDeserializer,
    cangjieTypeChecker: NewCangJieTypeChecker,
    samConversionResolver: SamConversionResolver,
    typeAttributeTranslators: TypeAttributeTranslators
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            DeserializationConfiguration.Default, // TODO
            DeserializedClassDataFinder(this),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
            this,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            emptyList(),
            notFoundClasses,
            contractDeserializer,
            AdditionalClassPartsProvider.None, PlatformDependentDeclarationFilter.All,
            BuiltInSerializerProtocol.extensionRegistry,
            cangjieTypeChecker,
            samConversionResolver,
            typeAttributeTranslators = typeAttributeTranslators.translators
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
        if (finder.hasMetadataPackage(fqName))
            MetadataPackageFragment(fqName, storageManager, moduleDescriptor, metadataPartProvider, finder)
        else null
}

class MetadataPackageFragment(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    private val metadataPartProvider: MetadataPartProvider,
    private val finder: CangJieMetadataFinder
) : DeserializedPackageFragment(fqName, storageManager, module) {
    override val classDataFinder = MetadataClassDataFinder(finder)

    private lateinit var components: DeserializationComponents

    override fun initialize(components: DeserializationComponents) {
        this.components = components
    }

    private val memberScope = storageManager.createLazyValue { computeMemberScope() }

    private fun computeMemberScope(): MemberScope {
        // For each .cangjie_metadata file which represents a package part, add a separate deserialized scope
        // with top level callables and type aliases (but no classes) only from that part
        val packageParts = metadataPartProvider.findMetadataPackageParts(fqName.asString())
        val scopes = arrayListOf<DeserializedPackageMemberScope>()
        for (partName in packageParts) {
            val stream = finder.findMetadata(ClassId(fqName, Name.identifier(partName))) ?: continue
            val (proto, nameResolver, version) = readProto(stream)

            scopes.add(
                DeserializedPackageMemberScope(
                    this,
                    proto.`package`,
                    nameResolver,
                    version,
                    containerSource = null,
                    components = components,
                    debugName = "scope with top-level callables and type aliases (no classes) for package part $partName of $this",
                    classNames = { emptyList() },
                )
            )
        }

        // Also add the deserialized scope that can load all classes from this package
        scopes.add(object : DeserializedPackageMemberScope(
            this, ProtoBuf.Package.getDefaultInstance(),
            NameResolverImpl(
                ProtoBuf.StringTable.getDefaultInstance(),
                ProtoBuf.QualifiedNameTable.getDefaultInstance()
            ),
            BuiltInsBinaryVersion.INSTANCE, // Exact version does not matter here
            containerSource = null, components = components,
            debugName = "scope for all classes of $this",
            classNames = { emptyList() },
        ) {
            override fun hasClass(name: Name): Boolean = hasTopLevelClass(name)
            override fun definitelyDoesNotContainName(name: Name) = false
            override fun getClassifierNames(): Set<Name>? = null
            override fun getNonDeclaredClassifierNames(): Set<Name>? = null
        })

        return ChainedMemberScope.create(".cangjie_metadata parts scope of $this", scopes)
    }

    override fun getMemberScope() = memberScope()

    override fun hasTopLevelClass(name: Name): Boolean {
        // TODO: check if the corresponding file exists
        return true
    }

    companion object {
        @Deprecated(
            "The constant has been moved",
            ReplaceWith("METADATA_FILE_EXTENSION", "cn.cangnova.cangjie.serialization.deserialization"),
        )
        const val METADATA_FILE_EXTENSION =
            cn.cangnova.cangjie.serialization.deserialization.METADATA_FILE_EXTENSION

        @Deprecated(
            "The constant has been moved",
            ReplaceWith("DOT_METADATA_FILE_EXTENSION", "cn.cangnova.cangjie.serialization.deserialization")
        )
        const val DOT_METADATA_FILE_EXTENSION =
            cn.cangnova.cangjie.serialization.deserialization.DOT_METADATA_FILE_EXTENSION
    }
}

class CangJieBuiltInsPackageFragmentProvider(
    storageManager: StorageManager,
    finder: CangJieClassFinder,
    moduleDescriptor: ModuleDescriptor,
    notFoundClasses: NotFoundClasses,
    additionalClassPartsProvider: AdditionalClassPartsProvider =AdditionalClassPartsProvider.None ,
    platformDependentDeclarationFilter: PlatformDependentDeclarationFilter = PlatformDependentDeclarationFilter.All,
    deserializationConfiguration: DeserializationConfiguration,
    cangjieTypeChecker: NewCangJieTypeChecker,
    samConversionResolver: SamConversionResolver
) : AbstractDeserializedPackageFragmentProvider(storageManager, finder, moduleDescriptor) {
    init {
        components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            deserializationConfiguration,
            DeserializedClassDataFinder(this),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, BuiltInSerializerProtocol),
            this,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            listOf(
//                BuiltInFictitiousFunctionClassFactory(storageManager, moduleDescriptor),
//                JvmBuiltInClassDescriptorFactory(storageManager, moduleDescriptor)
            ),
            notFoundClasses,
            ContractDeserializer.DEFAULT,
            additionalClassPartsProvider, platformDependentDeclarationFilter,
            BuiltInSerializerProtocol.extensionRegistry,
            cangjieTypeChecker,
            samConversionResolver,
//            enumEntriesDeserializationSupport = JvmEnumEntriesDeserializationSupport,
        )
    }

    override fun findPackage(fqName: FqName): DeserializedPackageFragment? =
        finder.findBuiltInsData(fqName)?.let { inputStream ->
            BuiltInsPackageFragmentImpl.create(
                fqName,
                storageManager,
                moduleDescriptor,
                inputStream,
                isFallback = false
            )
        }

    companion object {
        const val DOT_BUILTINS_METADATA_FILE_EXTENSION = ".cangjie_builtins"
    }
}
