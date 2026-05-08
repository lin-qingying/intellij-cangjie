@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProviderBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule

/**
 * IDE 插件层 project-structure provider。
 *
 * 对位 Kotlin IDEA 插件 `IDEProjectStructureProvider`：
 * 这层只暴露 IntelliJ 插件真正需要的 OpenAPI / source-module 辅助入口，
 * 不把底层 Analysis API 平台接口直接扩散到插件调用面。
 */
abstract class CangJieIdeProjectStructureProvider : CangJieProjectStructureProviderBase() {
    abstract val self: CangJieIdeProjectStructureProvider

    abstract fun getCaSourceModule(moduleId: ModuleId, kind: CaSourceModuleKind): CaSourceModule?

    abstract fun getCaSourceModules(moduleId: ModuleId): List<CaSourceModule>

    abstract fun getCaSourceModule(moduleEntity: ModuleEntity, kind: CaSourceModuleKind): CaSourceModule?

    abstract fun getCaSourceModules(moduleEntity: ModuleEntity): List<CaSourceModule>

    abstract fun getCaSourceModuleSymbolId(module: CaSourceModule): ModuleId

    abstract fun getCaSourceModule(openapiModule: Module, kind: CaSourceModuleKind): CaSourceModule?

    abstract fun getCaSourceModules(openapiModule: Module): List<CaSourceModule>

    abstract fun getOpenapiModule(module: CaSourceModule): Module

    abstract fun getCaLibraryModules(libraryId: LibraryId): List<CaLibraryModule>

    abstract fun getCaLibraryModules(libraryEntity: LibraryEntity): List<CaLibraryModule>

    abstract fun getCaLibraryModules(library: Library): List<CaLibraryModule>

    abstract fun getCaLibraryModuleSymbolicId(libraryModule: CaLibraryModule): LibraryId

    abstract fun getOpenapiLibrary(module: CaLibraryModule): Library?

    abstract fun getAssociatedCaModules(virtualFile: VirtualFile): List<CaModule>

    /**
     * 返回会在任意 IDE project-structure 模块缓存失效时递增的 tracker。
     */
    abstract fun getCacheDependenciesTracker(): ModificationTracker
}

val Project.cangjieIdeProjectStructureProvider: CangJieIdeProjectStructureProvider
    get() = CangJieProjectStructureProvider.getInstance(this) as CangJieIdeProjectStructureProvider
