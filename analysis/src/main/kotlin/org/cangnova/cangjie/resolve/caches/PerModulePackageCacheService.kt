/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.caches

import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPackageDirective
import org.cangnova.cangjie.psi.NotNullableUserDataProperty
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.DumbModeAccessType
import org.cangnova.cangjie.descriptors.AnalysisContextProvider
import org.cangnova.cangjie.descriptors.analysisContext
import org.cangnova.cangjie.descriptors.analysisContextProvider
import org.cangnova.cangjie.resolve.caches.PerModulePackageCacheService.Companion.DEBUG_LOG_ENABLE_PerModulePackageCache
import org.cangnova.cangjie.stubindex.CangJiePackageIndexUtils
import org.cangnova.cangjie.utils.isCangJieFileType
import org.cangnova.cangjie.utils.isUnitTestMode

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

class CangJiePackageStatementPsiTreeChangePreprocessor(private val project: Project) : PsiTreeChangePreprocessor {
    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        // skip events out of scope of this processor
        when (event.code) {
            PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED,
            PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED -> Unit

            else -> return
        }

        val eFile = event.file ?: event.child ?: run {
            LOG.debugIfEnabled(project, true) { "Got PsiEvent: $event without file/child" }
            return
        }

        val file = eFile as? CjFile ?: return

        when (event.code) {
            PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED -> {
                val child = event.child ?: run {
                    LOG.debugIfEnabled(project, true) { "Got PsiEvent: $event without child" }
                    return
                }
                if (child.getParentOfType<CjPackageDirective>(false) != null)
                    PerModulePackageCacheService.getInstance(project).notifyPackageChange(file)
            }

            PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED -> {
                val parent = event.parent ?: run {
                    LOG.debugIfEnabled(project, true) { "Got PsiEvent: $event without parent" }
                    return
                }
                val childrenOfType = parent.getChildrenOfType<CjPackageDirective>()
                if (
                    (!event.isGenericChange && (childrenOfType.any() || parent is CjPackageDirective)) ||
                    (childrenOfType.any { it.name.isEmpty() } && parent is CjFile)
                ) {
                    PerModulePackageCacheService.getInstance(project).notifyPackageChange(file)
                }
            }

            else -> error("unsupported event code ${event.code} for PsiEvent $event")
        }
    }

    companion object {
        val LOG = Logger.getInstance(this::class.java)
    }
}

@Service(Service.Level.PROJECT)

class PerModulePackageCacheService(private val project: Project) : Disposable {
    companion object {
        const val FULL_DROP_THRESHOLD = 1000
        private val LOG = Logger.getInstance(this::class.java)

        fun getInstance(project: Project): PerModulePackageCacheService = project.service()

        var Project.DEBUG_LOG_ENABLE_PerModulePackageCache: Boolean
                by NotNullableUserDataProperty<Project, Boolean>(Key.create("debug.PerModulePackageCache"), false)
    }

    private val pendingVFileChanges: MutableSet<VFileEvent> = mutableSetOf()
    private val pendingCjFileChanges: MutableSet<CjFile> = mutableSetOf()
    private val cacheInstance =
        AtomicReference<ConcurrentMap<Module, ConcurrentMap<AnalysisContext, ConcurrentMap<FqName, Boolean>>>>()
    private val useStrongMapForCaching = Registry.`is`("cangjie.cache.packages.strong.map", false)
    private val implicitPackagePrefixCache = ImplicitPackagePrefixCache(project)

    override fun dispose() {
        clear()

    }

    internal fun onTooComplexChange() {
        clear()
    }

    private fun cache(): ConcurrentMap<Module, ConcurrentMap<AnalysisContext, ConcurrentMap<FqName, Boolean>>> {
        cacheInstance.get()?.let { return it }
        val map =
            ContainerUtil.createConcurrentWeakMap<Module, ConcurrentMap<AnalysisContext, ConcurrentMap<FqName, Boolean>>>()
        return if (cacheInstance.compareAndSet(null, map)) {
            map
        } else {
            cacheInstance.get()!!
        }
    }

