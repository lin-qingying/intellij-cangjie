package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieModuleDependentsProviderBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider.CaIdeProjectStructureState

/**
 * IDE 平台的模块依赖方提供器。
 *
 * 当前仓库的项目结构真相集中在 [CaIdeProjectStructureState.snapshot] 中，因此这里与 Kotlin IDE provider
 * 一样承担“给定模块，返回依赖它的模块集合”的职责，但依赖源直接使用本地统一模块图快照，而不是再平行维护一份
 * Workspace Model 到 Analysis API 模块的映射缓存。
 */
@OptIn(CaPlatformInterface::class)
class CaIdeModuleDependentsProvider(
    private val project: Project,
) : CangJieModuleDependentsProviderBase() {
    private val projectStructureState: CaIdeProjectStructureState
        get() = project.getService(CaIdeProjectStructureState::class.java)

    /**
     * CachedValue 依赖必须是 IntelliJ 认可的依赖类型。
     * 这里对齐 Kotlin 平台层做法，用统一修改计数驱动模块依赖图缓存失效，
     * 而不是把 project service 本身塞进依赖列表。
     */
    private val projectStructureModificationTracker = ModificationTracker {
        projectStructureState.modificationCount
    }

    override fun getDirectDependents(module: CaModule): Set<CaModule> {
        return directDependentsCache.value[module].orEmpty()
    }

    override fun getRefinementDependents(module: CaModule): Set<CaModule> {
        return emptySet()
    }

    private val directDependentsCache: CachedValue<Map<CaModule, Set<CaModule>>> =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result.create(
                buildDirectDependentsIndex(projectStructureState.snapshot.allModules),
                projectStructureModificationTracker,
            )
        }

    private fun buildDirectDependentsIndex(allModules: List<CaModule>): Map<CaModule, Set<CaModule>> {
        val index = linkedMapOf<CaModule, LinkedHashSet<CaModule>>()

        for (candidateDependent in allModules) {
            for (dependency in candidateDependent.allDirectDependencies) {
                if (dependency === candidateDependent) continue
                index.getOrPut(dependency) { linkedSetOf() }.add(candidateDependent)
            }
        }

        return index.mapValues { (_, dependents) -> dependents.toSet() }
    }
}
