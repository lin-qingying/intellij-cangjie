package com.linqingying.cangjie.ide.projectStructure

import com.linqingying.cangjie.analyzer.ModuleInfo

import com.linqingying.cangjie.ide.projectStructure.moduleInfo.NotUnderContentRootModuleInfo
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager


val PsiElement.moduleInfo: ModuleInfo
    get() = moduleInfoOrNull ?: NotUnderContentRootModuleInfo(project, containingFile as? CjFile)
val PsiElement.moduleInfoOrNull: ModuleInfo?
    get() {
        val anchorElement = ModuleInfoProvider.findAnchorElement(this)
        return if (anchorElement != null) {
            cachedModuleInfo(anchorElement)
        } else {
            ModuleInfoProvider.getInstance(project).firstOrNull(this)
        }
    }
fun ModuleInfoProvider.firstOrNull(virtualFile: VirtualFile): ModuleInfo? =
    collect(virtualFile).unwrap(ModuleInfoProvider.LOG::warn).firstOrNull()

fun ModuleInfoProvider.firstOrNull(
    element: PsiElement,
    config: ModuleInfoProvider.Configuration = ModuleInfoProvider.Configuration.Default
): ModuleInfo? =
    collect(element, config).unwrap(ModuleInfoProvider.LOG::warn).firstOrNull()

private fun cachedModuleInfo(
    anchorElement: PsiElement,
) = CachedValuesManager.getCachedValue(anchorElement) {
    val project = anchorElement.project
    CachedValueProvider.Result.create(
        ModuleInfoProvider.getInstance(project).firstOrNull(anchorElement),
        ProjectRootModificationTracker.getInstance(project),

//        CangJieModificationTrackerFactory.getInstance(project).createProjectWideOutOfBlockModificationTracker(),
    )
}
