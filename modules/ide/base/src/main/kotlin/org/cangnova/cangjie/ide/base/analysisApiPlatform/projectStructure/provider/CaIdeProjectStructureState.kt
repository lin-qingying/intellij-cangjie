@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.InheritedSdkDependency
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.workspaceModel.ide.impl.legacyBridge.library.LibraryBridge
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBaseResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaDanglingFileModuleImpl
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.config.CangJieSourceRootTypes
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeBuiltinsModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeLibraryFallbackDependenciesModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeMutableModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeNotUnderContentRootModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library.CaIdeLibraryModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.librarySource.CaIdeLibrarySourceModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source.CaIdeSourceModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source.CaIdeSourceModuleBase
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * IDE 平台的统一项目结构状态源。
 *
 * 这里不再把 IntelliJ 项目压扁成“source root + library”的简化模型，而是显式维护
 * Analysis API 需要的完整模块族：
 * 1. 项目源码模块
 * 2. 库二进制模块
 * 3. 库源码模块
 * 4. 游离文件模块
 * 5. 不在内容根下的临时模块
 * 6. fallback / builtins 这类依赖边界模块
 *
 * 这样 low-level CFIR、session cache、权限服务和平台失效策略才能围绕同一张模块图工作，
 * 不再在各层重复推导 use-site module 和依赖边界。
 */
