package org.cangnova.cangjie.serialization.deserialization.builtins

import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.builtins.BuiltInsLoader
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.NotFoundClasses
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.descriptors.PackageFragmentProviderImpl
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.cangnova.cangjie.serialization.deserialization.DeserializationComponents
import org.cangnova.cangjie.serialization.deserialization.DeserializationConfiguration
import org.cangnova.cangjie.serialization.deserialization.DeserializedClassDataFinder
import org.cangnova.cangjie.serialization.deserialization.ErrorReporter
import org.cangnova.cangjie.serialization.deserialization.FlatBuffersBasedClassDataFinder
import org.cangnova.cangjie.serialization.deserialization.LocalClassifierTypeSettings
import org.cangnova.cangjie.storage.StorageManager
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

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

//        使用文件系统读取
        return getInputStreamFromFile(path)
    }
}

class BuiltInsLoaderImpl : BuiltInsLoader {
    private val resourceLoader = BuiltInsResourceLoader()

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        isFallback: Boolean
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

            )
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

        ): PackageFragmentProvider {
        val packageFragments = packageFqNames.mapNotNull { fqName ->

            if (fqName == BUILT_INS_PACKAGE_FQ_NAME) {
//                craetePackageFragmentDescriptor(storageManager, module, fqName)
                null
            } else {

                val resourcePath = BuiltInSerializerFlatbuffers.getBuiltInsFilePath(fqName)

                try {
//                    val inputStream = getURL(project, resourcePath).openStream()

                    val inputStream = loadResource(resourcePath)
                        ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
                    BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream, isFallback)

                } catch (e: FileNotFoundException) {
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
//            AnnotationAndConstantLoaderImpl(module, notFoundClasses, BuiltInSerializerProtocol),
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
//            FlexibleTypeDeserializer.ThrowException,
            emptyList(),     /*  classDescriptorFactories,*/
            notFoundClasses,
//            ContractDeserializer.DEFAULT,
//            additionalClassPartsProvider,
//            platformDependentDeclarationFilter,
        )

        for (packageFragment in packageFragments) {
            if (packageFragment is DeserializedPackageFragment) {
                packageFragment.initialize(components)

            }
        }

        return provider
    }

}