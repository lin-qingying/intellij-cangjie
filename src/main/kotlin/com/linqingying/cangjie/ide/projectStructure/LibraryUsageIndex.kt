package com.linqingying.cangjie.ide.projectStructure

import com.linqingying.cangjie.analyzer.LibraryInfo
import com.linqingying.cangjie.ide.cache.project.LibraryInfoCache
import com.linqingying.cangjie.ide.cache.project.getIdeaModelInfosCache
import com.linqingying.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap

/**
 * For each [LibraryInfo], [LibraryUsageIndex] contains all [Module]s which depend on that library info. The index supports deduplicated
 * libraries: if a [LibraryInfo] comprises two [Library] instances with the same roots, [getDependentModules] will return the union of both
 * [Library]'s dependents.
 *
 * The resulting dependents are stable and do not depend on the state of [LibraryInfoCache], as they are computed from the project model.
 */
interface LibraryUsageIndex {
    fun getDependentModules(libraryInfo: LibraryInfo): Sequence<Module>
    fun hasDependentModule(libraryInfo: LibraryInfo, module: Module): Boolean
}

class LibraryUsageIndexImpl(private val project: Project) : LibraryUsageIndex {
    private val moduleDependentsByLibrary: CachedValue<MultiMap<Library, Module>> =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result(
                computeLibraryModuleDependents(),
//                ModuleModificationTracker.getInstance(project),
//                JavaLibraryModificationTracker.getInstance(project),
            )
        }

    private fun computeLibraryModuleDependents(): MultiMap<Library, Module> = runReadAction {
        val moduleDependentsByLibrary = MultiMap.createSet<Library, Module>()
        val libraryCache = LibraryInfoCache.getInstance(project)
        for (module in ModuleManager.getInstance(project).modules) {
            checkCanceled()
            for (entry in ModuleRootManager.getInstance(module).orderEntries) {
                if (entry !is LibraryOrderEntry) continue
                val library = entry.library ?: continue
                val keyLibrary = libraryCache.deduplicatedLibrary(library)
                moduleDependentsByLibrary.putValue(keyLibrary, module)
            }
        }

        moduleDependentsByLibrary
    }

    override fun getDependentModules(libraryInfo: LibraryInfo): Sequence<Module> = sequence<Module> {
        val ideaModelInfosCache = getIdeaModelInfosCache(project)
        for (module in moduleDependentsByLibrary.value[libraryInfo.library]) {
            val mappedModuleInfos = ideaModelInfosCache.getModuleInfosForModule(module)
            if (mappedModuleInfos.any { true }) {
                yield(module)
            }
        }
    }

    override fun hasDependentModule(libraryInfo: LibraryInfo, module: Module): Boolean {
        return module in moduleDependentsByLibrary.value[libraryInfo.library]

    }
}
