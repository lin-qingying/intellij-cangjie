package org.cangnova.cangjie.serialization.deserialization.builtins

import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import org.cangnova.cangjie.builtins.BuiltInsLoader
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.BASIC_PACKAGE_FQ_NAME
import org.cangnova.cangjie.builtins.basic.BasicTypesPackageFragmentProvider
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
import org.cangnova.cangjie.serialization.deserialization.LocalClassifierTypeSettings
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.toolchain.api.CjSdk
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
        // 使用文件系统读取
        return getInputStreamFromFile(path)
    }
}

class BuiltInsLoaderImpl : BuiltInsLoader {
    private val resourceLoader = BuiltInsResourceLoader()

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        isFallback: Boolean,
        sdk: CjSdk?
    ): PackageFragmentProvider {
        return createBuiltInPackageFragmentProvider(
            storageManager,
            builtInsModule,
            StandardNames.ALL_NAMES,
            isFallback,
            resourceLoader::loadResource,
            sdk
        )
    }

    private fun createBasicPackageFragmentDescriptor(
        storageManager: StorageManager,
        module: ModuleDescriptor
    ): BasicTypesPackageFragmentProvider {
        return BasicTypesPackageFragmentProvider(
            storageManager, module,
        )
    }


    fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
        isFallback: Boolean,
        loadResource: (String) -> InputStream?, sdk: CjSdk?
    ): PackageFragmentProvider {

        val packageFragments = packageFqNames.mapNotNull { fqName ->

            if (fqName == BASIC_PACKAGE_FQ_NAME) {
                createBasicPackageFragmentDescriptor(storageManager, module)
                null
            } else {
                // 如果有 project，使用新的服务；否则返回 null（跳过此包）
                val resourcePath =
                    BuiltInSerializerFlatbuffers.getBuiltInsFilePath(fqName, sdk)


                if (resourcePath == null) {
                    null
                } else {
                    try {
                        val inputStream = loadResource(resourcePath)
                            ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
                        BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream, isFallback)
                    } catch (e: FileNotFoundException) {
                        null
                    }
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
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            emptyList(),
            notFoundClasses,
        )

        for (packageFragment in packageFragments) {
            if (packageFragment is DeserializedPackageFragment) {
                packageFragment.initialize(components)
            }
        }

        return provider
    }

}