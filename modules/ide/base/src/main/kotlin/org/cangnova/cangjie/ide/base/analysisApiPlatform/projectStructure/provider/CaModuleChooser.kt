package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule

/**
 * 对位 Kotlin `ModuleChooser`。
 *
 * 当前候选通常只有一个，但仍保留独立选择层，
 * 以便 use-site module、builtins 与库候选的优先级规则集中在这里维护。
 */
internal object CaModuleChooser {
    fun chooseModule(
        modules: Sequence<CaModule>,
        useSiteModule: CaModule?,
    ): CaModule? {
        if (useSiteModule != null) {
            chooseByPriority(modules, useSiteModule)?.let { return it }
        }

        val allCandidates = modules.toList()
        return allCandidates.singleOrNull()
            ?: allCandidates.firstOrNull { it is CaLibrarySourceModule }
            ?: allCandidates.firstOrNull()
    }

    private fun chooseByPriority(
        modules: Sequence<CaModule>,
        useSiteModule: CaModule,
    ): CaModule? {
        var bestCandidate: CaModule? = null
        var bestPriority: ModulePriority? = null

        for (candidate in modules) {
            val priority = getCandidatePriority(candidate, useSiteModule) ?: continue
            if (priority.isTheHighestPriority) return candidate
            if (bestPriority == null || priority < bestPriority) {
                bestPriority = priority
                bestCandidate = candidate
            }
        }

        return bestCandidate
    }

    private fun getCandidatePriority(candidate: CaModule, useSiteModule: CaModule): ModulePriority? {
        when (useSiteModule) {
            candidate -> return ModulePriority.Self
            is CaDanglingFileModule -> return getCandidatePriority(candidate, useSiteModule.contextModule)
        }

        when (candidate) {
            is CaBuiltinsModule -> return ModulePriority.BuiltIns
            is CaNotUnderContentRootModule, is CaDanglingFileModule -> return null
        }

        when (useSiteModule) {
            is CaLibraryModule, is CaLibrarySourceModule -> return null
            is CaNotUnderContentRootModule -> return null
            is CaBuiltinsModule -> return null
        }

        val dependencyIndex = useSiteModule.allDirectDependencies.indexOf(candidate)
        if (dependencyIndex < 0) return null
        return ModulePriority.ModuleDependency(dependencyIndex)
    }

    private sealed class ModulePriority : Comparable<ModulePriority> {
        protected abstract val priorityNumber: Int

        val isTheHighestPriority: Boolean
            get() = priorityNumber == 0

        override fun compareTo(other: ModulePriority): Int =
            priorityNumber.compareTo(other.priorityNumber)

        data object Self : ModulePriority() {
            override val priorityNumber: Int
                get() = 0
        }

        data object BuiltIns : ModulePriority() {
            override val priorityNumber: Int
                get() = Int.MAX_VALUE
        }

        class ModuleDependency(
            private val indexInClassPath: Int,
        ) : ModulePriority() {
            override val priorityNumber: Int
                get() = indexInClassPath + 1
        }
    }
}