    private inline fun <T> MutableCollection<T>.processPending(crossinline body: (T) -> Unit) {
        this.removeIf { value ->
            try {
                body(value)
            } catch (pce: ProcessCanceledException) {
                throw pce
            } catch (exc: Exception) {
                // Log and proceed. Otherwise pending object processing won't be cleared and exception will be thrown forever.
                LOG.error(exc)
            }

            return@removeIf true
        }
    }

    private val projectScope = GlobalSearchScope.projectScope(project)

    private fun VirtualFile.containedInOrContains(root: String) =
        (VfsUtilCore.isEqualOrAncestor(url, root)
                || isDirectory && VfsUtilCore.isEqualOrAncestor(root, url))

    private fun checkPendingChanges() = synchronized(this) {
        if (pendingVFileChanges.size + pendingCjFileChanges.size >= FULL_DROP_THRESHOLD) {
            onTooComplexChange()
        } else {
            pendingVFileChanges.processPending { event ->
                val vfile = event.file ?: return@processPending
                // When VirtualFile !isValid (deleted for example), it impossible to use getModuleInfoByVirtualFile
                // For directory we must check both is it in some sourceRoot, and is it contains some sourceRoot
                if (vfile.isDirectory || !vfile.isValid) {
                    cacheInstance.get()?.let { cache ->
                        for ((module, data) in cache) {
                            val sourceRootUrls = module.rootManager.sourceRootUrls
                            if (sourceRootUrls.any { url ->
                                    vfile.containedInOrContains(url)
                                }) {
                                LOG.debugIfEnabled(project) { "Invalidated cache for $module" }
                                data.clear()
                            }
                        }
                    }
                } else {
                    val provider = AnalysisContextProvider.getInstance(project)
                    val infoByVirtualFile = provider.getContextForFile(project, vfile)
                    if (infoByVirtualFile == null) {
                        LOG.debugIfEnabled(project) { "Skip $vfile as it has no AnalysisContext" }
                    }
                    if (infoByVirtualFile != null && infoByVirtualFile.isSourceContext) {
                        invalidateCacheForAnalysisContext(infoByVirtualFile)
                    }
                }

                implicitPackagePrefixCache.update(event)
            }

            pendingCjFileChanges.processPending { file ->
                if (file.virtualFile != null && file.virtualFile !in projectScope) {
                    LOG.debugIfEnabled(project) {
                        "Skip $file without vFile, or not in scope: ${file.virtualFile?.let { it !in projectScope }}"
                    }
                    return@processPending
                }
                val analysisContext = file.analysisContext
                if (  analysisContext.isSourceContext) {
                    invalidateCacheForAnalysisContext(analysisContext)
                } /*else if (analysisContext == null) {
                    LOG.debugIfEnabled(project) { "Skip $file as it has no AnalysisContext" }
                }*/
                implicitPackagePrefixCache.update(file)
            }
        }
    }

    private fun invalidateCacheForAnalysisContext(context: AnalysisContext) {
        LOG.debugIfEnabled(project) { "Invalidated cache for $context" }
        val cache = cacheInstance.get() ?: return
        // 遍历所有模块的缓存并清除该上下文相关的条目
        for ((_, perContextData) in cache) {
            val dataForContext = perContextData[context] ?: continue
            dataForContext.clear()
        }
    }

