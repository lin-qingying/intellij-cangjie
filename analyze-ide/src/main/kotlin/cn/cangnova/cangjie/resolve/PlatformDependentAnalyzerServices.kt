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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.ModuleInfo
import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.container.StorageComponentContainer

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
//emptyList<String>()
                listOf(

                    "std.core.*",
//"<built-ins module>"
//                    "untitled3.A.*"
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
                languageVersionSettings.supportsFeature(LanguageFeature.DefaultImportOfPackageCangJieComparisons),
                includeLowPriorityImports
            )
        )

    //
//    abstract fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>)
//
    open val excludedImports: List<FqName> get() = emptyList()

    //
    open fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
        ModuleInfo.DependencyOnBuiltIns.LAST
}


object PlatformDependentAnalyzerServicesImpl : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = CangJiePlatformConfigurator

}
