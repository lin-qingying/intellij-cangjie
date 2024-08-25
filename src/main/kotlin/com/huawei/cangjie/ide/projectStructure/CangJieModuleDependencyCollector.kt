package com.huawei.cangjie.ide.projectStructure

import com.huawei.cangjie.analyzer.CangJieModuleInfo
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.ide.cache.project.LibraryInfoCache
import com.huawei.cangjie.ide.cache.project.moduleInfos
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*

@Service(Service.Level.PROJECT)

class CangJieModuleDependencyCollector (private val project: Project){

    companion object {
        private val LOG = Logger.getInstance(CangJieModuleDependencyCollector::class.java)

        fun getInstance(project: Project): CangJieModuleDependencyCollector = project.service()

    }

    enum class CollectionMode {
        COLLECT_IGNORED,
        COLLECT_NON_IGNORED;
    }

    fun collectModuleDependencies(
        module: Module,
//        platform: TargetPlatform,
//        sourceRootType: CangJieSourceRootType,
        includeExportedDependencies: Boolean,
        collectionMode: CollectionMode = CollectionMode.COLLECT_NON_IGNORED,
    ): Collection<ModuleInfo> {
        val debugInfo = if (LOG.isDebugEnabled) ArrayList<String>() else null

        val orderEnumerator = getOrderEnumerator(module,/* sourceRootType,*/ includeExportedDependencies)

//        val dependencyFilter = when {
//            module.isHMPPEnabled -> HmppSourceModuleDependencyFilter(platform)
//            else -> NonHmppSourceModuleDependenciesFilter(platform)
//        }.applyIf(collectionMode == CollectionMode.COLLECT_IGNORED, ::NegatedModuleDependencyFilter)

        val result = LinkedHashSet<ModuleInfo>()

        orderEnumerator.forEach { orderEntry ->
            if (isApplicable(orderEntry/*, sourceRootType*/)) {
                debugInfo?.add("Add entry ${orderEntry.presentableName}")
                for (moduleInfo in collectModuleDependenciesForOrderEntry(orderEntry/*, sourceRootType*/)) {
//                    if (dependencyFilter.isSupportedDependency(moduleInfo)) {
                    debugInfo?.add("Add module ${moduleInfo.displayedName}")
                    result.add(moduleInfo)
//                    }
                }
            } else {
                debugInfo?.add("Skip entry ${orderEntry.presentableName}")
            }

            return@forEach true
        }

        if (debugInfo != null) {
            val debugString = buildString {
                appendLine("Building dependency list for module ${this}")
//                appendLine("Platform = ${platform}, isForTests = ${sourceRootType == TestSourceCangJieRootType}")
                debugInfo.joinTo(this, separator = "; ", prefix = "[", postfix = "]")
            }
            LOG.debug(debugString)
        }

        return result
    }

    private fun getOrderEnumerator(
        module: Module,
//        sourceRootType: CangJieSourceRootType,
        includeExportedDependencies: Boolean,
    ): OrderEnumerator {
        val rootManager = ModuleRootManager.getInstance(module)

        val dependencyEnumerator = rootManager.orderEntries().compileOnly()
        if (includeExportedDependencies) {
            dependencyEnumerator.recursively().exportedOnly()
        }

//        if (sourceRootType == SourceCangJieRootType && module.buildSystemType == BuildSystemType.JPS) {
//            dependencyEnumerator.productionOnly()
//        }

        return dependencyEnumerator
    }

    private fun isApplicable(orderEntry: OrderEntry/*, sourceRootType: CangJieSourceRootType*/): Boolean {
        if (!orderEntry.isValid) {
            return false
        }

        return orderEntry !is ExportableOrderEntry
//                || sourceRootType == TestSourceCangJieRootType
                || orderEntry is ModuleOrderEntry && orderEntry.isProductionOnTestDependency
                || orderEntry.scope.isForProductionCompile
    }

    private fun collectModuleDependenciesForOrderEntry(orderEntry: OrderEntry/*, sourceRootType: CangJieSourceRootType*/): List< ModuleInfo> {
        fun Module.toInfos() = moduleInfos.filter {
            /*   sourceRootType == TestSourceCangJieRootType ||*/ it is CangJieModuleInfo
        }

        return when (orderEntry) {
            is ModuleSourceOrderEntry -> {
                orderEntry.ownerModule.toInfos()
            }

//            is ModuleOrderEntry -> {
//                val module = orderEntry.module ?: return emptyList()
//                if (sourceRootType == SourceCangJieRootType && orderEntry.isProductionOnTestDependency) {
//                    listOfNotNull(module.testSourceInfo)
//                } else {
//                    module.toInfos()
//                }
//            }

            is LibraryOrderEntry -> {
                val library = orderEntry.library ?: return listOf()
                LibraryInfoCache.getInstance(project)[library]
            }


            else -> {
                throw IllegalStateException("Unexpected order entry $orderEntry")
            }
        }
    }

}
