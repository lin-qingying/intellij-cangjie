/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.project.PossiblyDumbAware
import com.intellij.psi.PsiDirectory
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import org.cangnova.cangjie.psi.packgae.CangJiePackage

abstract class CangJiePsiElementFinderBase : PossiblyDumbAware {
    open fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        return true
    }

    companion object {

        val EP =
            ProjectExtensionPointName<CangJiePsiElementFinderBase>("cn.cangnova.cangjie.psi.elementFinder")

    }
}

