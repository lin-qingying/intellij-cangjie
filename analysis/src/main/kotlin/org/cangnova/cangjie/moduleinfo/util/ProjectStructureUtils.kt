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

package org.cangnova.cangjie.moduleinfo.util

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.config.CangJieSourceRootTypes
import org.cangnova.cangjie.moduleinfo.BinaryModuleInfo
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleProductionSourceInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfoWithExpectedBy
import org.cangnova.cangjie.moduleinfo.ModuleTestSourceInfo
import org.cangnova.cangjie.moduleinfo.cache.IdeaModelInfosCache
import org.cangnova.cangjie.moduleinfo.sourceModuleInfos
import org.cangnova.cangjie.moduleinfo.testSourceInfo
import org.cangnova.cangjie.projectStructure.CangJieSourceFilterScope
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.UserDataProperty
import org.cangnova.cangjie.utils.safeAs
import java.util.ArrayDeque
import java.util.HashSet


fun PsiElement.isUnderCangJieSourceRootTypes(): Boolean {
    val cjFile = this.containingFile.safeAs<CjFile>() ?: return false
    val file = cjFile.virtualFile?.takeIf { it !is VirtualFileWindow && it.fileSystem !is NonPhysicalFileSystem }
        ?: return false
    val projectFileIndex = ProjectRootManager.getInstance(cjFile.project).fileIndex
    return projectFileIndex.isInTestSourceContent(file)
}

fun Module.asSourceInfo(rootTypeId: String?): ModuleSourceInfoWithExpectedBy? =
    when {
        rootTypeId == CangJieSourceRootTypes.SOURCE -> ModuleProductionSourceInfo(this)
        rootTypeId == CangJieSourceRootTypes.TEST -> ModuleTestSourceInfo(this)
        else -> null
    }

@JvmField
@Deprecated("Use 'customSourceRootTypeId' instead.")
val MODULE_ROOT_TYPE_KEY = getOrCreateKey<String>("Cj_SourceRootType")


@JvmField
@Deprecated("Use 'customLibrary' instead.")
val LIBRARY_KEY = getOrCreateKey<Library>("Cj_Library")

private inline fun <reified T> getOrCreateKey(name: String): Key<T> {
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    val existingKey = Key.findKeyByName(name) as Key<T>?
    return existingKey ?: Key.create(name)
}

/**
 * [customSourceRootTypeId] provides a custom source root type ID for an Android light classes file.
 * It must not be changed after the first assignment because the calculation of the module info
 * cached by [ModuleInfoProvider] might depend on this property.
 */
@Suppress("DEPRECATION")
var UserDataHolder.customSourceRootTypeId: String? by UserDataProperty(MODULE_ROOT_TYPE_KEY)


/**
 * @see customSourceRootTypeId
 */
@Suppress("DEPRECATION")
var UserDataHolder.customLibrary: Library? by UserDataProperty(LIBRARY_KEY)

fun getIdeaModelInfosCache(project: Project): IdeaModelInfosCache = project.service()

fun getModuleInfosFromIdeaModel(project: Project): List<IdeaModuleInfo> {
    return runReadAction {
        val ideaModelInfosCache = getIdeaModelInfosCache(project)

        ideaModelInfosCache.allModules()

    }
}
private interface ModuleIndex {

    fun plainUsages(module: Module): Collection<Module>

    fun exportingUsages(module: Module): Collection<Module>
}

private class ModuleIndexImpl(
    private val plainUsages: MultiMap<Module, Module>,
    private val exportingUsages: MultiMap<Module, Module>,
) : ModuleIndex {
    override fun plainUsages(module: Module): Collection<Module> = plainUsages[module]

    override fun exportingUsages(module: Module): Collection<Module> = exportingUsages[module]
}
private fun getModuleIndex(project: Project): ModuleIndex = CachedValuesManager.getManager(project).getCachedValue(project) {
    val plainUsages: MultiMap<Module, Module> = MultiMap.create()
    val exportingUsages: MultiMap<Module, Module> = MultiMap.create()

    runReadAction {
        for (module in ModuleManager.getInstance(project).modules) {
            ProgressManager.checkCanceled()
            for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
                if (orderEntry !is ModuleOrderEntry) continue
                val referenced = orderEntry.module ?: continue
                val map = if (orderEntry.isExported) exportingUsages else plainUsages
                map.putValue(referenced, module)
            }
        }
    }

    CachedValueProvider.Result(
        ModuleIndexImpl(plainUsages = plainUsages, exportingUsages = exportingUsages),
        ModuleModificationTracker.getInstance(project)
    )
}
/**
 * 模块修改追踪器
 *
 * 监听模块根变化并更新修改计数，用于缓存失效
 */
class ModuleModificationTracker(project: Project) :
    SimpleModificationTracker(), ModuleRootListener, Disposable {

    init {
        project.messageBus.connect(this).subscribe(ModuleRootListener.TOPIC, this)
    }

    override fun beforeRootsChange(event: ModuleRootEvent) {
        // 不需要在变化前处理
    }

    override fun rootsChanged(event: ModuleRootEvent) {
        // 模块根发生变化，增加修改计数以使缓存失效
        incModificationCount()
    }

    override fun dispose() = Unit

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ModuleModificationTracker = project.service()
    }
}
//NOTE: getDependents adapted from com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope#buildDependents()
private fun getDependents(module: Module): Set<Module> {
    val result = HashSet<Module>()
    result.add(module)

    val processedExporting = HashSet<Module>()

    val index = getModuleIndex(module.project)

    val walkingQueue = ArrayDeque<Module>(10)
    walkingQueue.addLast(module)

    while (true) {
        val current = walkingQueue.pollFirst() ?: break
        processedExporting.add(current)
        result.addAll(index.plainUsages(current))
        for (dependent in index.exportingUsages(current)) {
            result.add(dependent)
            if (processedExporting.add(dependent)) {
                walkingQueue.addLast(dependent)
            }
        }
    }
    return result
}

/**
 * 获取模块信息对应的源根类型标识符
 */
val ModuleInfo.sourceRootTypeId: String?
    get() = when (this) {
        is ModuleProductionSourceInfo -> CangJieSourceRootTypes.SOURCE
        is ModuleTestSourceInfo -> CangJieSourceRootTypes.TEST
        else -> null
    }

fun IdeaModuleInfo.projectSourceModules(): List<ModuleSourceInfo> {
    return when (this) {
        is ModuleSourceInfo -> listOf(this)
        else -> emptyList()
    }
}

fun ModuleSourceInfo.getDependentModules(): Set<ModuleSourceInfo> {
    val dependents = getDependents(module)
    return when (sourceRootTypeId) {
        CangJieSourceRootTypes.TEST -> dependents.mapNotNullTo(HashSet<ModuleSourceInfo>()) { it.testSourceInfo }
        CangJieSourceRootTypes.SOURCE -> dependents.flatMapTo(HashSet<ModuleSourceInfo>()) { it.sourceModuleInfos }
        else -> error("ModuleInfo.sourceRootTypeId is null")
    }
}

val BinaryModuleInfo.binariesScope: GlobalSearchScope
    get() {
        val contentScope = contentScope
        if (GlobalSearchScope.isEmptyScope(contentScope)) {
            return contentScope
        }

        val project = contentScope.project
            ?: error("Project is empty for scope $contentScope (${contentScope.javaClass.name})")

        return CangJieSourceFilterScope.libraryClasses(contentScope, project)
    }