class CaIdeProjectStructureState(
    private val project: Project,
) {
    private val psiManager = PsiManager.getInstance(project)
    private val projectFileIndex = ProjectFileIndex.getInstance(project)
    private val projectRootManager = ProjectRootManager.getInstance(project)
    private val cache = project.getService(CaIdeProjectStructureProviderCache::class.java)

    private val explicitGlobalInvalidationCount = AtomicLong(0)
    private val explicitModuleInvalidationCounts = ConcurrentHashMap<CaModule, AtomicLong>()
    private val danglingFileModulesByPath = ConcurrentHashMap<String, CaDanglingFileModuleImpl>()
    private val outsideContentModulesByPath = ConcurrentHashMap<String, CaIdeNotUnderContentRootModule>()
    @Volatile
    private var cachedSnapshotState: CachedSnapshotState? = null
    private val resolutionScopeProvider = CaBaseResolutionScopeProvider()

    val allModules: List<CaModule>
        get() = snapshot.allModules

    val resolvableModules: List<CaModule>
        get() = snapshot.allResolvableModules

    val sourceLikeModules: List<CaModule>
        get() = snapshot.allSourceLikeModules

    val allSourceFiles: List<PsiFileSystemItem>
        get() = snapshot.allSourceFiles

    val snapshot: CaProjectStructureSnapshot
        get() = getOrBuildSnapshot()

    val modificationCount: Long
        get() = PsiModificationTracker.getInstance(project).modificationCount + explicitGlobalInvalidationCount.get()

    fun getImplementingModules(module: CaModule): List<CaModule> {
        return snapshot.allModules.filter { module in it.directDependsOnDependencies }
    }

    fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule {
        return outsideContentModulesByPath.values.firstOrNull() ?: emptyOutsideContentRootModule(project)
    }

    internal fun resolveCandidate(candidate: CaModuleCandidate): CaModule = when (candidate) {
        is CaModuleCandidate.FixedModule -> candidate.module
        is CaModuleCandidate.SourceRoot -> sourceModuleFor(candidate.root)
        is CaModuleCandidate.LibraryBinaryFile -> libraryBinaryModuleFor(candidate.file)
        is CaModuleCandidate.LibrarySourceFile -> librarySourceModuleFor(candidate.file)
        is CaModuleCandidate.NotUnderContentRoot -> outsideContentModuleFor(candidate.item)
    }

    internal fun getBuiltinsModule(targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform): CaIdeBuiltinsModule =
        cache.getBuiltinsModule(targetPlatform)

    internal fun getBuiltinsModules(): List<CaIdeBuiltinsModule> =
        snapshot.allModules.filterIsInstance<CaIdeBuiltinsModule>()

    internal fun getLibraryBinaryModule(libraryId: LibraryId): CaIdeLibraryModule {
        return getLibraryBinaryModuleOrNull(libraryId)
            ?: error("无法按 workspace library id `${libraryId.presentableName}` 恢复 LibraryEntity。")
    }

    private fun getLibraryBinaryModuleOrNull(libraryId: LibraryId): CaIdeLibraryModule? {
        val snapshot = project.workspaceModel.currentSnapshot
        val libraryEntity = libraryId.resolve(snapshot)
        if (libraryEntity == null) {
            cache.removeLibraryBinaryModule(libraryId)
            return null
        }

        val libraryModule = cache.cachedLibraryBinaryModule(libraryId) {
            val binaryRoots = libraryEntity.roots
                .filter { root -> root.type == LibraryRootTypeId.COMPILED }
                .mapNotNull { root -> root.url.virtualFile }
                .mapNotNull(::toPsiFileSystemItem)
            check(binaryRoots.isNotEmpty()) {
                "库 `${libraryEntity.name}` 没有可用的二进制根，无法构建 IDE 库模块。"
            }
            CaIdeLibraryModule(
                project = project,
                entityId = libraryId,
                libraryName = libraryEntity.name,
                binaryRoots = binaryRoots,
            )
        }
        refreshLibraryModuleDependencies(libraryModule, currentSnapshotStamp())
        return libraryModule
    }

    internal fun getOpenapiLibrary(libraryId: LibraryId): Library? {
        val snapshot = project.workspaceModel.currentSnapshot
        val libraryEntity = libraryId.resolve(snapshot) ?: return null
        return libraryEntity.findOpenapiLibraryBridge(snapshot, project)
    }

    internal fun isInLibrarySource(file: VirtualFile): Boolean = projectFileIndex.isInLibrarySource(file)

    internal fun isInLibraryClasses(file: VirtualFile): Boolean = projectFileIndex.isInLibraryClasses(file)

    internal fun getSourceRootForFile(file: VirtualFile): VirtualFile? = projectFileIndex.getSourceRootForFile(file)

    internal fun isInSource(file: VirtualFile): Boolean = projectFileIndex.isInSource(file)

    internal fun getSourceModule(moduleId: ModuleId, kind: CaSourceModuleKind): CaIdeSourceModule? {
        val hasRoots = projectRootManager.contentSourceRoots.any { root ->
            val metadata = resolveSourceRootMetadata(root)
            metadata.moduleId == moduleId && metadata.kind == kind
        }
        if (!hasRoots) {
            return null
        }

        return sourceModuleFor(
            SourceRootMetadata(
                moduleId = moduleId,
                kind = kind,
            ),
        )
    }

    private fun chooseModuleForItem(item: PsiFileSystemItem): CaModule {
        val candidates = CaIdeCandidateCollector.collectCandidates(item, this)
            .map(::resolveCandidate)
        return CaModuleChooser.chooseModule(candidates, useSiteModule = null)
            ?: getNotUnderContentRootModule(project)
    }

    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    fun getModuleModificationCount(module: CaModule): Long {
        return explicitModuleInvalidationCounts[module]?.get() ?: modificationCount
    }

    fun getResolutionScope(module: CaModule): CaResolutionScope {
        return resolutionScopeProvider.getResolutionScope(module)
    }

    fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        explicitGlobalInvalidationCount.incrementAndGet()
        modules.forEach { module ->
            explicitModuleInvalidationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }

        val delegatedInvalidationService =
            project.getService(CaSessionProvider::class.java) as? org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
        if (delegatedInvalidationService != null) {
            delegatedInvalidationService.invalidate(modules)
        }
    }

    /**
     * 仅提升全局 project-structure 修改计数。
     *
     * 对位 Kotlin `ProjectStructureProviderService.incOutOfBlockModificationCount()` 所需的全局失效钩子。
     * 这里不伪造模块集合，避免把“全局缓存失效”偷换成错误的模块依赖语义。
     */
    fun invalidateGlobal() {
        explicitGlobalInvalidationCount.incrementAndGet()
    }

    /**
     * IDE 平台统一构造当前模块图快照。
     *
     * 这里显式把“模块图枚举”“源码视图枚举”“可解析模块过滤”绑定到同一轮结构收集，
     * 避免不同属性访问时各自重建出不一致的中间结果。
     */
    private fun buildSnapshot(): CaProjectStructureSnapshot {
        cache.removeInvalidEntries(project.workspaceModel.currentSnapshot)

        val snapshotStamp = currentSnapshotStamp()
        val sourceEntries = sourceModuleEntries()
        sourceEntries.forEach { entry ->
            refreshRegularDependencies(entry.module, entry.roots.map(PsiFileSystemItem::getVirtualFile), snapshotStamp)
        }

        val danglingModules = danglingFileModulesByPath.values
            .filter(CaDanglingFileModuleImpl::isValid)
            .sortedBy(CaModule::moduleDescription)

        val builtinsModules = activeTargetPlatforms(
            sourceEntries = sourceEntries,
            danglingModules = danglingModules,
        ).map(::getBuiltinsModule)
            .sortedBy(CaModule::moduleDescription)

        val allModules = buildList {
            addAll(builtinsModules)
            addAll(sourceEntries.map(SourceModuleEntry::module))
            addAll(cache.librarySourceModules().sortedBy(CaModule::moduleDescription))
            addAll(cache.libraryModules().sortedBy(CaModule::moduleDescription))
            addAll(cache.fallbackDependencyModules().sortedBy(CaModule::moduleDescription))
            addAll(danglingModules)
            addAll(outsideContentModulesByPath.values.sortedBy(CaModule::moduleDescription))
        }.distinct()

        val allSourceFiles = buildList {
            sourceEntries.flatMapTo(this, SourceModuleEntry::roots)
            cache.librarySourceModules().flatMapTo(this) { it.sourceRoots }
            danglingModules.flatMapTo(this, CaDanglingFileModuleImpl::files)
            outsideContentModulesByPath.values.mapTo(this, CaIdeNotUnderContentRootModule::item)
        }.distinctBy { item ->
            item.virtualFile?.url ?: item.name
        }

        return CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allModules.filter(CaModule::isResolvable),
            allSourceLikeModules = allModules.filterIsInstance<CaSourceModule>(),
            allSourceFiles = allSourceFiles,
        )
    }

    /**
     * IDE 平台的模块图快照需要绑定到统一的结构时间戳。
     *
     * 这里显式用“PSI 修改 + 显式 invalidation”组成结构戳，
     * 保证 session provider、模块查询和低层 resolve 至少围绕同一轮 Analysis API 结构视图工作。
     */
    private fun getOrBuildSnapshot(): CaProjectStructureSnapshot {
        val currentStamp = currentSnapshotStamp()
        val cached = cachedSnapshotState
        if (cached?.stamp == currentStamp) {
            return cached.snapshot
        }

        synchronized(this) {
            val synchronizedCached = cachedSnapshotState
            if (synchronizedCached?.stamp == currentStamp) {
                return synchronizedCached.snapshot
            }

            return buildSnapshot().also { snapshot ->
                cachedSnapshotState = CachedSnapshotState(currentStamp, snapshot)
            }
        }
    }

    private fun currentSnapshotStamp(): SnapshotStamp {
        return SnapshotStamp(
            psiModificationCount = PsiModificationTracker.getInstance(project).modificationCount,
            explicitInvalidationCount = explicitGlobalInvalidationCount.get(),
        )
    }

    private fun sourceModuleEntries(): List<SourceModuleEntry> {
        val groupedRoots = linkedMapOf<SourceModuleCacheKey, MutableList<PsiFileSystemItem>>()
        val sourceRootMetadataByKey = linkedMapOf<SourceModuleCacheKey, SourceRootMetadata>()

        for (root in projectRootManager.contentSourceRoots) {
            val psiRoot = toPsiFileSystemItem(root) ?: continue
            val metadata = resolveSourceRootMetadata(root)
            val key = SourceModuleCacheKey(
                moduleId = metadata.moduleId,
                kind = metadata.kind,
            )
            groupedRoots.getOrPut(key) { mutableListOf() }.add(psiRoot)
            sourceRootMetadataByKey.putIfAbsent(key, metadata)
        }

        return groupedRoots.entries.map { (key, roots) ->
            val metadata = sourceRootMetadataByKey.getValue(key)
            val module = sourceModuleFor(metadata)
            SourceModuleEntry(
                roots = roots.sortedBy { it.virtualFile.path },
                module = module,
            )
        }.sortedBy { entry ->
            buildString {
                append(entry.module.openapiModule.name)
                append(':')
                append(if (entry.module.kind == CaSourceModuleKind.TEST) "test" else "production")
            }
        }
    }

    private fun sourceModuleFor(sourceRootMetadata: SourceRootMetadata): CaIdeSourceModule {
        return cache.cachedSourceModule(sourceRootMetadata.moduleId, sourceRootMetadata.kind) {
            CaIdeSourceModule(
                project = project,
                entityId = sourceRootMetadata.moduleId,
                kind = sourceRootMetadata.kind,
            )
        }
    }

    private fun sourceModuleFor(root: VirtualFile): CaIdeSourceModule {
        val sourceRootMetadata = resolveSourceRootMetadata(root)
        val groupedRoots = projectRootManager.contentSourceRoots
            .filter { candidate ->
                val candidateMetadata = resolveSourceRootMetadata(candidate)
                candidateMetadata.moduleId == sourceRootMetadata.moduleId &&
                    candidateMetadata.kind == sourceRootMetadata.kind
            }
            .mapNotNull(::toPsiFileSystemItem)
        return sourceModuleFor(sourceRootMetadata)
            .also { module ->
                refreshRegularDependencies(module, groupedRoots.map(PsiFileSystemItem::getVirtualFile), currentSnapshotStamp())
            }
    }

    private fun resolveSourceRootMetadata(root: VirtualFile): SourceRootMetadata {
        val module = projectFileIndex.getModuleForFile(root)
            ?: error("无法为 source root `${root.path}` 定位 IntelliJ module。")

        val sourceRootTypeId = ModuleRootManager.getInstance(module)
            .contentEntries
            .asSequence()
            .flatMap { it.sourceFolders.asSequence() }
            .firstOrNull { it.file?.url == root.url }
            ?.rootType
            ?.let(CangJieSourceRootTypes::findIdByType)

        return SourceRootMetadata(
            moduleId = moduleEntityId(module),
            kind = sourceModuleKindForRootType(sourceRootTypeId),
        )
    }

    private fun libraryBinaryModuleFor(file: VirtualFile): CaIdeLibraryModule {
        val libraryId = resolveLibraryId(file)
        return getLibraryBinaryModule(libraryId)
    }

    private fun librarySourceModuleFor(file: VirtualFile): CaIdeLibrarySourceModule {
        val libraryId = resolveLibraryId(file)
        val binaryModule = libraryBinaryModuleFor(file)
        return cache.cachedLibrarySourceModule(LibrarySourceKey(libraryId)) {
            val sourceModule = CaIdeLibrarySourceModule(binaryLibraryModule = binaryModule)
            check(sourceModule.sourceRoots.isNotEmpty()) {
                "库 `${binaryModule.libraryName}` 没有可用的源码根，无法构建 IDE 库源码模块。"
            }
            sourceModule
        }
    }

    private fun danglingFileModuleFor(file: CjFile): CaDanglingFileModuleImpl {
        val virtualFile = file.viewProvider.virtualFile
        val pathKey = virtualFile.path.ifBlank { virtualFile.url }
        val contextModule = danglingFileContextModuleFor(file)
        return danglingFileModulesByPath.compute(pathKey) { _, existingModule ->
            if (
                existingModule != null &&
                existingModule.isValid &&
                existingModule.contextModule == contextModule &&
                existingModule.resolutionMode == CaDanglingFileResolutionMode.PREFER_SELF &&
                existingModule.files.singleOrNull() == file
            ) {
                existingModule
            } else {
                CaDanglingFileModuleImpl(
                    files = listOf(file),
                    contextModule = contextModule,
                    resolutionMode = CaDanglingFileResolutionMode.PREFER_SELF,
                )
            }
        } ?: error("无法为 `${file.name}` 构建 dangling file module。")
    }

    private fun outsideContentModuleFor(item: PsiFileSystemItem): CaIdeNotUnderContentRootModule {
        val virtualFile = item.virtualFile
            ?: error("无法为不在 content root 下的 PSI 构建模块：缺少 VirtualFile。")
        return outsideContentModulesByPath.computeIfAbsent(virtualFile.path) {
            CaIdeNotUnderContentRootModule(
                project = project,
                item = item,
                pathKey = virtualFile.path,
            )
        }.also { module ->
            refreshOutsideContentDependencies(module, virtualFile, currentSnapshotStamp())
        }
    }

    /**
     * 从 IntelliJ order entry 恢复 Analysis API 的直接依赖边界。
     *
     * 这里不把依赖简单压成一组 VirtualFile，而是显式恢复：
     * - 项目 source module
     * - library source module
     * - library binary module
     *
     * 脚本依赖模块和 fallback 模块会在更外层再包装一次，避免上层误把它们当成源码 use-site 模块。
     */
    private fun regularDependenciesFor(owner: CaModule, anchorFiles: List<VirtualFile>): List<CaModule> {
        if (owner is CaIdeSourceModuleBase) {
            return regularDependenciesForSourceModule(owner)
        }

        val dependencies = linkedSetOf<CaModule>()
        for (anchorFile in anchorFiles) {
            for (orderEntry in projectFileIndex.getOrderEntriesForFile(anchorFile)) {
                val sourceRoots = orderEntry.getFiles(OrderRootType.SOURCES).toList()
                val classRoots = orderEntry.getFiles(OrderRootType.CLASSES).toList()

                sourceRoots.forEach { root ->
                    when {
                        projectFileIndex.isInLibrarySource(root) -> dependencies += librarySourceModuleFor(root)
                        projectFileIndex.getSourceRootForFile(root) != null -> dependencies += sourceModuleFor(root)
                    }
                }

                classRoots.forEach { root ->
                    dependencies += libraryBinaryModuleFor(root)
                }
            }
        }

        dependencies -= owner
        dependencies -= getBuiltinsModule(owner.targetPlatform)
        return dependencies.toList()
    }

    /**
     * 对位 Kotlin `KaSourceModuleDependenciesProvider.getDirectRegularDependencies(...)`。
     *
     * 项目源码模块的直接 regular 依赖应从 workspace `ModuleEntity.dependencies` 恢复，
     * 而不是重新扫描 order-entry roots。这样依赖边界才与 IntelliJ workspace model 的声明保持一致。
     */
    private fun regularDependenciesForSourceModule(owner: CaIdeSourceModuleBase): List<CaModule> {
        val moduleEntity = owner.entityId.resolve(project.workspaceModel.currentSnapshot)
            ?: error("无法按 workspace module id `${owner.entityId.name}` 恢复 ModuleEntity。")

        return buildSet {
            for (dependency in moduleEntity.dependencies) {
                dependency.collectRegularDependencies(owner.kind, this)
            }
            remove(owner)
            remove(getBuiltinsModule(owner.targetPlatform))
        }.toList()
    }

    private fun ModuleDependencyItem.collectRegularDependencies(
        ownerKind: CaSourceModuleKind,
        sink: MutableSet<CaModule>,
    ) {
        when (this) {
            is ModuleDependency -> collectModuleDependencies(ownerKind, sink)
            is LibraryDependency -> {
                when (scope) {
                    DependencyScope.COMPILE, DependencyScope.PROVIDED -> {
                        getLibraryBinaryModuleOrNull(library)?.let(sink::add)
                    }

                    DependencyScope.TEST -> {
                        if (ownerKind == CaSourceModuleKind.TEST) {
                            getLibraryBinaryModuleOrNull(library)?.let(sink::add)
                        }
                    }

                    DependencyScope.RUNTIME -> Unit
                }
            }

            is InheritedSdkDependency, is SdkDependency, is ModuleSourceDependency -> Unit
        }
    }

    private fun ModuleDependency.collectModuleDependencies(
        ownerKind: CaSourceModuleKind,
        sink: MutableSet<CaModule>,
    ) {
        when (scope) {
            DependencyScope.COMPILE, DependencyScope.PROVIDED -> {
                getSourceModule(module, CaSourceModuleKind.PRODUCTION)?.let(sink::add)

                val dependsOnTest = when (ownerKind) {
                    CaSourceModuleKind.PRODUCTION -> productionOnTest
                    CaSourceModuleKind.TEST -> true
                }
                if (dependsOnTest) {
                    getSourceModule(module, CaSourceModuleKind.TEST)?.let(sink::add)
                }
            }

            DependencyScope.TEST -> {
                if (ownerKind == CaSourceModuleKind.TEST) {
                    getSourceModule(module, CaSourceModuleKind.PRODUCTION)?.let(sink::add)
                    getSourceModule(module, CaSourceModuleKind.TEST)?.let(sink::add)
                }
            }

            DependencyScope.RUNTIME -> Unit
        }
    }

    /**
     * 以结构时间戳驱动依赖刷新，避免同一轮模块装配发生递归重入。
     *
     * source root / library source / fallback module 会在恢复 IntelliJ order entry 时
     * 彼此再次访问模块工厂。如果这里对每次访问都直接 clear + rebuild，遇到
     * `A -> B -> A` 或 “同 root 自回访” 就会无限递归。
     */
    private fun refreshRegularDependencies(
        module: CaIdeMutableModule,
        anchorFiles: List<VirtualFile>,
        snapshotStamp: SnapshotStamp,
    ) {
        if (!module.tryStartDependencyRefresh(snapshotStamp)) {
            return
        }

        try {
            module.directRegularDependencies.clear()
            module.directRegularDependencies += regularDependenciesFor(module, anchorFiles)
            module.finishDependencyRefresh(snapshotStamp)
        } catch (throwable: Throwable) {
            module.resetDependencyRefresh(snapshotStamp)
            throw throwable
        }
    }

    private fun refreshOutsideContentDependencies(
        module: CaIdeNotUnderContentRootModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ) {
        module.directRegularDependencies.clear()
        val fallbackModule = fallbackDependencyModuleFor(module, anchorFile, snapshotStamp)
        if (fallbackModule.directRegularDependencies.isNotEmpty()) {
            module.directRegularDependencies += fallbackModule
        }
    }

    /**
     * 对位 Kotlin `KaLibraryModuleImpl.directRegularDependencies`：
     * 常规库模块需要显式把 fallback dependencies module 作为直接 regular 依赖挂入，
     * 这样库 use-site session 才能沿着统一模块图解析默认可见依赖。
     */
    private fun refreshLibraryModuleDependencies(
        module: CaIdeLibraryModule,
        snapshotStamp: SnapshotStamp,
    ) {
        refreshRegularDependencies(
            module = module,
            anchorFiles = module.binaryRoots.mapNotNull(PsiFileSystemItem::getVirtualFile),
            snapshotStamp = snapshotStamp,
        )
        val fallbackModule = fallbackDependencyModuleFor(
            owner = module,
            anchorFile = module.binaryRoots.firstNotNullOf(PsiFileSystemItem::getVirtualFile),
            snapshotStamp = snapshotStamp,
        )
        if (fallbackModule !in module.directRegularDependencies) {
            module.directRegularDependencies += fallbackModule
        }
    }

    private fun fallbackDependencyModuleFor(
        owner: CaModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ): CaIdeLibraryFallbackDependenciesModule {
        val ownerKey = owner.stableModuleName ?: owner.moduleDescription
        val dependencyOwnerName = when (owner) {
            is CaIdeLibraryModule -> owner.libraryName
            is CaIdeLibrarySourceModule -> owner.libraryName
            else -> owner.moduleDescription
        }
        return cache.cachedFallbackDependencyModule(ownerKey) {
            CaIdeLibraryFallbackDependenciesModule(
                project = project,
                dependencyOwnerName = dependencyOwnerName,
                ownerStableName = ownerKey,
                targetPlatform = owner.targetPlatform,
            )
        }.also { fallbackModule ->
            refreshRegularDependencies(fallbackModule, listOf(anchorFile), snapshotStamp)
        }
    }

    private fun resolveLibraryId(file: VirtualFile): LibraryId {
        val orderEntries = projectFileIndex.getOrderEntriesForFile(file)
        var libraryId: LibraryId? = null

        for (orderEntry in orderEntries) {
            if (libraryId == null) {
                libraryId = ((orderEntry as? LibraryOrderEntry)?.library as? LibraryBridge)?.libraryId
            }
        }

        return libraryId
            ?: error("文件 `${file.path}` 位于库范围内，但无法从 IDE root model 恢复 workspace library id。")
    }

    /**
     * IDE source module 的主身份直接对位 workspace model `ModuleId`，
     * 不能退化成仅使用 module name 的本地拼接键。
     */
    private fun moduleEntityId(module: Module): ModuleId {
        require(module is ModuleBridge) {
            "Expected ${ModuleBridge::class}, but got ${module::class} instead"
        }
        return module.findModuleEntity(project.workspaceModel.currentSnapshot)?.symbolicId
            ?: error("无法为 IntelliJ module `${module.name}` 恢复 workspace module id。")
    }

    private fun isDanglingLikeFile(file: CjFile): Boolean {
        return file.isCodeFragment || file is CjCodeFragment || !file.isPhysical
    }

    /**
     * IDE dangling file 必须显式绑定到一个上下文模块。
     *
     * 代码片段走其 context element，普通非物理文件则回溯 original file。
     * 若两者都不存在，说明平台侧没有给出可分析的宿主模块，直接在 project-structure 层报错。
     */
    private fun danglingFileContextModuleFor(file: CjFile): CaModule {
        (file as? CjCodeFragment)?.context?.let { contextElement ->
            val contextItem = contextElement.containingFile as? PsiFileSystemItem
                ?: error("游离文件 `${file.name}` 的 context element 不位于 PSI 文件中。")
            return chooseModuleForItem(contextItem)
        }

        val originalFile = file.originalFile.takeUnless { it == file } as? CjFile
        if (originalFile != null) {
            return chooseModuleForItem(originalFile)
        }

        error("游离文件 `${file.name}` 缺少 context module，无法构建 Analysis API dangling file module。")
    }

    private fun toPsiFileSystemItem(file: VirtualFile): PsiFileSystemItem? {
        return psiManager.findDirectory(file) ?: psiManager.findFile(file)
    }

    /**
     * IDE project-structure 快照中的 builtins 模块需要按活跃目标平台枚举，而不是永远只有一个全局实例。
     *
     * 当前即使大多数模块仍然落在默认 `cjnative`，这里也提前和 low-level builtins/session
     * 的按平台分桶模型对齐；将来一旦 IDE 侧能把某些模块标成 `cjvm`，快照无需再重构。
     */
    private fun activeTargetPlatforms(
        sourceEntries: List<SourceModuleEntry>,
        danglingModules: List<CaModule>,
    ): Set<TargetPlatform> = buildSet {
        addAll(sourceEntries.map { entry -> entry.module.targetPlatform })
        addAll(cache.libraryModules().map(CaModule::targetPlatform))
        addAll(cache.librarySourceModules().map(CaModule::targetPlatform))
        addAll(cache.fallbackDependencyModules().map(CaModule::targetPlatform))
        addAll(danglingModules.map(CaModule::targetPlatform))
        addAll(outsideContentModulesByPath.values.map(CaModule::targetPlatform))
    }.ifEmpty { setOf(CangJiePlatforms.defaultCangJiePlatform) }

    private data class SourceModuleEntry(
        val roots: List<PsiFileSystemItem>,
        val module: CaIdeSourceModule,
    )

    private data class SourceModuleCacheKey(
        val moduleId: ModuleId,
        val kind: CaSourceModuleKind,
    )

    private data class SourceRootMetadata(
        val moduleId: ModuleId,
        val kind: CaSourceModuleKind,
    )
}

