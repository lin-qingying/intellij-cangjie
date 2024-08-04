package com.linqingying.cangjie.ide.formatter

import com.linqingying.cangjie.ide.codeinsight.impl.TrailingCommaVisitor

import com.linqingying.cangjie.ide.formatter.util.TrailingCommaContext
import com.linqingying.cangjie.ide.formatter.util.TrailingCommaHelper.findInvalidCommas
import com.linqingying.cangjie.ide.formatter.util.TrailingCommaHelper.lineBreakIsMissing
import com.linqingying.cangjie.ide.formatter.util.TrailingCommaHelper.trailingCommaOrLastElement
import com.linqingying.cangjie.ide.formatter.util.TrailingCommaState
import com.linqingying.cangjie.ide.formatter.util.addTrailingCommaIsAllowedFor
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjPsiFactory
import com.linqingying.cangjie.psi.psiUtil.siblings
import com.linqingying.cangjie.utils.reformatted
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper
import com.intellij.refactoring.suggested.createSmartPointer

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