    fun packageExists(packageFqName: FqName, context: AnalysisContext): Boolean {
        if (!context.isSourceContext) {
            // 非源码上下文不使用缓存
            return CangJiePackageIndexUtils.packageExists(packageFqName, context.scope)
        }

        checkPendingChanges()

        // 尝试从上下文中获取模块信息
        val module = context.scope.project?.let { proj ->
            proj.modules.firstOrNull { mod ->
                // 简单的模块匹配逻辑
                context.scope.isSearchInModuleContent(mod)
            }
        }

        if (module == null) {
            // 无法确定模块，直接查询
            return try {
                CangJiePackageIndexUtils.packageExists(packageFqName, context.scope)
            } catch (e: IndexNotReadyException) {
                DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
                    CangJiePackageIndexUtils.packageExists(packageFqName, context.scope)
                })
            }
        }

        val perContextCache = cache().getOrPut(module) {
            if (useStrongMapForCaching) ConcurrentHashMap() else CollectionFactory.createConcurrentSoftMap()
        }
        val cacheForCurrentContext = perContextCache.getOrPut(context) {
            if (useStrongMapForCaching) ConcurrentHashMap() else CollectionFactory.createConcurrentSoftMap()
        }
        return try {
            cacheForCurrentContext.getOrPut(packageFqName) {
                val packageExists = CangJiePackageIndexUtils.packageExists(packageFqName, context.scope)
                LOG.debugIfEnabled(project) { "Computed cache value for $packageFqName in $context is $packageExists" }
                packageExists
            }
        } catch (e: IndexNotReadyException) {
            DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
                CangJiePackageIndexUtils.packageExists(packageFqName, context.scope)
            })
        }
    }

    fun getImplicitPackagePrefix(sourceRoot: VirtualFile): FqName {
        checkPendingChanges()
        return implicitPackagePrefixCache.getPrefix(sourceRoot)
    }


    private fun clear() {
        synchronized(this) {
            pendingVFileChanges.clear()
            pendingCjFileChanges.clear()
            cacheInstance.set(null)
            implicitPackagePrefixCache.clear()
        }
    }


    class PackageCacheBulkFileListener(private val project: Project) : BulkFileListener {
        override fun before(events: List<VFileEvent>) = onEvents(events, false)
        override fun after(events: List<VFileEvent>) = onEvents(events, true)

        private fun isRelevant(event: VFileEvent): Boolean = when (event) {
            is VFilePropertyChangeEvent -> false
            is VFileCreateEvent -> true
            is VFileMoveEvent -> true
            is VFileDeleteEvent -> true
            is VFileContentChangeEvent -> true
            is VFileCopyEvent -> true
            else -> {
                LOG.warn("Unknown vfs event: ${event.javaClass}")
                false
            }
        }

        private fun onEvents(events: List<VFileEvent>, isAfter: Boolean) {
            val service = getInstance(project)
            val fileManager = PsiManagerEx.getInstanceEx(project).fileManager
            if (events.size >= FULL_DROP_THRESHOLD) {
                service.onTooComplexChange()
            } else {
                events.asSequence()
                    .filter(::isRelevant)
                    .filter {
                        (it.isValid || it !is VFileCreateEvent) && it.file != null
                    }
                    .filter {
                        val vFile = it.file!!
                        vFile.isDirectory || vFile.isCangJieFileType()
                    }
                    .filter {
                        // It expected that content change events will be duplicated with more precise PSI events and processed
                        // in CangJiePackageStatementPsiTreeChangePreprocessor, but events might have been missing if PSI view provider
                        // is absent.
                        if (it is VFileContentChangeEvent) {
                            isAfter && fileManager.findCachedViewProvider(it.file) == null
                        } else {
                            true
                        }
                    }
                    .filter {
                        when (val origin = it.requestor) {
                            is Project -> origin == project
                            is PsiManager -> origin.project == project
                            else -> true
                        }
                    }
                    .forEach { event -> service.notifyPackageChange(event) }
            }
        }
    }

    class PackageCacheModuleRootListener(private val project: Project) : ModuleRootListener {
        override fun rootsChanged(event: ModuleRootEvent) {
            getInstance(project).onTooComplexChange()
        }
    }

    internal fun notifyPackageChange(file: VFileEvent): Unit = synchronized(this) {
        pendingVFileChanges += file
    }

    internal fun notifyPackageChange(file: CjFile): Unit = synchronized(this) {
        pendingCjFileChanges += file
    }


}

