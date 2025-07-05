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

package cn.cangnova.cangjie.builtins


//import cn.cangnova.cangjie.metadata.ProtoBuf
//import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
//import cn.cangnova.cangjie.metadata.builtins.BuiltInsProtoBuf

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import cn.cangnova.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME

import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.NotFoundClasses
import cn.cangnova.cangjie.descriptors.PackageFragmentProvider
import cn.cangnova.cangjie.descriptors.PackageFragmentProviderImpl
import cn.cangnova.cangjie.ide.run.cjpm.toolchain
import cn.cangnova.cangjie.incremental.components.LookupTracker
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.resolve.lazy.declarations.impl.AbstractPackageFragmentDescriptorBuiltlnImpl
import cn.cangnova.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorBasicImpl
import cn.cangnova.cangjie.resolve.sam.SamConversionResolverImpl
import cn.cangnova.cangjie.serialization.deserialization.*
import cn.cangnova.cangjie.serialization.deserialization.builtins.BuiltInsPackageFragmentImpl
import cn.cangnova.cangjie.storage.StorageManager
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.util.*


interface BuiltInsLoader {
    fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
//        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
//        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
//        additionalClassPartsProvider: AdditionalClassPartsProvider,
        isFallback: Boolean,
        project: Project? = null,
    ): PackageFragmentProvider

    companion object {
        val Instance: BuiltInsLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val implementations = ServiceLoader.load(BuiltInsLoader::class.java, BuiltInsLoader::class.java.classLoader)
            implementations.firstOrNull() ?: throw IllegalStateException(
                "No BuiltInsLoader implementation was found. Please ensure that the META-INF/services/ is not stripped " +
                        "from your application and that the Java virtual machine is not running under a security manager"
            )
        }
    }
}

//fun InputStream.readBuiltinsPackageFragment(): Pair<ProtoBuf.PackageFragment?, BuiltInsBinaryVersion> =
//    use { stream ->
//        val version = BuiltInsBinaryVersion.readFrom(stream)
//        val proto =
//            if (version.isCompatibleWithCurrentCompilerVersion()) {
//
//                ProtoBuf.PackageFragment.parseFrom(
//                    stream,
////                ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions) //轻量级
//                    ExtensionRegistry.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions)
//                )
//            } else null
//
//
//        val printer: JsonFormat.Printer = JsonFormat.printer()
//        val jsonStr: String = printer.print(proto)
//
//        proto to version
//    }
fun craetePackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,
    fqName: FqName
): AbstractPackageFragmentDescriptorBuiltlnImpl {

    return when (fqName) {


        BUILT_INS_PACKAGE_FQ_NAME -> PackageFragmentDescriptorBasicImpl(storageManager, module, fqName)
        else ->
            TODO("Not yet implemented")

    }
}

class BuiltInsLoaderImpl : BuiltInsLoader {


    private fun getURL(project: Project, resourcePath: String): URL {
//        val toolchainVsrsion = runReadAction {
//            project.toolchain?.cjc()?.version?.semver?.rawVersion ?: ""
//        }
//        val stdlibPathByVersion: Path = CjToolchainBase.stdlibPath.resolve(toolchainVsrsion)

        return CjToolchainBase.stdlibPath.resolve(resourcePath).toUri().toURL()
    }

    fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
//        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
//        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
//        additionalClassPartsProvider: AdditionalClassPartsProvider = AdditionalClassPartsProvider.None,
        isFallback: Boolean,
        loadResource: (String) -> InputStream?,
        project: Project? = null
    ): PackageFragmentProvider {
        val packageFragments = packageFqNames.mapNotNull { fqName ->

            if (fqName == BUILT_INS_PACKAGE_FQ_NAME) {
                craetePackageFragmentDescriptor(storageManager, module, fqName)

            } else {
//                val project =
//                    project ?: ProjectManager.getInstance().openProjects.firstOrNull() ?: return@mapNotNull null


                val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)

                try{
//                    val inputStream = getURL(project, resourcePath).openStream()

                    val inputStream = loadResource("stdlib/$resourcePath")
                        ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
                    BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream, isFallback)

                }catch (e: FileNotFoundException){
                    null
                }


            }

        }


        val provider = PackageFragmentProviderImpl(packageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, module)

        val components = DeserializationComponents(
            storageManager,
            module,
            DeserializationConfiguration.Default,
            DeserializedClassDataFinder(provider),
            AnnotationAndConstantLoaderImpl(module, notFoundClasses, BuiltInSerializerProtocol),
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            FlexibleTypeDeserializer.ThrowException,
            emptyList(),     /*  classDescriptorFactories,*/
            notFoundClasses,
            ContractDeserializer.DEFAULT,
//            additionalClassPartsProvider,
//            platformDependentDeclarationFilter,
            extensionRegistryLite = BuiltInSerializerProtocol.extensionRegistry,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList())
        )

        for (packageFragment in packageFragments) {
            if (packageFragment is DeserializedPackageFragment) {
                packageFragment.initialize(components)

            }
        }

        return provider
    }

    private val resourceLoader = BuiltInsResourceLoader()

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
//        classDescriptorFactories: Iterable<ClassDescriptorFactory>,
//        platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
//        additionalClassPartsProvider: AdditionalClassPartsProvider,
        isFallback: Boolean,
        project: Project?
    ): PackageFragmentProvider {
        return createBuiltInPackageFragmentProvider(
            storageManager,
            builtInsModule,
            StandardNames.ALL_NAMES,
//            classDescriptorFactories,
//            platformDependentDeclarationFilter,
//            additionalClassPartsProvider,
            isFallback,
            resourceLoader::loadResource,
            project
        )
    }
}

class BuiltInsResourceLoader {
    fun getInputStreamFromFile(path: String): InputStream? {
        return try {
            FileInputStream(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadResource(path: String): InputStream? {
        val classLoader = this::class.java.classLoader ?: return ClassLoader.getSystemResourceAsStream(path)

        val resource = classLoader.getResource(path) ?: return null
        return resource.openConnection().apply { useCaches = false }.getInputStream()
    }
}

