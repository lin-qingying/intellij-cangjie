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

package cn.cangnova.cangjie.metadata.decompiler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.cjpm.toolchain.impl.CjpmMetadata
import cn.cangnova.cangjie.contracts.ContractDeserializer
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.MutablePackageFragmentDescriptor
import cn.cangnova.cangjie.incremental.components.LookupTracker
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.deserialization.BinaryVersion
import cn.cangnova.cangjie.metadata.deserialization.NameResolver
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.sam.SamConversionResolverImpl
import cn.cangnova.cangjie.serialization.SerializerExtensionProtocol
import cn.cangnova.cangjie.serialization.deserialization.*
import cn.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import cn.cangnova.cangjie.storage.LockBasedStorageManager
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.DefaultBuiltIns
import cn.cangnova.cangjie.types.SimpleType


class CangJieMetadataDeserializerForDecompiler(
      project: Project,
    packageFqName: FqName,
    private val proto: ProtoBuf.PackageFragment,
    private val nameResolver: NameResolver,
    private val metadataVersion: BinaryVersion,
    serializerProtocol: SerializerExtensionProtocol,
    flexibleTypeDeserializer: FlexibleTypeDeserializer
) : DeserializerForDecompilerBase(packageFqName,project) {
    override val builtIns: CangJieBuiltIns get() = CangJieBuiltIns(project,LockBasedStorageManager(project,"BuiltIns"))

    override val deserializationComponents: DeserializationComponents

    private object Configuration : DeserializationConfiguration by DeserializationConfiguration.Default {
        override val preserveDeclarationsOrdering: Boolean
            get() = true
    }

    init {
        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        deserializationComponents = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            Configuration,
            ProtoBasedClassDataFinder(
                proto,
                nameResolver,
                metadataVersion
            ),
            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, serializerProtocol),
            packageFragmentProvider,
            ResolveEverythingToCangJieAnyLocalClassifierResolver(builtIns),
            LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING,
            flexibleTypeDeserializer,
            emptyList(),
            notFoundClasses,
            ContractDeserializer.DEFAULT,
            extensionRegistryLite = serializerProtocol.extensionRegistry,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList()),
            enumEntriesDeserializationSupport = enumEntriesDeserializationSupport,
        )
    }

    override fun resolveDeclarationsInFacade(facadeFqName: FqName): List<DeclarationDescriptor> {
        assert(facadeFqName == directoryPackageFqName) {
            "Was called for $facadeFqName; only members of $directoryPackageFqName package are expected."
        }

        val dummyPackageFragment = createDummyPackageFragment(facadeFqName)
        val membersScope = DeserializedPackageMemberScope(
            dummyPackageFragment, proto.`package`, nameResolver, metadataVersion, containerSource = null,
            components = deserializationComponents,
            debugName = "scope of dummyPackageFragment ${dummyPackageFragment.fqName} in module " +
                    "${deserializationComponents.moduleDescriptor} @CangJieMetadataDeserializerForDecompiler"
        ) { emptyList() }

        return membersScope.getContributedDescriptors().toList()
    }

    companion object {
        private val LOG = Logger.getInstance(CangJieMetadataDeserializerForDecompiler::class.java)
    }
}

abstract class DeserializerForDecompilerBase(val directoryPackageFqName: FqName,val project:Project) : ResolverForDecompiler {
    protected abstract val deserializationComponents: DeserializationComponents

    protected abstract val builtIns: CangJieBuiltIns

    protected val storageManager: StorageManager = LockBasedStorageManager.NO_LOCKS

    protected val moduleDescriptor: ModuleDescriptorImpl = createDummyModule("module for building decompiled sources")

    protected val packageFragmentProvider: PackageFragmentProvider = object : PackageFragmentProviderOptimized {
        override fun collectPackageFragments(
            fqName: FqName,
            packageFragments: MutableCollection<PackageFragmentDescriptor>
        ) {
            packageFragments.add(createDummyPackageFragment(fqName))
        }

        override fun isEmpty(fqName: FqName): Boolean = false

        @Suppress("OVERRIDE_DEPRECATION")
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            return listOf(createDummyPackageFragment(fqName))
        }

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
            throw UnsupportedOperationException("This method is not supposed to be called.")
        }
    }

    protected val enumEntriesDeserializationSupport: EnumEntriesDeserializationSupport =
        EnumEntriesDeserializationSupport.Default

    override fun resolveTopLevelClass(classId: ClassId) = deserializationComponents.deserializeClass(classId)

    protected fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor =
        MutablePackageFragmentDescriptor(moduleDescriptor, fqName)

    private fun createDummyModule(name: String) =
        ModuleDescriptorImpl(Name.special("<$name>"), storageManager, builtIns)

    init {
        moduleDescriptor.initialize(packageFragmentProvider)
        moduleDescriptor.setDependencies(moduleDescriptor, moduleDescriptor.builtIns.builtInsModule)
    }
}


class ResolveEverythingToCangJieAnyLocalClassifierResolver(private val builtIns: CangJieBuiltIns) :
    LocalClassifierTypeSettings {
    override val replacementTypeForLocalClassifiers: SimpleType
        get() = builtIns.anyType
}