private fun Logger.debugIfEnabled(project: Project, withCurrentTrace: Boolean = false, message: () -> String) {
    if (isUnitTestMode && project.DEBUG_LOG_ENABLE_PerModulePackageCache) {
        val msg = message()
        if (withCurrentTrace) {
            val e = Exception().apply { fillInStackTrace() }
            this.debug(msg, e)
        } else {
            this.debug(msg)
        }
    }
}
private typealias ImplicitPackageData = MutableMap<FqName, MutableList<VirtualFile>>

class ImplicitPackagePrefixCache(private val project: Project) {
    private val implicitPackageCache = ConcurrentHashMap<VirtualFile, ImplicitPackageData>()

    fun getPrefix(sourceRoot: VirtualFile): FqName {
        val implicitPackageMap =
            implicitPackageCache.getOrPut(sourceRoot) { analyzeImplicitPackagePrefixes(sourceRoot) }
        return implicitPackageMap.keys.singleOrNull() ?: FqName.ROOT
    }

    internal fun clear() {
        implicitPackageCache.clear()
    }

    private fun analyzeImplicitPackagePrefixes(sourceRoot: VirtualFile): MutableMap<FqName, MutableList<VirtualFile>> {
        val result = mutableMapOf<FqName, MutableList<VirtualFile>>()
        val cjFiles = sourceRoot.children.filter(VirtualFile::isCangJieFileType)
        for (cjFile in cjFiles) {
            result.addFile(cjFile)
        }
        return result
    }

    private fun ImplicitPackageData.addFile(cjFile: VirtualFile) {
        synchronized(this) {
            val psiFile = PsiManager.getInstance(project).findFile(cjFile) as? CjFile ?: return
            addPsiFile(psiFile, cjFile)
        }
    }

    private fun ImplicitPackageData.addPsiFile(
        psiFile: CjFile,
        cjFile: VirtualFile
    ) = getOrPut(psiFile.packageFqName) { mutableListOf() }.add(cjFile)

    private fun ImplicitPackageData.removeFile(file: VirtualFile) {
        synchronized(this) {
            for ((key, value) in this) {
                if (value.remove(file)) {
                    if (value.isEmpty()) remove(key)
                    break
                }
            }
        }
    }

    private fun ImplicitPackageData.updateFile(file: CjFile) {
        synchronized(this) {
            removeFile(file.virtualFile)
            addPsiFile(file, file.virtualFile)
        }
    }

    internal fun update(event: VFileEvent) {
        when (event) {
            is VFileCreateEvent -> checkNewFileInSourceRoot(event.file)
            is VFileDeleteEvent -> checkDeletedFileInSourceRoot(event.file)
            is VFileCopyEvent -> {
                val newParent = event.newParent
                if (newParent.isValid) {
                    checkNewFileInSourceRoot(newParent.findChild(event.newChildName))
                }
            }

            is VFileMoveEvent -> {
                checkNewFileInSourceRoot(event.file)
                if (event.oldParent.getSourceRoot(project) == event.oldParent) {
                    implicitPackageCache[event.oldParent]?.removeFile(event.file)
                }
            }
        }
    }

    private fun checkNewFileInSourceRoot(file: VirtualFile?) {
        if (file == null) return
        if (file.getSourceRoot(project) == file.parent) {
            implicitPackageCache[file.parent]?.addFile(file)
        }
    }

    private fun checkDeletedFileInSourceRoot(file: VirtualFile?) {
        val directory = file?.parent
        if (directory == null || !directory.isValid) return
        if (directory.getSourceRoot(project) == directory) {
            implicitPackageCache[directory]?.removeFile(file)
        }
    }

    internal fun update(cjFile: CjFile) {
        val parent = cjFile.virtualFile?.parent ?: return
        if (cjFile.sourceRoot == parent) {
            implicitPackageCache[parent]?.updateFile(cjFile)
        }
    }
}

fun VirtualFile.getSourceRoot(project: Project): VirtualFile? =
    ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(this)

val PsiFileSystemItem.sourceRoot: VirtualFile?
    get() = virtualFile?.getSourceRoot(project)
