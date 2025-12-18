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

package org.cangnova.cangjie.serialization.deserialization.builtins

import org.cangnova.cangjie.builtins.BuiltInsLoader
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.BASIC_PACKAGE_FQ_NAME
import org.cangnova.cangjie.builtins.basic.BasicTypesPackageFragmentProvider
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.NotFoundClasses
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.descriptors.PackageFragmentProviderImpl
import org.cangnova.cangjie.descriptors.impl.CompositePackageFragmentProvider
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.serialization.deserialization.*
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream



class BuiltInsLoaderImpl : BuiltInsLoader {
    private val resourceLoader = BuiltInsResourceLoader()



    override fun createBasicTypesPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor
    ): PackageFragmentProvider {
        return createBasicPackageFragmentDescriptor(storageManager, builtInsModule)
    }

    override fun createStdLibPackageFragmentProvider(
        storageManager: StorageManager,
        stdlibModule: ModuleDescriptor,
        sdk: CjSdk?
    ): PackageFragmentProvider {
        // 从 SDK 加载标准库的 .cjo 文件
        val stdPackages = StandardNames.ALL_NAMES.filter { it != BASIC_PACKAGE_FQ_NAME }.toSet()

        return createBuiltInPackageFragmentProvider(
            storageManager,
            stdlibModule,
            stdPackages,
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


    private fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
        loadResource: (String) -> InputStream?,
        sdk: CjSdk?
    ): PackageFragmentProvider {

        val packageFragments = packageFqNames
            .filter { it != BASIC_PACKAGE_FQ_NAME }
            .mapNotNull { fqName ->


                // 如果有 project，使用新的服务；否则返回 null（跳过此包）
                val resourcePath =
                    BuiltInSerializerFlatbuffers.getBuiltInsFilePath(fqName, sdk)


                if (resourcePath == null) {
                    null
                } else {
                    try {
                        val inputStream = loadResource(resourcePath)
                            ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
                        BuiltInsPackageFragmentImpl.create(fqName, storageManager, module, inputStream,  )
                    } catch (_: FileNotFoundException) {
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
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            emptyList(),
            notFoundClasses,
        )

        for (packageFragment in packageFragments) {
            packageFragment.initialize(components)
        }

        return provider
    }

}