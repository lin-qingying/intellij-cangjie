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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.MutablePackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.impl.ProjectDescriptorImpl
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.serialization.deserialization.DeserializationComponents
import org.cangnova.cangjie.serialization.deserialization.DeserializationConfiguration
import org.cangnova.cangjie.serialization.deserialization.FlatBuffersBasedClassDataFinder
import org.cangnova.cangjie.serialization.deserialization.LocalClassifierTypeSettings
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.telemetry.error.ErrorTelemetry


class CangJieMetadataDeserializerForDecompiler(
    project: Project,
    packageFqName: FqName,
    private val `package`: PackageWrapper,

    private val metadataVersion: BinaryVersion,
    serializerProtocol: SerializerExtensionFlatbuffers,

    ) : DeserializerForDecompilerBase(packageFqName, project) {
    val cangjieProject = ProjectDescriptorImpl(
        Name.identifier("BuiltIns"), project, LockBasedStorageManager(  "BuiltIns"),
    )
    override val builtIns: CangJieBuiltIns
        get() = cangjieProject.builtIns

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
            FlatBuffersBasedClassDataFinder(
                `package`,

                metadataVersion,

                ),

//            AnnotationAndConstantLoaderImpl(moduleDescriptor, notFoundClasses, serializerProtocol),
            packageFragmentProvider,
            ResolveEverythingToCangJieAnyLocalClassifierResolver(builtIns),
            LoggingErrorReporter(LOG),
            LookupTracker.DO_NOTHING,
//            flexibleTypeDeserializer,
            emptyList(),
            notFoundClasses,

//            extensionRegistryLite = serializerProtocol.extensionRegistry,
//            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList()),
//            enumEntriesDeserializationSupport = enumEntriesDeserializationSupport,
        )
    }

    override fun resolveAllDeclarationsInPackage(packageFqName: FqName): List<DeclarationDescriptor> {
        try {
            assert(packageFqName == directoryPackageFqName) {
                "Was called for $packageFqName; only members of $directoryPackageFqName package are expected."
            }
            val dummyPackageFragment = createDummyPackageFragment(packageFqName)
            val membersScope = DeserializedPackageMemberScope(
                dummyPackageFragment, `package`, metadataVersion, containerSource = null,
                components = deserializationComponents,
                debugName = "scope of dummyPackageFragment ${dummyPackageFragment.fqName} in module " +
                        "${deserializationComponents.moduleDescriptor} @CangJieMetadataDeserializerForDecompiler"
            ) { emptyList() }

            return membersScope.getContributedDescriptors().toList()
        } catch (e: Exception) {
            ErrorTelemetry.sendException(
                e,
                "CangJieMetadataDeserializerForDecompiler.resolveAllDeclarationsInPackage",
                mapOf(
                    "package_name" to packageFqName.toString(),
                    "metadata_version" to metadataVersion.toString()
                )
            )
            LOG.error("Failed to resolveName declarations in package $packageFqName", e)
            return emptyList()
        }
    }

    companion object {
        private val LOG = Logger.getInstance(CangJieMetadataDeserializerForDecompiler::class.java)
    }
}

abstract class DeserializerForDecompilerBase(val directoryPackageFqName: FqName, val project: Project) :
    ResolverForDecompiler {
    protected abstract val deserializationComponents: DeserializationComponents

    protected abstract val builtIns: CangJieBuiltIns

    protected val storageManager: StorageManager = LockBasedStorageManager.NO_LOCKS
    protected val cangjieProjectDescriptor: ProjectDescriptorImpl =
        createDummyCangJieProject("project for building decompiled sources")

    protected val moduleDescriptor: ModuleDescriptorImpl = createDummyModule("module for building decompiled sources")
    override fun resolveTopLevelClass(classId: ClassId) = deserializationComponents.deserializeClass(classId)
    override fun resolveTopLevelEnum(classId: ClassId): EnumDescriptor? =
        deserializationComponents.deserializeEnum(classId)

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

    protected fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor =
        MutablePackageFragmentDescriptor(moduleDescriptor, fqName)

    private fun createDummyCangJieProject(name: String) = ProjectDescriptorImpl(
        Name.special("<$name>"), project, storageManager
    )


    private fun createDummyModule(name: String) =
        ModuleDescriptorImpl(cangjieProjectDescriptor, Name.special("<$name>"), "<$name>", storageManager)

    init {
        moduleDescriptor.initialize(packageFragmentProvider)
        moduleDescriptor.setDependencies(moduleDescriptor, moduleDescriptor.builtIns.builtInsModule)
    }
}


class ResolveEverythingToCangJieAnyLocalClassifierResolver(private val builtIns: CangJieBuiltIns) :
    LocalClassifierTypeSettings {
    override val replacementTypeForLocalClassifiers: SimpleType
        get() = builtIns.stdlibTypes.anyType
}