private fun sourceModuleKindForRootType(sourceRootTypeId: String?): CaSourceModuleKind {
    return if (CangJieSourceRootTypes.isTestSource(sourceRootTypeId)) {
        CaSourceModuleKind.TEST
    } else {
        CaSourceModuleKind.PRODUCTION
    }
}

private fun emptyOutsideContentRootModule(project: Project): CaIdeNotUnderContentRootModule {
    val baseDir = project.baseDir
        ?: error("IDE project-structure 无法创建 not-under-content-root module：Project 没有 baseDir。")
    val item = PsiManager.getInstance(project).findDirectory(baseDir)
        ?: error("IDE project-structure 无法创建 not-under-content-root module：baseDir 无法解析为 PSI 目录。")
    return CaIdeNotUnderContentRootModule(
        project = project,
        item = item,
        pathKey = project.basePath ?: baseDir.path,
    )
}

internal data class SnapshotStamp(
    val psiModificationCount: Long,
    val explicitInvalidationCount: Long,
)

private data class CachedSnapshotState(
    val stamp: SnapshotStamp,
    val snapshot: CaProjectStructureSnapshot,
)

/**
 * 当前 IDE 模块类路径没有 Kotlin 插件使用的 `findLibraryBridge(snapshot)` 扩展，
 * 因此这里只在 provider-state 内集中保留等价职责的 bridge 恢复逻辑。
 *
 * 约束仍然和 Kotlin 一样：输入是 workspace `LibraryEntity`，输出是对应的 OpenAPI `LibraryBridge`。
 * 只是 bridge 的定位方式暂时退回到本地可用的 library table / module order-entry 扫描。
 */
private fun com.intellij.platform.workspace.jps.entities.LibraryEntity.findOpenapiLibraryBridge(
    snapshot: com.intellij.platform.workspace.storage.EntityStorage,
    project: Project,
): Library? {
    LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries
        .firstOrNull { library -> (library as? LibraryBridge)?.libraryId == symbolicId }
        ?.let { return it }

    LibraryTablesRegistrar.getInstance().libraryTable.libraries
        .firstOrNull { library -> (library as? LibraryBridge)?.libraryId == symbolicId }
        ?.let { return it }

    for (module in ModuleManager.getInstance(project).modules) {
        var matchedLibrary: Library? = null
        ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library ->
            if ((library as? LibraryBridge)?.libraryId == symbolicId) {
                matchedLibrary = library
                return@forEachLibrary false
            }
            true
        }
        if (matchedLibrary != null) {
            return matchedLibrary
        }
    }

    return null
}
