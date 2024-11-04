package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.container.StorageComponentContainer
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.storage.LockBasedStorageManager

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
