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

package org.cangnova.cangjie.analysis

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.projectStructure.RootKindFilter
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

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

