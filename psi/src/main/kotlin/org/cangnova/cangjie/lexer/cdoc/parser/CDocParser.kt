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

package org.cangnova.cangjie.lexer.cdoc.parser

import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens.*
import org.cangnova.cangjie.lexer.cdoc.parser.CDocElementTypes.*
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag.Companion.findByTagName
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

class CDocParser : PsiParser {
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

    companion object {
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
