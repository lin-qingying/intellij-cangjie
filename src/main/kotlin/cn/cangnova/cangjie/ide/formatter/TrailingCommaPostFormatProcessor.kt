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

package cn.cangnova.cangjie.ide.formatter


import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaContext
import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaHelper.findInvalidCommas
import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaHelper.lineBreakIsMissing
import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaHelper.trailingCommaOrLastElement
import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaState
import cn.cangnova.cangjie.ide.formatter.util.TrailingCommaVisitor
import cn.cangnova.cangjie.ide.formatter.util.addTrailingCommaIsAllowedFor
import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjPsiFactory
import cn.cangnova.cangjie.psi.psiUtil.siblings
import cn.cangnova.cangjie.utils.reformatted
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper

class TrailingCommaPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        if (source.language != CangJieLanguage) return source

        return TrailingCommaPostFormatVisitor(settings).process(source)
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        if (source.language != CangJieLanguage) return rangeToReformat

        return TrailingCommaPostFormatVisitor(settings).processText(source, rangeToReformat)
    }
}

private class TrailingCommaPostFormatVisitor(private val settings: CodeStyleSettings) : TrailingCommaVisitor() {
    private val myPostProcessor = PostFormatProcessorHelper(settings.cangjieCommonSettings)

    override fun process(trailingCommaContext: TrailingCommaContext) = processIfInRange(trailingCommaContext.cjElement) {
        processCommaOwner(trailingCommaContext)
    }

    private fun processIfInRange(element: CjElement, block: () -> Unit = {}) {
        if (myPostProcessor.isElementPartlyInRange(element)) {
            block()
        }
    }

    private fun processCommaOwner(trailingCommaContext: TrailingCommaContext) {
        val cjElement = trailingCommaContext.cjElement

        val lastElementOrComma = trailingCommaOrLastElement(cjElement) ?: return
        updatePsi(cjElement) {
            val state = trailingCommaContext.state
            when {
                state == TrailingCommaState.MISSING && settings.cangjieCustomSettings.addTrailingCommaIsAllowedFor(cjElement) -> {

                    val hasChange = false

                    correctCommaPosition(cjElement) || hasChange
                }

                state == TrailingCommaState.EXISTS -> {
                    correctCommaPosition(cjElement)
                }

                state == TrailingCommaState.REDUNDANT -> {

                    lastElementOrComma.delete()
                    true
                }

                else -> false
            }
        }
    }

    private fun updatePsi(element: CjElement, updater: () -> Boolean) {
        val oldLength = element.parent?.textLength
        if (!updater()) return

        val resultElement = element.reformatted(true)
        oldLength?.let { myPostProcessor.updateResultRange(it, resultElement.parent.textLength) }
    }

    private fun correctCommaPosition(parent: CjElement): Boolean {
        var hasChange = false
        for (pointerToComma in findInvalidCommas(parent).map { it.createSmartPointer() }) {
            pointerToComma.element?.let {
                correctComma(it)
                hasChange = true
            }
        }

        return hasChange || lineBreakIsMissing(parent)
    }

    fun process(formatted: PsiElement): PsiElement {
        LOG.assertTrue(formatted.isValid)
        formatted.accept(this)
        return formatted
    }

    fun processText(
        source: PsiFile,
        rangeToReformat: TextRange,
    ): TextRange {
        myPostProcessor.resultTextRange = rangeToReformat
        source.accept(this)
        return myPostProcessor.resultTextRange
    }

    companion object {
        private val LOG = Logger.getInstance(TrailingCommaVisitor::class.java)
    }
}

private fun PsiElement.addCommaAfter(factory: CjPsiFactory) {
    val comma = factory.createComma()
    parent.addAfter(comma, this)
}

private fun correctComma(comma: PsiElement) {
    val prevWithComment = comma.leafIgnoringWhitespace(false) ?: return
    val prevWithoutComment = comma.leafIgnoringWhitespaceAndComments(false) ?: return
    if (prevWithComment != prevWithoutComment) {
        val check = { element: PsiElement -> element is PsiWhiteSpace || element is PsiComment }
        val firstElement = prevWithComment.siblings(forward = false, withItself = true).takeWhile(check).last()
        val commentOwner = prevWithComment.parent
        comma.parent.addRangeAfter(firstElement, prevWithComment, comma)
        commentOwner.deleteChildRange(firstElement, prevWithComment)
    }
}
fun <E : PsiElement> E.createSmartPointer(): SmartPsiElementPointer<E> =
    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
