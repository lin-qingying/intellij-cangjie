package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * IDE 平台 session 失效服务。
 */
class CaIdeSessionInvalidationService(
    project: Project,
) : CaSessionInvalidationService {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override fun invalidate(modules: Set<CaModule>) {
        state.invalidate(modules)
    }
}
