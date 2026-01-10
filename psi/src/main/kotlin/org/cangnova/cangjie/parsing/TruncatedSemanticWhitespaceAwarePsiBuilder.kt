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
package org.cangnova.cangjie.parsing

import com.intellij.psi.tree.IElementType


class TruncatedSemanticWhitespaceAwarePsiBuilder(
    builder: SemanticWhitespaceAwarePsiBuilder ,
    private val myEOFPosition: Int
) : SemanticWhitespaceAwarePsiBuilderAdapter(builder) {
    override fun eof(): Boolean {
        return super.eof() || isOffsetBeyondEof(currentOffset)
    }

    override fun getTokenText(): String? {
        if (eof()) return null
        return super.getTokenText()
    }

    override fun getTokenType(): IElementType? {
        if (eof()) return null
        return super.getTokenType()
    }

    override fun lookAhead(steps: Int): IElementType? {
        if (eof()) return null

        val rawLookAheadSteps = rawLookAhead(steps)
        if (isOffsetBeyondEof(rawTokenTypeStart(rawLookAheadSteps))) return null

        return super.rawLookup(rawLookAheadSteps)
    }

    private fun rawLookAhead(steps: Int): Int {
        var steps = steps
        var cur = 0
        while (steps > 0) {
            cur++

            var rawTokenType = rawLookup(cur)
            while (rawTokenType != null && isWhitespaceOrComment(rawTokenType)) {
                cur++
                rawTokenType = rawLookup(cur)
            }

            steps--
        }
        return cur
    }

    private fun isOffsetBeyondEof(offsetFromCurrent: Int): Boolean {
        return myEOFPosition >= 0 && offsetFromCurrent >= myEOFPosition
    }
}
