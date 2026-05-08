@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule

/**
 * IDE 项目结构辅助服务。
 *
 * 对位 Kotlin `ProjectStructureProviderService`。
 * Kotlin 这里服务于 `LibraryInfo` 与 FE10 时代的 project-structure 缓存；
 * 仓颉没有对应的 FE10 `LibraryInfo` 层，因此显式收敛到当前真实存在的 `CaLibraryModule`。
 */
interface CangJieProjectStructureProviderService {
    fun createLibraryModificationTracker(libraryModule: CaLibraryModule): ModificationTracker

    fun incOutOfBlockModificationCount()

    companion object {
        fun getInstance(project: Project): CangJieProjectStructureProviderService = project.service()
    }
}
