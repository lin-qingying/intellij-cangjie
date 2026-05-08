@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.ModuleId
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeBuiltinsModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeLibraryFallbackDependenciesModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library.CaIdeLibraryModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.librarySource.CaIdeLibrarySourceModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source.CaIdeSourceModule
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind
import java.util.concurrent.ConcurrentHashMap

/**
 * IDE project-structure 的模块缓存仓库。
 *
 * 对位 Kotlin `K2IDEProjectStructureProviderCache`：
 * 这里只负责稳定可复用模块实例的缓存与复用，
 * 不承担快照装配、候选选择、依赖恢复这些 provider/state 级职责。
 */
class CaIdeProjectStructureProviderCache(
    private val project: Project,
) {
    private val productionSourceModulesById = ConcurrentHashMap<ModuleId, CaIdeSourceModule>()
    private val testSourceModulesById = ConcurrentHashMap<ModuleId, CaIdeSourceModule>()
    private val libraryModulesByKey = ConcurrentHashMap<LibraryId, CaIdeLibraryModule>()
    private val librarySourceModulesByKey = ConcurrentHashMap<LibrarySourceKey, CaIdeLibrarySourceModule>()
    private val fallbackDependencyModulesByOwnerKey = ConcurrentHashMap<String, CaIdeLibraryFallbackDependenciesModule>()

    private val builtinsModule: CaIdeBuiltinsModule by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaIdeBuiltinsModule(project = project)
    }

    internal fun getBuiltinsModule(): CaIdeBuiltinsModule = builtinsModule

    internal fun cachedSourceModule(
        moduleId: ModuleId,
        kind: CaSourceModuleKind,
        builder: () -> CaIdeSourceModule,
    ): CaIdeSourceModule {
        return sourceModuleCache(kind).computeIfAbsent(moduleId) { builder() }
    }

    internal fun cachedLibraryBinaryModule(
        libraryId: LibraryId,
        builder: () -> CaIdeLibraryModule,
    ): CaIdeLibraryModule {
        return libraryModulesByKey.computeIfAbsent(libraryId) { builder() }
    }

    internal fun cachedLibrarySourceModule(
        key: LibrarySourceKey,
        builder: () -> CaIdeLibrarySourceModule,
    ): CaIdeLibrarySourceModule {
        return librarySourceModulesByKey.computeIfAbsent(key) { builder() }
    }

    internal fun cachedFallbackDependencyModule(
        ownerKey: String,
        builder: () -> CaIdeLibraryFallbackDependenciesModule,
    ): CaIdeLibraryFallbackDependenciesModule {
        return fallbackDependencyModulesByOwnerKey.computeIfAbsent(ownerKey) { builder() }
    }

    internal fun libraryModules(): Collection<CaIdeLibraryModule> = libraryModulesByKey.values

    internal fun librarySourceModules(): Collection<CaIdeLibrarySourceModule> = librarySourceModulesByKey.values

    internal fun fallbackDependencyModules(): Collection<CaIdeLibraryFallbackDependenciesModule> =
        fallbackDependencyModulesByOwnerKey.values

    private fun sourceModuleCache(kind: CaSourceModuleKind): ConcurrentHashMap<ModuleId, CaIdeSourceModule> {
        return when (kind) {
            CaSourceModuleKind.PRODUCTION -> productionSourceModulesById
            CaSourceModuleKind.TEST -> testSourceModulesById
        }
    }
}

internal data class LibrarySourceKey(
    val libraryId: LibraryId,
)
