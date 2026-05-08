package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeMutableModule

/**
 * IDE 平台库二进制模块基类。
 *
 * 对位 Kotlin `KaEntityBasedLibraryModuleBase` / `KaLibraryEntityBasedLibraryModuleBase`：
 * - workspace `LibraryId` 是唯一主身份；
 * - 二进制根直接决定内容作用域；
 * - 库模块默认不额外声明 friend / dependsOn 语义。
 */
internal abstract class CaIdeLibraryModuleBase(
    project: Project,
    internal val entityId: LibraryId,
    final override val libraryName: String,
    final override val binaryRoots: List<PsiFileSystemItem>,
) : CaIdeMutableModule(project, { binaryRoots }, includeLibrariesInScope = true), CaLibraryModule {
    private val psiManager = PsiManager.getInstance(project)

    /**
     * 对位 Kotlin `KaEntityBasedLibraryModuleBase.entity`。
     *
     * IDE 库模块的额外结构信息必须回到 workspace `LibraryEntity` 取，
     * 不能把 openapi Library 或外部收集到的 root 列表固化在模块实例上。
     */
    internal val entity: LibraryEntity
        get() = entityId.resolve(project.workspaceModel.currentSnapshot)
            ?: error("无法按 workspace library id `${entityId.presentableName}` 恢复 LibraryEntity。")

    internal fun computeRoots(rootType: LibraryRootTypeId): List<PsiFileSystemItem> {
        return entity.roots
            .filter { root -> root.type == rootType }
            .mapNotNull { root -> root.url.virtualFile }
            .mapNotNull { file -> psiManager.findDirectory(file) ?: psiManager.findFile(file) }
    }

    final override val stableModuleName: String
        get() = "ide-library:${entityId.presentableName}"
}
