@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider.SnapshotStamp

/**
 * IDE 平台模块的公共基类。
 *
 * IDE 项目结构需要在模块图装配阶段回填依赖，因此这里显式暴露可变依赖集合。
 * 这些集合只在平台结构服务内部维护，对 Analysis API 消费方仍然表现为只读视图。
 */
internal abstract class CaIdeMutableModule(
    final override val project: Project,
    private val scopeRootsProvider: () -> List<PsiFileSystemItem>,
    private val includeLibrariesInScope: Boolean,
) : CaModuleBase() {
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()
    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()
    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()
    private val dependencyRefreshLock = Any()

    @Volatile
    private var dependencyRefreshState: DependencyRefreshState = DependencyRefreshState.Idle

    protected fun currentScopeRoots(): List<PsiFileSystemItem> = scopeRootsProvider()

    override val baseContentScope: GlobalSearchScope
        get() = CaIdeModuleContentScope(project, currentScopeRoots().mapNotNull { it.virtualFile }, includeLibrariesInScope)

    fun tryStartDependencyRefresh(snapshotStamp: SnapshotStamp): Boolean {
        synchronized(dependencyRefreshLock) {
            return when (val state = dependencyRefreshState) {
                is DependencyRefreshState.Completed ->
                    if (state.snapshotStamp == snapshotStamp) {
                        false
                    } else {
                        dependencyRefreshState = DependencyRefreshState.Refreshing(snapshotStamp)
                        true
                    }

                is DependencyRefreshState.Refreshing ->
                    if (state.snapshotStamp == snapshotStamp) {
                        false
                    } else {
                        dependencyRefreshState = DependencyRefreshState.Refreshing(snapshotStamp)
                        true
                    }

                DependencyRefreshState.Idle -> {
                    dependencyRefreshState = DependencyRefreshState.Refreshing(snapshotStamp)
                    true
                }
            }
        }
    }

    fun finishDependencyRefresh(snapshotStamp: SnapshotStamp) {
        synchronized(dependencyRefreshLock) {
            dependencyRefreshState = DependencyRefreshState.Completed(snapshotStamp)
        }
    }

    fun resetDependencyRefresh(snapshotStamp: SnapshotStamp) {
        synchronized(dependencyRefreshLock) {
            val state = dependencyRefreshState
            if (state is DependencyRefreshState.Refreshing && state.snapshotStamp == snapshotStamp) {
                dependencyRefreshState = DependencyRefreshState.Idle
            }
        }
    }

    private sealed interface DependencyRefreshState {
        data object Idle : DependencyRefreshState
        data class Refreshing(val snapshotStamp: SnapshotStamp) : DependencyRefreshState
        data class Completed(val snapshotStamp: SnapshotStamp) : DependencyRefreshState
    }
}
