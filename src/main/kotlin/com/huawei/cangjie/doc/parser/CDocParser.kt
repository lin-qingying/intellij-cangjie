package com.huawei.cangjie.doc.parser

import com.huawei.cangjie.doc.lexer.CDocTokens.*
import com.huawei.cangjie.doc.parser.CDocElementTypes.*
import com.huawei.cangjie.doc.parser.CDocKnownTag.Companion.findByTagName
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

class CDocParser :PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        if (builder.tokenType === START) {
            builder.advanceLexer()
        }
        var currentSectionMarker: PsiBuilder.Marker? = builder.mark()



        while (!builder.eof()) {
            if (builder.tokenType === TAG_NAME) {
                currentSectionMarker = parseTag(builder, currentSectionMarker)
            } else if (builder.tokenType === END) {
                if (currentSectionMarker != null) {
                    currentSectionMarker.done(CDOC_SECTION)
                    currentSectionMarker = null
                }
                builder.advanceLexer()
            } else {
                builder.advanceLexer()
            }
        }

        currentSectionMarker?.done(CDOC_SECTION)
        rootMarker.done(root)
        return builder.treeBuilt
    }

    companion object{
        private fun parseTag(builder: PsiBuilder, currentSectionMarker: PsiBuilder.Marker?): PsiBuilder.Marker? {
            var currentSectionMarker = currentSectionMarker
            val tagName = builder.tokenText
            val knownTag: CDocKnownTag? = tagName?.let { findByTagName(it) }
            if (knownTag != null && knownTag.isSectionStart) {
                if (currentSectionMarker != null) {
                    currentSectionMarker.done(CDOC_SECTION)
                }
                currentSectionMarker = builder.mark()
            }
            val tagStart = builder.mark()
            builder.advanceLexer()
            while (!builder.eof() && !isAtEndOfTag(builder)) {
                builder.advanceLexer()
            }
            tagStart.done(CDOC_TAG)
            return currentSectionMarker
        }

        private fun isAtEndOfTag(builder: PsiBuilder): Boolean {
            if (builder.tokenType === END) {
                return true
            }
            if (builder.tokenType === LEADING_ASTERISK) {
                var lookAheadCount = 1
                if (builder.lookAhead(1) === TEXT) {
                    lookAheadCount++
                }
                if (builder.lookAhead(lookAheadCount) === TAG_NAME) {
                    return true
                }
            }
            return false
        }
    }
}
