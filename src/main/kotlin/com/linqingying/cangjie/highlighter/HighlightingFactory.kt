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

package com.linqingying.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DetailedDescription
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


object HighlightingFactory {
    fun highlightName(element: PsiElement, highlightInfoType: HighlightInfoType, message: @DetailedDescription String? = null): HighlightInfo.Builder? {
        val project = element.project
        if (!element.textRange.isEmpty) {
            return highlightName(project, element.textRange, highlightInfoType, message)
        }
        return null
    }

    fun highlightName(
        project: Project,
        textRange: TextRange,
        highlightInfoType: HighlightInfoType,
        message: @DetailedDescription String? = null
    ): HighlightInfo.Builder {
        val builder = HighlightInfo.newHighlightInfo(highlightInfoType)
        if (message != null) {
            builder.descriptionAndTooltip(message)
        }
        val annotation = builder
            .range(textRange)
        return annotation
    }
}
