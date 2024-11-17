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

package com.linqingying.cangjie.ide.lineMarkers.markers

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import javax.swing.Icon

class InheritanceMergeableLineMarkerInfo(
    element: PsiElement,
    textRange: TextRange,
    icon: Icon,
    tooltip: Function<in PsiElement, String>,
    navigationHandler: GutterIconNavigationHandler<PsiElement>,
    alignment: GutterIconRenderer.Alignment,
    accessibleNameProvider: () -> String
) : MergeableLineMarkerInfo<PsiElement>(element, textRange, icon, tooltip, navigationHandler, alignment, accessibleNameProvider) {

    override fun canMergeWith(info: MergeableLineMarkerInfo<*>): Boolean = info is InheritanceMergeableLineMarkerInfo && info.icon == icon

    override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<*>>): Icon = infos.first().icon
}
