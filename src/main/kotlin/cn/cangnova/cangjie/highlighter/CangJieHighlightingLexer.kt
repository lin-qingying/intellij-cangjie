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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.doc.lexer.CDocLexer
import cn.cangnova.cangjie.lexer.CangJieLexer
import cn.cangnova.cangjie.lexer.CjTokens
import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.StringLiteralLexer
import com.intellij.psi.tree.IElementType

class CangJieHighlightingLexer : LayeredLexer(CangJieLexer()) {
    init {

        registerSelfStoppingLayer(
            CDocLexer(),
            arrayOf<IElementType>(CjTokens.DOC_COMMENT),
            IElementType.EMPTY_ARRAY,
        )
        registerSelfStoppingLayer(
            StringLiteralLexer('r', CjTokens.RUNE_LITERAL),
            arrayOf<IElementType>(CjTokens.RUNE_LITERAL),
            IElementType.EMPTY_ARRAY,
        )
    }
}
