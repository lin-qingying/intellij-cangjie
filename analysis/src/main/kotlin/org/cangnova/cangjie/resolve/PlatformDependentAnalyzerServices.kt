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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.StorageComponentContainer
import org.cangnova.cangjie.descriptors.DependencyOnBuiltIns
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.ImportPath
import org.cangnova.cangjie.storage.LockBasedStorageManager

interface PlatformConfigurator {
    val platformSpecificContainer: StorageComponentContainer
    fun configureModuleComponents(container: StorageComponentContainer)
    fun configureModuleDependentCheckers(container: StorageComponentContainer)
}

abstract class PlatformDependentAnalyzerServices {
    private data class DefaultImportsKey(val includeCangJieComparisons: Boolean, val includeLowPriorityImports: Boolean)

    // TODO 默认导入
    private val defaultImports = LockBasedStorageManager("TargetPlatform").let { storageManager ->
        storageManager.createMemoizedFunction<DefaultImportsKey, List<ImportPath>> { (includeCangJieComparisons, includeLowPriorityImports) ->
            ArrayList<ImportPath>().apply {

                listOf(

                    "std.core.*",

                ).forEach { add(ImportPath.fromString(it)) }


//            computePlatformSpecificDefaultImports(storageManager, this)

                if (includeLowPriorityImports) {
                    addAll(defaultLowPriorityImports)
                }
            }
        }
    }

    abstract val platformConfigurator: PlatformConfigurator

    //
    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()

    //
    fun getDefaultImports(
        languageVersionSettings: LanguageVersionSettings,
        includeLowPriorityImports: Boolean
    ): List<ImportPath> =
        defaultImports(
            DefaultImportsKey(
                true,
                includeLowPriorityImports
            )
        )

    //
//    abstract fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>)
//
    open val excludedImports: List<FqName> get() = emptyList()

    //
    open fun dependencyOnBuiltIns(): DependencyOnBuiltIns =
        DependencyOnBuiltIns.LAST
}


object PlatformDependentAnalyzerServicesImpl : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = CangJiePlatformConfigurator

}
