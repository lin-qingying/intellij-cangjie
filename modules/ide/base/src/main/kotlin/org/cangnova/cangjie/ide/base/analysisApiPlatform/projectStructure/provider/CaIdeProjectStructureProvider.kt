@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.ModificationTracker
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.workspaceModel.ide.impl.legacyBridge.library.LibraryBridge
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.CaNotUnderContentRootModuleFactory
import org.cangnova.cangjie.ide.base.facet.implementingModules
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library.CaIdeLibraryModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source.CaIdeSourceModuleBase
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind
import org.cangnova.cangjie.projectStructure.CangJieIdeProjectStructureProvider
import org.cangnova.cangjie.projectStructure.openapiModule
import org.cangnova.cangjie.projectStructure.sourceModuleKind
import org.cangnova.cangjie.projectStructure.toCaSourceModule

/**
 * IDE 平台项目结构提供器。
 *
 * 对位 Kotlin `K2IDEProjectStructureProvider`：
 * use-site module 选择协议保留在 provider 层，模块图与依赖恢复细节统一委托给状态服务。
 */
class CaIdeProjectStructureProvider(
    private val project: Project,
) : CangJieIdeProjectStructureProvider() {
    private val state = project.getService(CaIdeProjectStructureState::class.java)
    private val projectStructureModificationTracker = ModificationTracker {
        state.modificationCount
    }

    override val self: CangJieIdeProjectStructureProvider
        get() = this

    override fun getCaSourceModule(moduleId: ModuleId, kind: CaSourceModuleKind): CaSourceModule? {
        val moduleEntity = moduleId.resolve(project.workspaceModel.currentSnapshot) ?: return null
        return getCaSourceModule(moduleEntity, kind)
    }

    override fun getCaSourceModules(moduleId: ModuleId): List<CaSourceModule> {
        val moduleEntity = moduleId.resolve(project.workspaceModel.currentSnapshot) ?: return emptyList()
        return getCaSourceModules(moduleEntity)
    }

    override fun getCaSourceModule(moduleEntity: ModuleEntity, kind: CaSourceModuleKind): CaSourceModule? =
        state.getSourceModule(moduleEntity.symbolicId, kind)

    override fun getCaSourceModules(moduleEntity: ModuleEntity): List<CaSourceModule> {
        val productionModule = getCaSourceModule(moduleEntity, CaSourceModuleKind.PRODUCTION)
        val testModule = getCaSourceModule(moduleEntity, CaSourceModuleKind.TEST)
        return listOfNotNull(productionModule, testModule)
    }

    override fun getCaSourceModuleSymbolId(module: CaSourceModule): ModuleId {
        require(module is CaIdeSourceModuleBase) {
            "Expected ${CaIdeSourceModuleBase::class}, but got ${module::class} instead"
        }
        return module.entityId
    }

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        val containingFile = element.containingFile
            ?: error("无法为 `${element::class.simpleName}` 选择 Analysis API use-site module：元素不位于 PSI 文件中。")
        val containingItem = containingFile as? PsiFileSystemItem
            ?: error("无法为 `${element::class.simpleName}` 选择 Analysis API use-site module：元素不位于 PSI 文件中。")

        computeSpecialModule(containingFile)?.let { return it }
        return cachedCaModule(containingItem, useSiteModule)
    }

    private fun computeCaModule(
        containingItem: PsiFileSystemItem,
        useSiteModule: CaModule?,
    ): CaModule {
        val candidates = CaIdeCandidateCollector.collectCandidates(containingItem, state)
            .map { candidate -> state.resolveCandidate(candidate) }
        return CaModuleChooser.chooseModule(candidates, useSiteModule)
            ?: getNotUnderContentRootModule(project)
    }

    override fun getImplementingModules(module: CaModule): List<CaModule> {
        return when (module) {
            is CaSourceModule -> {
                val moduleKind = module.sourceModuleKind
                module.openapiModule.implementingModules.mapNotNull { it.toCaSourceModule(moduleKind) }
            }

            else -> emptyList()
        }
    }

    override fun getCaSourceModule(openapiModule: com.intellij.openapi.module.Module, kind: CaSourceModuleKind): CaSourceModule? {
        val moduleEntity = getModuleEntity(openapiModule) ?: return null
        return getCaSourceModule(moduleEntity, kind)
    }

    override fun getCaSourceModules(openapiModule: com.intellij.openapi.module.Module): List<CaSourceModule> {
        val moduleEntity = getModuleEntity(openapiModule) ?: return emptyList()
        return getCaSourceModules(moduleEntity)
    }

    override fun getOpenapiModule(module: CaSourceModule): com.intellij.openapi.module.Module {
        require(module is CaIdeSourceModuleBase) {
            "Expected ${CaIdeSourceModuleBase::class}, but got ${module::class} instead"
        }
        return module.openapiModule
    }

    override fun getCaLibraryModules(library: Library): List<CaLibraryModule> {
        require(library is LibraryBridge) {
            "Expected ${LibraryBridge::class}, but got ${library::class} instead"
        }
        return getCaLibraryModules(library.libraryId)
    }

    override fun getCaLibraryModules(libraryId: LibraryId): List<CaLibraryModule> {
        val libraryEntity = libraryId.resolve(project.workspaceModel.currentSnapshot) ?: return emptyList()
        return getCaLibraryModules(libraryEntity)
    }

    override fun getCaLibraryModules(libraryEntity: LibraryEntity): List<CaLibraryModule> {
        return listOf(state.getLibraryBinaryModule(libraryEntity.symbolicId))
    }

    override fun getCaLibraryModuleSymbolicId(libraryModule: CaLibraryModule): LibraryId {
        require(libraryModule is CaIdeLibraryModule) {
            "Expected ${CaIdeLibraryModule::class}, but got ${libraryModule::class} instead"
        }
        return libraryModule.entityId
    }

    override fun getOpenapiLibrary(module: CaLibraryModule): Library? {
        require(module is CaIdeLibraryModule) {
            "Expected ${CaIdeLibraryModule::class}, but got ${module::class} instead"
        }
        return state.getOpenapiLibrary(module.entityId)
    }

    override fun getAssociatedCaModules(virtualFile: com.intellij.openapi.vfs.VirtualFile): List<CaModule> {
        val builtinsModules = state.getBuiltinsModules()
            .filter { builtinsModule -> virtualFile in builtinsModule.contentScope }
        if (builtinsModules.isNotEmpty()) {
            return builtinsModules
        }
        return when {
            state.isInLibrarySource(virtualFile) -> listOf(state.resolveCandidate(CaModuleCandidate.LibrarySourceFile(virtualFile)))
            state.isInLibraryClasses(virtualFile) -> listOf(state.resolveCandidate(CaModuleCandidate.LibraryBinaryFile(virtualFile)))
            state.getSourceRootForFile(virtualFile) != null && state.isInSource(virtualFile) ->
                listOf(state.resolveCandidate(CaModuleCandidate.SourceRoot(state.getSourceRootForFile(virtualFile)!!)))
            else -> emptyList()
        }
    }

    override fun getCacheDependenciesTracker(): ModificationTracker = projectStructureModificationTracker

    override fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule =
        CaNotUnderContentRootModuleFactory.EP_NAME.extensionList.firstNotNullOfOrNull { factory ->
            factory.create(project, null)
        } ?: state.getNotUnderContentRootModule(project)

    private fun getModuleEntity(openapiModule: com.intellij.openapi.module.Module): ModuleEntity? {
        require(openapiModule is ModuleBridge) {
            "Expected ${ModuleBridge::class}, but got ${openapiModule::class} instead"
        }
        return openapiModule.findModuleEntity(project.workspaceModel.currentSnapshot)
    }

    /**
     * 对位 Kotlin `cachedKaModule(...)`：
     * 以 PSI 锚点缓存 use-site module 选择结果，避免 IDE 高频查询重复重跑候选收集与优先级选择。
     */
    private fun cachedCaModule(
        anchorElement: PsiFileSystemItem,
        useSiteModule: CaModule?,
    ): CaModule {
        val contextToModule = CachedValuesManager.getCachedValue(anchorElement) {
            CachedValueProvider.Result.create(
                ConcurrentFactoryMap.createMap<CaModule?, CaModule> { contextModule ->
                    computeCaModule(anchorElement, contextModule)
                },
                ProjectRootModificationTracker.getInstance(project),
                projectStructureModificationTracker,
            )
        }
        return contextToModule[useSiteModule]
            ?: error("无法为 `${anchorElement.name}` 解析 Analysis API use-site module。")
    }
}
