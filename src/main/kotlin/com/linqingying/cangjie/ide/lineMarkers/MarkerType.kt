/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.ide.lineMarkers

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement

class MarkerType(
    val debugMessage: String, val tooltip: (PsiElement) -> String?,   navigator: LineMarkerNavigator
)
{
    override fun toString(): String {
        return debugMessage
    }





      val navigationHandler: GutterIconNavigationHandler<PsiElement> =
        if (ApplicationManager.getApplication().isUnitTestMode && navigator is GutterIconNavigationHandler<*>) {
            @Suppress("UNCHECKED_CAST")
            navigator as GutterIconNavigationHandler<PsiElement>
        } else {
            GutterIconNavigationHandler { e, elt ->
                DumbService.getInstance(elt.project).withAlternativeResolveEnabled { navigator.browse(e, elt) }
            }
        }

}


