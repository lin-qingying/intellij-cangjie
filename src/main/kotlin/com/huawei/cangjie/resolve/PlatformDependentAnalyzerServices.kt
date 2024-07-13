package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.container.StorageComponentContainer
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.storage.LockBasedStorageManager
import java.util.ArrayList

interface PlatformConfigurator {
    val platformSpecificContainer: StorageComponentContainer
    fun configureModuleComponents(container: StorageComponentContainer)
    fun configureModuleDependentCheckers(container: StorageComponentContainer)
}
abstract class PlatformDependentAnalyzerServices {
    private data class DefaultImportsKey(val includeCangJieComparisons: Boolean, val includeLowPriorityImports: Boolean)
//
private val defaultImports = LockBasedStorageManager("TargetPlatform").let { storageManager ->
    storageManager.createMemoizedFunction<DefaultImportsKey, List<ImportPath>> { (includeKotlinComparisons, includeLowPriorityImports) ->
        ArrayList<ImportPath>().apply {
//            listOf(
//                "kotlin.*",
//                "kotlin.annotation.*",
//                "kotlin.collections.*",
//                "kotlin.ranges.*",
//                "kotlin.sequences.*",
//                "kotlin.text.*",
//                "kotlin.io.*"
//            ).forEach { add(ImportPath.fromString(it)) }

            if (includeKotlinComparisons) {
                add(ImportPath.fromString("kotlin.comparisons.*"))
            }

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
//    open fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
//        ModuleInfo.DependencyOnBuiltIns.LAST
}


 object PlatformDependentAnalyzerServicesImpl : PlatformDependentAnalyzerServices(){
     override val platformConfigurator: PlatformConfigurator = CangJiePlatformConfigurator

 }
