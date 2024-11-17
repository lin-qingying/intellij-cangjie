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

package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.psiUtil.startOffset
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.min


class ToFromOriginalFileMapper private constructor(
    val originalFile: CjFile,
    private val syntheticFile: CjFile,
    private val completionOffset: Int
)
{
    private val syntheticLength: Int
    private val originalLength: Int
    private val tailLength: Int
    private val shift: Int

    private val typeParamsOffset: Int
    private val typeParamsShift: Int
    init {
        val (originalText, syntheticText) = runReadAction {
            originalFile.text to syntheticFile.text
        }

        typeParamsOffset = (0 until completionOffset).firstOrNull { originalText[it] != syntheticText[it] } ?: 0
        if (typeParamsOffset > 0) {
            val typeParamsEnd = (typeParamsOffset until completionOffset).first { originalText[typeParamsOffset] == syntheticText[it] }
            typeParamsShift = typeParamsEnd - typeParamsOffset
        } else {
            typeParamsShift = 0
        }

        syntheticLength = syntheticText.length
        originalLength = originalText.length
        val minLength = min(originalLength, syntheticLength)
        tailLength = (0 until minLength).firstOrNull {
            syntheticText[syntheticLength - it - 1] != originalText[originalLength - it - 1]
        } ?: minLength
        shift = syntheticLength - originalLength
    }
    private fun toSyntheticFile(offset: Int): Int? = when {
        offset <= typeParamsOffset -> offset
        offset in (typeParamsOffset + 1)..completionOffset -> offset + typeParamsShift
        offset >= originalLength - tailLength -> offset + shift + typeParamsShift
        else -> null
    }
    fun <TElement : PsiElement> toOriginalFile(element: TElement): TElement? {
        if (element.containingFile != syntheticFile) return element
        val offset = toOriginalFile(element.startOffset) ?: return null
        return PsiTreeUtil.findElementOfClassAtOffset(originalFile, offset, element::class.java, true)
    }
    private fun toOriginalFile(offset: Int): Int? = when {
        offset <= typeParamsOffset -> offset
        offset in (typeParamsOffset + 1)..completionOffset -> offset - typeParamsShift
        offset >= originalLength - tailLength -> offset - shift - typeParamsShift
        else -> null
    }
    fun <TElement : PsiElement> toSyntheticFile(element: TElement): TElement? {
        if (element.containingFile != originalFile) return element
        val offset = toSyntheticFile(element.startOffset) ?: return null
        return PsiTreeUtil.findElementOfClassAtOffset(syntheticFile, offset, element::class.java, true)
    }
    companion object {
        fun create(parameters: CompletionParameters): ToFromOriginalFileMapper {
            val originalFile = parameters.originalFile as CjFile
            val syntheticFile = parameters.position.containingFile as CjFile
            return ToFromOriginalFileMapper(originalFile, syntheticFile, parameters.offset)
        }
    }
}
