@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider.CaIdeProjectStructureState
import org.cangnova.cangjie.projectStructure.CangJieProjectStructureProviderService

/**
 * IDE 项目结构辅助服务实现。
 *
 * 对位 Kotlin `K1IdeProjectStructureProviderService`。
 */
class CaIdeProjectStructureProviderService(
    private val project: Project,
) : CangJieProjectStructureProviderService {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override fun createLibraryModificationTracker(libraryModule: CaLibraryModule): ModificationTracker {
        return ModificationTracker { state.getModuleModificationCount(libraryModule) }
    }

    override fun incOutOfBlockModificationCount() {
        state.invalidateGlobal()
    }
}
