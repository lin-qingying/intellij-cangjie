package com.huawei.cangjie.ide.highlighter

import com.huawei.cangjie.psi.CjCodeFragment
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

//
//fun CjFile.shouldHighlightErrors(): Boolean {
//    if (isCompiled) {
//        return false
//    }
//
//    if (this is CjCodeFragment && context != null) {
//        return true
//    }
//
//    val indexingInProgress = isIndexingInProgress(project)
////    if (!indexingInProgress && isScript()) { /* isScript() is based on stub index */
////        return calculateShouldHighlightScript()
////    }
//
//    return RootKindFilter.projectSources.copy(includeScriptsOutsideSourceRoots = indexingInProgress).matches(this)
//}

private fun isIndexingInProgress(project: Project) = runReadAction { DumbService.getInstance(project).isDumb }
