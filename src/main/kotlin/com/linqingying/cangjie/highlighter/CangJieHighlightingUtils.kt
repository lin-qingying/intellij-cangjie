@file:JvmName("CangJieHighlightingUtils")

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.ide.base.projectStructure.RootKindFilter
import com.linqingying.cangjie.ide.base.projectStructure.matches
import com.linqingying.cangjie.psi.CjCodeFragment
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

//
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
    if (CangJieLanguageServerServices.getInstance().astConfig.isFeatureEnabled(Feature.LIBRARY_DIAGNOSTICS)) {
        return RootKindFilter.projectSources.copy(includeLibraryClassFiles = true).matches(this)

    }

    return RootKindFilter.projectSources.copy().matches(this)
}

private fun isIndexingInProgress(project: Project) = runReadAction { DumbService.getInstance(project).isDumb }


