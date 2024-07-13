@file:JvmName("CangJieHighlightingUtils")
package com.huawei.cangjie.idea.highlighter

import com.huawei.cangjie.idea.base.projectStructure.RootKindFilter
import com.huawei.cangjie.idea.base.projectStructure.RootKindMatcher
import com.huawei.cangjie.psi.CjCodeFragment
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

//@ApiStatus.Internal
fun CjFile.shouldHighlightErrors(): Boolean {
    if (isCompiled) {
        return false
    }

    if (this is CjCodeFragment && context != null) {
        return true
    }

    val indexingInProgress = isIndexingInProgress(project)
//    if (!indexingInProgress && isScript()) { /* isScript() is based on stub index */
//        return calculateShouldHighlightScript()
//    }

    return RootKindFilter.projectSources.copy(includeScriptsOutsideSourceRoots = indexingInProgress).matches(this)
}

private fun isIndexingInProgress(project: Project) = runReadAction { DumbService.getInstance(project).isDumb }

fun RootKindFilter.matches(element: PsiElement): Boolean {
    return RootKindMatcher.matches(element, this)
}
fun RootKindFilter.matches(project: Project, virtualFile: VirtualFile): Boolean {
    return RootKindMatcher.matches(project, virtualFile, this)
}
