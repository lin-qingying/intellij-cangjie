package com.huawei.cangjie.ide.projectStructure

import com.huawei.cangjie.analyzer.ModuleInfo

import com.huawei.cangjie.ide.projectStructure.moduleInfo.NotUnderContentRootModuleInfo
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.roots.ProjectRootModificationTracker
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
