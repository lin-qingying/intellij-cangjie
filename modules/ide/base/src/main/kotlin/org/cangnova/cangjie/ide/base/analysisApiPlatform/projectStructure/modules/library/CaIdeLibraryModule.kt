package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.psi.PsiFileSystemItem

/**
 * IDE 中的库二进制模块。
 */
internal class CaIdeLibraryModule(
    project: Project,
    entityId: LibraryId,
    libraryName: String,
    binaryRoots: List<PsiFileSystemItem>,
) : CaIdeLibraryModuleBase(
    project = project,
    entityId = entityId,
    libraryName = libraryName,
    binaryRoots = binaryRoots,
) {
    override val isResolvable: Boolean
        get() = false

    override val moduleDescription: String
        get() = "IDE library binaries $libraryName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CaIdeLibraryModule && entityId == other.entityId
    }

    override fun hashCode(): Int {
        return entityId.hashCode()
    }
}
