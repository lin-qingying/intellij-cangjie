@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import com.intellij.workspaceModel.ide.legacyBridge.findModule
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeMutableModule
import org.cangnova.cangjie.config.CangJieSourceRootTypes
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind
import org.cangnova.cangjie.projectStructure.CaSourceModuleWithKind

/**
 * IDE 平台源码模块基类。
 *
 * 这里对位 Kotlin `KaSourceModuleBase` 的层次职责：
 * 具体实现类只补 stable identity 与相等性，源码模块的公共视图统一收敛在基类。
 */
internal abstract class CaIdeSourceModuleBase(
    project: Project,
    internal val entityId: ModuleId,
    final override val kind: CaSourceModuleKind,
) : CaIdeMutableModule(project, { computePsiRoots(project, entityId, kind) }, includeLibrariesInScope = false), CaSourceModule, CaSourceModuleWithKind {
    /**
     * 对位 Kotlin `KaSourceModuleBase.module` 的职责：
     * source module 不固化 openapi module 实例，而是从 workspace `ModuleEntity` 回桥接到当前 IntelliJ module。
     */
    internal val openapiModule: Module
        get() {
            val snapshot = project.workspaceModel.currentSnapshot
            val entity = entityId.resolve(snapshot)
                ?: error("无法按 workspace module id `${entityId.name}` 恢复 ModuleEntity。")
            return entity.findModule(snapshot)
                ?: error("无法按 workspace module id `${entityId.name}` 恢复 IntelliJ module。")
        }

    override val name: String
        get() = openapiModule.name

    override val languageVersionSettings: LanguageVersionSettings
        get() = CangJieProjectStructureProvider.getInstance(project).globalLanguageVersionSettings

    override val psiRoots: List<PsiFileSystemItem>
        get() = currentScopeRoots()

    override val moduleDescription: String
        get() = "IDE source module ${openapiModule.name} (${if (kind == CaSourceModuleKind.TEST) "test" else "production"})"
}

private fun computePsiRoots(
    project: Project,
    moduleId: ModuleId,
    kind: CaSourceModuleKind,
): List<PsiFileSystemItem> {
    val projectRootManager = ProjectRootManager.getInstance(project)
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    val psiManager = PsiManager.getInstance(project)

    return projectRootManager.contentSourceRoots
        .filter { root ->
            val module = projectFileIndex.getModuleForFile(root) ?: return@filter false
            val workspaceModuleId = module.workspaceModuleId(project) ?: return@filter false
            if (workspaceModuleId != moduleId) return@filter false

            module.sourceModuleKindForRoot(root) == kind
        }
        .mapNotNull { root -> psiManager.findDirectory(root) ?: psiManager.findFile(root) }
        .sortedBy { it.virtualFile.path }
}

private fun Module.sourceModuleKindForRoot(root: com.intellij.openapi.vfs.VirtualFile): CaSourceModuleKind {
    val sourceRootTypeId = ModuleRootManager.getInstance(this)
        .contentEntries
        .asSequence()
        .flatMap { it.sourceFolders.asSequence() }
        .firstOrNull { it.file?.url == root.url }
        ?.rootType
        ?.let(CangJieSourceRootTypes::findIdByType)

    return if (CangJieSourceRootTypes.isTestSource(sourceRootTypeId)) {
        CaSourceModuleKind.TEST
    } else {
        CaSourceModuleKind.PRODUCTION
    }
}

private fun Module.workspaceModuleId(project: Project): ModuleId? {
    if (this !is ModuleBridge) return null
    return findModuleEntity(project.workspaceModel.currentSnapshot)?.symbolicId
}
