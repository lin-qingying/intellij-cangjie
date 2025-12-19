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

package org.cangnova.cangjie.moduleinfo;
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.*
import com.intellij.util.applyIf
import org.cangnova.cangjie.utils.CangJieSourceRootType
import org.cangnova.cangjie.utils.SourceCangJieRootType
import org.cangnova.cangjie.utils.TestSourceCangJieRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

@Service(Service.Level.PROJECT)
class ModuleDependencyCollector(private val project: Project) {
    companion object {
        private val LOG = Logger.getInstance(ModuleDependencyCollector::class.java)

        fun getInstance(project: Project): ModuleDependencyCollector = project.service()
    }

    enum class CollectionMode {
        COLLECT_IGNORED,
        COLLECT_NON_IGNORED;
    }


    fun collectModuleDependencies(
        module: Module,

        sourceRootType: CangJieSourceRootType,
        includeExportedDependencies: Boolean,
        collectionMode: CollectionMode = CollectionMode.COLLECT_NON_IGNORED,
    ): Collection<IdeaModuleInfo> {
        val debugInfo = if (LOG.isDebugEnabled) ArrayList<String>() else null

        val orderEnumerator = getOrderEnumerator(module, sourceRootType, includeExportedDependencies)

        val dependencyFilter = when {
            module.isHMPPEnabled -> HmppSourceModuleDependencyFilter(platform)
            else -> NonHmppSourceModuleDependenciesFilter(platform)
        }.applyIf(collectionMode == CollectionMode.COLLECT_IGNORED, ::NegatedModuleDependencyFilter)

        val result = LinkedHashSet<IdeaModuleInfo>()

        orderEnumerator.forEach { orderEntry ->
            if (isApplicable(orderEntry, sourceRootType)) {
                debugInfo?.add("Add entry ${orderEntry.presentableName}")
                for (moduleInfo in collectModuleDependenciesForOrderEntry(orderEntry, sourceRootType)) {
                    if (dependencyFilter.isSupportedDependency(moduleInfo)) {
                        debugInfo?.add("Add module ${moduleInfo.displayedName}")
                        result.add(moduleInfo)
                    }
                }
            } else {
                debugInfo?.add("Skip entry ${orderEntry.presentableName}")
            }

            return@forEach true
        }

        if (debugInfo != null) {
            val debugString = buildString {
                appendLine("Building dependency list for module ${this}")
                appendLine("Platform = ${platform}, isForTests = ${sourceRootType == TestSourceCangJieRootType}")
                debugInfo.joinTo(this, separator = "; ", prefix = "[", postfix = "]")
            }
            LOG.debug(debugString)
        }

        return result
    }

    private fun getOrderEnumerator(
        module: Module,
        sourceRootType: CangJieSourceRootType,
        includeExportedDependencies: Boolean,
    ): OrderEnumerator {
        val rootManager = ModuleRootManager.getInstance(module)

        val dependencyEnumerator = rootManager.orderEntries().compileOnly()
        if (includeExportedDependencies) {
            dependencyEnumerator.recursively().exportedOnly()
        }

        if (sourceRootType == SourceCangJieRootType && module.buildSystemType == BuildSystemType.JPS) {
            dependencyEnumerator.productionOnly()
        }

        return dependencyEnumerator
    }

    private fun isApplicable(orderEntry: OrderEntry, sourceRootType: CangJieSourceRootType): Boolean {
        if (!orderEntry.isValid) {
            return false
        }

        return orderEntry !is ExportableOrderEntry
                || sourceRootType == TestSourceCangJieRootType
                || orderEntry is ModuleOrderEntry && orderEntry.isProductionOnTestDependency
                || orderEntry.scope.isForProductionCompile
    }

    private fun collectModuleDependenciesForOrderEntry(orderEntry: OrderEntry, sourceRootType: CangJieSourceRootType): List<IdeaModuleInfo> {
        fun Module.toInfos() = sourceModuleInfos.filter {
            sourceRootType == TestSourceCangJieRootType || it is ModuleProductionSourceInfo
        }

        return when (orderEntry) {
            is ModuleSourceOrderEntry -> {
                orderEntry.ownerModule.toInfos()
            }

            is ModuleOrderEntry -> {
                val module = orderEntry.module ?: return emptyList()
                if (sourceRootType == SourceCangJieRootType && orderEntry.isProductionOnTestDependency) {
                    listOfNotNull(module.testSourceInfo)
                } else {
                    module.toInfos()
                }
            }

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
val Module.productionSourceInfo: ModuleProductionSourceInfo?
    get() {
        val hasProductionRoots = hasRootsOfType(setOf(JavaSourceRootType.SOURCE, SourceKotlinRootType))


        return if (hasProductionRoots) ModuleProductionSourceInfo(this) else null
    }
val Module.testSourceInfo: ModuleTestSourceInfo?
    get() {
        val hasTestRoots = hasRootsOfType(setOf(JavaSourceRootType.TEST_SOURCE, TestSourceKotlinRootType))


        return if (hasTestRoots) ModuleTestSourceInfo(this) else null
    }
val Module.sourceModuleInfos: List<ModuleSourceInfo>
    get() = listOfNotNull(testSourceInfo, productionSourceInfo)

private fun Module.hasRootsOfType(rootTypes: Set<JpsModuleSourceRootType<*>>): Boolean {
    return rootManager.contentEntries.any { it.getSourceFolders(rootTypes).isNotEmpty() }
}