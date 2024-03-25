package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.StorageComponentContainer

interface PlatformConfigurator {
    val platformSpecificContainer: StorageComponentContainer
    fun configureModuleComponents(container: StorageComponentContainer)
    fun configureModuleDependentCheckers(container: StorageComponentContainer)
}
abstract class PlatformDependentAnalyzerServices {
    private data class DefaultImportsKey(val includeCangJieComparisons: Boolean, val includeLowPriorityImports: Boolean)
//
//    private val defaultImports = LockBasedStorageManager("TargetPlatform").let { storageManager ->
//        storageManager.createMemoizedFunction<DefaultImportsKey, List<ImportPath>> { (includeCangJieComparisons, includeLowPriorityImports) ->
//            ArrayList<ImportPath>().apply {
//                listOf(
//
//                ).forEach { add(ImportPath.fromString(it)) }
//
//                if (includeCangJieComparisons) {
//
//                }
//
//                computePlatformSpecificDefaultImports(storageManager, this)
//
//                if (includeLowPriorityImports) {
//                    addAll(defaultLowPriorityImports)
//                }
//            }
//        }
//    }

    abstract val platformConfigurator: PlatformConfigurator
//
//    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()
//
//    fun getDefaultImports(
//        languageVersionSettings: LanguageVersionSettings,
//        includeLowPriorityImports: Boolean
//    ): List<ImportPath> =
//        defaultImports(
//            DefaultImportsKey(
//                languageVersionSettings.supportsFeature(LanguageFeature.DefaultImportOfPackageCangJieComparisons),
//                includeLowPriorityImports
//            )
//        )
//
//    abstract fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>)
//
//    open val excludedImports: List<FqName> get() = emptyList()
//
//    open fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
//        ModuleInfo.DependencyOnBuiltIns.LAST
}
