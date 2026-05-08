package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * IDE 平台修改计数服务。
 *
 * 对外暴露的是 Analysis API 视角下的统一修改计数，
 * 内部由项目结构状态源统一整合 PSI 变化与显式失效。
 */
class CaIdeModificationTracker(
    project: Project,
) : CaModificationTracker {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override val modificationCount: Long
        get() = state.modificationCount

    override fun getModuleModificationCount(module: CaModule): Long =
        state.getModuleModificationCount(module)
}
