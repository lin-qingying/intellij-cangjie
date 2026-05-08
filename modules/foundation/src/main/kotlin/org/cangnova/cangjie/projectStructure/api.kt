@file:Suppress("unused")
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider

/**
 * 对位 Kotlin `KaSourceModuleKind`。
 *
 * IDE 平台源码模块按 IntelliJ module 的生产/测试视图分层，
 * 每个源码模块必须准确绑定其中一种 kind。
 */
enum class CaSourceModuleKind {
    PRODUCTION,
    TEST,
}

/**
 * 对位 Kotlin `KaSourceModuleWithKind`。
 */
interface CaSourceModuleWithKind : CaSourceModule {
    val kind: CaSourceModuleKind
}

fun ModuleId.toCaSourceModule(project: Project, kind: CaSourceModuleKind): CaSourceModule? =
    project.cangjieIdeProjectStructureProvider.getCaSourceModule(this, kind)

fun ModuleId.toCaSourceModuleForProduction(project: Project): CaSourceModule? =
    toCaSourceModule(project, CaSourceModuleKind.PRODUCTION)

fun ModuleId.toCaSourceModuleForTest(project: Project): CaSourceModule? =
    toCaSourceModule(project, CaSourceModuleKind.TEST)

fun ModuleId.toCaSourceModules(project: Project): List<CaSourceModule> =
    project.cangjieIdeProjectStructureProvider.getCaSourceModules(this)

fun ModuleEntity.toCaSourceModule(project: Project, kind: CaSourceModuleKind): CaSourceModule? =
    project.cangjieIdeProjectStructureProvider.getCaSourceModule(this, kind)

fun ModuleEntity.toCaSourceModules(project: Project): List<CaSourceModule> =
    project.cangjieIdeProjectStructureProvider.getCaSourceModules(this)

fun Module.toCaSourceModule(kind: CaSourceModuleKind): CaSourceModule? =
    project.cangjieIdeProjectStructureProvider.getCaSourceModule(this, kind)

fun Module.toCaSourceModuleForProduction(): CaSourceModule? =
    toCaSourceModule(CaSourceModuleKind.PRODUCTION)

fun Module.toCaSourceModuleForTest(): CaSourceModule? =
    toCaSourceModule(CaSourceModuleKind.TEST)

fun Module.toCaSourceModules(): List<CaSourceModule> =
    project.cangjieIdeProjectStructureProvider.getCaSourceModules(this)

fun Module.toCaSourceModuleWithElementSourceModuleKindOrProduction(contextElement: PsiElement): CaSourceModule? {
    val contextSourceModule =
        contextElement.getCaModuleOfTypeSafe<CaSourceModule>(project, useSiteModule = null)
            ?: return toCaSourceModuleForProduction()

    return toCaSourceModule(contextSourceModule.sourceModuleKind)
}

fun Module.toCaSourceModuleContainingElement(element: PsiElement): CaSourceModule? {
    val virtualFile = when (element) {
        is PsiFileSystemItem -> element.virtualFile
        else -> element.containingFile?.virtualFile
    } ?: return null

    return toCaSourceModules().firstOrNull { virtualFile in it.contentScope }
}

val CaSourceModule.symbolicId: ModuleId
    get() = project.cangjieIdeProjectStructureProvider.getCaSourceModuleSymbolId(this)

val CaSourceModule.sourceModuleKind: CaSourceModuleKind
    get() {
        require(this is CaSourceModuleWithKind) {
            "Expected `${CaSourceModuleWithKind::class.simpleName}`, but got `${this::class.simpleName}` instead."
        }
        return kind
    }

val CaSourceModule.openapiModule: Module
    get() = project.cangjieIdeProjectStructureProvider.getOpenapiModule(this)

fun Library.toCaLibraryModules(project: Project): List<CaLibraryModule> =
    project.cangjieIdeProjectStructureProvider.getCaLibraryModules(this)

fun LibraryId.toCaLibraryModules(project: Project): List<CaLibraryModule> =
    project.cangjieIdeProjectStructureProvider.getCaLibraryModules(this)

fun LibraryEntity.toCaLibraryModules(project: Project): List<CaLibraryModule> =
    project.cangjieIdeProjectStructureProvider.getCaLibraryModules(this)

val CaLibraryModule.symbolicId: LibraryId
    get() = project.cangjieIdeProjectStructureProvider.getCaLibraryModuleSymbolicId(this)

val CaLibraryModule.openapiLibrary: Library?
    get() = project.cangjieIdeProjectStructureProvider.getOpenapiLibrary(this)

fun PsiElement.getCaModule(project: Project, useSiteModule: CaModule?): CaModule =
    CangJieProjectStructureProvider.getModule(project, this, useSiteModule)

inline fun <reified M : CaModule> PsiElement.getCaModuleOfTypeSafe(project: Project, useSiteModule: CaModule?): M? =
    getCaModule(project, useSiteModule) as? M

inline fun <reified M : CaModule> PsiElement.getCaModuleOfType(project: Project, useSiteModule: CaModule?): M =
    getCaModule(project, useSiteModule) as M

fun VirtualFile.getAssociatedCaModules(project: Project): List<CaModule> =
    project.cangjieIdeProjectStructureProvider.getAssociatedCaModules(this)
