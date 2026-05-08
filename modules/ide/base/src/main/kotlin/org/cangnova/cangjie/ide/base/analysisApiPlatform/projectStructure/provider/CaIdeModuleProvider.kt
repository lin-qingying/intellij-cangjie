@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * IDE 平台模块图提供器。
 */
class CaIdeModuleProvider(
    project: Project,
) : CaModuleProvider {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return state.getModuleByStableName(stableModuleName)
    }
}
