package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.analyzer.CangJieModuleInfo
import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.analyzer.ModuleOrigin
import com.linqingying.cangjie.analyzer.projectSourceModules
import com.linqingying.cangjie.ide.cache.PerModulePackageCacheService
import com.linqingying.cangjie.name.FqName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project


interface PackageOracle {
    fun packageExists(fqName: FqName): Boolean

    object Optimistic : PackageOracle {
        override fun packageExists(fqName: FqName): Boolean = true
    }
}

interface PackageOracleFactory {
    fun createOracle(moduleInfo: ModuleInfo): PackageOracle

    object OptimisticFactory : PackageOracleFactory {
        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
    }
}
@Service(Service.Level.PROJECT)
class IdePackageOracleFactory(val project: Project) : PackageOracleFactory {
    override fun createOracle(moduleInfo: ModuleInfo): PackageOracle {
        if (moduleInfo !is CangJieModuleInfo) return PackageOracle.Optimistic

        return when (moduleInfo.moduleOrigin) {
            ModuleOrigin.MODULE -> CangJieSourceFilesOracle(moduleInfo, project)
            else -> PackageOracle.Optimistic // binaries for non-jvm platform need some oracles based on their structure
        }

    }


    private class CangJieSourceFilesOracle(moduleInfo: ModuleInfo, private val project: Project) : PackageOracle {
        private val cacheService: PerModulePackageCacheService = project.service()
        private val sourceModules = moduleInfo.projectSourceModules()
        override fun packageExists(fqName: FqName): Boolean {
            return sourceModules.any { cacheService.packageExists(fqName, it) }
        }
    }


}
