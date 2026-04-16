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

package org.cangnova.cangjie.formatter.util

import org.cangnova.cangjie.formatter.containsLineBreakInChild
import org.cangnova.cangjie.formatter.isMultiline
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.startOffset
import com.intellij.psi.PsiElement

enum class TrailingCommaState {

    EXISTS,


    MISSING,



    NOT_EXISTS,


    REDUNDANT,


    NOT_APPLICABLE,
    ;

    companion object {
        fun stateForElement(element: PsiElement): TrailingCommaState = when {
            element !is CjElement || !element.canAddTrailingComma() -> NOT_APPLICABLE
            isMultiline(element) ->
                if (TrailingCommaHelper.trailingCommaExists(element))
                    EXISTS
                else
                    MISSING
            else ->
                if (TrailingCommaHelper.trailingCommaExists(element))
                    REDUNDANT
                else
                    NOT_EXISTS
        }
    }
}

private fun isMultiline(cjElement: CjElement): Boolean = when {
    cjElement.parent is CjFunctionLiteral -> isMultiline(cjElement.parent as CjElement)

    cjElement is CjFunctionLiteral -> cjElement.isMultiline(
        startOffsetGetter = { valueParameterList?.startOffset },
        endOffsetGetter = { arrow?.endOffset },
    )

    cjElement is CjMatchEntry -> cjElement.isMultiline(
        startOffsetGetter = { startOffset },
        endOffsetGetter = { arrow?.endOffset },
    )



    else -> cjElement.isMultiline()
}

private fun <T : PsiElement> T.isMultiline(
    startOffsetGetter: T.() -> Int?,
    endOffsetGetter: T.() -> Int?,
): Boolean {
    val startOffset = startOffsetGetter() ?: startOffset
    val endOffset = endOffsetGetter() ?: endOffset
    return containsLineBreakInChild(startOffset, endOffset)
}
