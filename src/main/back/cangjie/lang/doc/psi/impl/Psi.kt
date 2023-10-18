package com.huawei.cangjie.lang.doc.psi.impl

import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.huawei.cangjie.lang.core.psi.SimpleMultiLineTextEscaper
import com.huawei.cangjie.lang.core.psi.ext.*
import com.huawei.cangjie.lang.doc.psi.*
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.AstBufferUtil
import com.intellij.psi.impl.source.tree.CompositePsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil


import com.intellij.util.text.CharArrayUtil


abstract class CjDocElementImpl(type: IElementType) : CompositePsiElement(type), CjDocElement {
    protected open fun <T: Any> notNullChild(child: T?): T =
        child ?: error("$text parent=${parent.text}")

    override val containingDoc: CjDocComment
        get() = ancestorStrict()
            ?: error("CjDocElement cannot leave outside of the doc comment! `${text}`")

    override val markdownValue: String
        get() = AstBufferUtil.getTextSkippingWhitespaceComments(this)

    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

class CjDocGapImpl(type: IElementType, val text: CharSequence) : LeafPsiElement(type, text), CjDocGap {
    override fun getTokenType(): IElementType = elementType
}

class CjDocAtxHeadingImpl(type: IElementType) : CjDocElementImpl(type), CjDocAtxHeading
class CjDocSetextHeadingImpl(type: IElementType) : CjDocElementImpl(type), CjDocSetextHeading

class CjDocEmphasisImpl(type: IElementType) : CjDocElementImpl(type), CjDocEmphasis
class CjDocStrongImpl(type: IElementType) : CjDocElementImpl(type), CjDocStrong
class CjDocCodeSpanImpl(type: IElementType) : CjDocElementImpl(type), CjDocCodeSpan
class CjDocAutoLinkImpl(type: IElementType) : CjDocElementImpl(type), CjDocAutoLink

class CjDocInlineLinkImpl(type: IElementType) : CjDocElementImpl(type), CjDocInlineLink {
    override val linkText: CjDocLinkText
        get() = notNullChild(childOfType())

    override val linkDestination: CjDocLinkDestination
        get() = notNullChild(childOfType())
}

class CjDocLinkReferenceShortImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkReferenceShort {
    override val linkLabel: CjDocLinkLabel
        get() = notNullChild(childOfType())
}

class CjDocLinkReferenceFullImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkReferenceFull {
    override val linkText: CjDocLinkText
        get() = notNullChild(childOfType())

    override val linkLabel: CjDocLinkLabel
        get() = notNullChild(childOfType())
}

class CjDocLinkDefinitionImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkDefinition {
    override val linkLabel: CjDocLinkLabel
        get() = notNullChild(childOfType())

    override val linkDestination: CjDocLinkDestination
        get() = notNullChild(childOfType())
}

class CjDocLinkTextImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkText
class CjDocLinkLabelImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkLabel
class CjDocLinkTitleImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkTitle
class CjDocLinkDestinationImpl(type: IElementType) : CjDocElementImpl(type), CjDocLinkDestination

class CjDocCodeFenceImpl(type: IElementType) : CjDocElementImpl(type), CjDocCodeFence {
    override fun isValidHost(): Boolean = true

    override val start: CjDocCodeFenceStartEnd
        get() = notNullChild(childOfType())

    override val end: CjDocCodeFenceStartEnd?
        get() = childrenOfType<CjDocCodeFenceStartEnd>().getOrNull(1)

    override val lang: CjDocCodeFenceLang?
        get() = childOfType()

    override fun updateText(text: String): PsiLanguageInjectionHost {
        val docKind = CjDocKind.of(containingDoc.elementType)
        val infix = docKind.infix

        val prevSibling = getPrevNonWhitespaceSibling() // Should be an `infix` (e.g. `///`)

        val newText = StringBuilder()

        if (prevSibling != null && prevSibling.text != docKind.prefix) {
            newText.append(docKind.prefix)

            val prevPrevSibling = prevSibling.prevSibling
            if (prevPrevSibling is PsiWhiteSpace) {
                newText.append(prevPrevSibling.text)
            } else {
                newText.append("\n")
            }
        }

        newText.append(docKind.infix)

         if (prevSibling != null && prevSibling.nextSibling != this) {
            newText.append(prevSibling.nextSibling.text)
        }

        var prevIndent = ""
        var index = 0
        while (index < text.length) {
            val linebreakIndex = text.indexOf("\n", index)
            if (linebreakIndex == -1) {
                newText.append(text, index, text.length)
                break
            } else {
                val nextLineStart = linebreakIndex + 1
                newText.append(text, index, nextLineStart)
                index = nextLineStart

                val firstNonWhitespace = CharArrayUtil.shiftForward(text, nextLineStart, " \t")
                if (firstNonWhitespace == text.length) continue
                val isStartCorrect = text.startsWith(infix, firstNonWhitespace) ||
                        docKind.isBlock && text.startsWith("*/", firstNonWhitespace)
                if (!isStartCorrect) {
                    newText.append(prevIndent)
                    newText.append(infix)
                    newText.append(" ")
                } else {
                    prevIndent = text.substring(nextLineStart, firstNonWhitespace)
                }
            }
        }

        if (docKind.isBlock && !newText.endsWith("*/")) {
            newText.append("\n*/")
        }

         val fromText = CjPsiFactory(project, markGenerated = true).createFile(newText)
        val newElement = PsiTreeUtil.findChildOfType(fromText, javaClass, false)
            ?: error(newText)
        return replace(newElement) as CjDocCodeFenceImpl
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<CjDocCodeFenceImpl> =
        SimpleMultiLineTextEscaper(this)
}

class CjDocCodeBlockImpl(type: IElementType) : CjDocElementImpl(type), CjDocCodeBlock
class CjDocHtmlBlockImpl(type: IElementType) : CjDocElementImpl(type), CjDocHtmlBlock

class CjDocCodeFenceStartEndImpl(type: IElementType) : CjDocElementImpl(type), CjDocCodeFenceStartEnd
class CjDocCodeFenceLangImpl(type: IElementType) : CjDocElementImpl(type), CjDocCodeFenceLang
