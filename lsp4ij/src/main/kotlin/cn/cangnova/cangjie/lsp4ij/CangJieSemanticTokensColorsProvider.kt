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

package cn.cangnova.cangjie.lsp4ij

import cn.cangnova.cangjie.highlighter.CangJieHighlightingColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.features.semanticTokens.DefaultSemanticTokensColorsProvider

class CangJieSemanticTokensColorsProvider : DefaultSemanticTokensColorsProvider() {
    override fun getTextAttributesKey(
        tokenType: String,
        tokenModifiers: List<String>,
        file: PsiFile
    ): TextAttributesKey? = when (tokenType) {
        "comment" -> CangJieHighlightingColors.BLOCK_COMMENT
        "keyword" -> CangJieHighlightingColors.KEYWORD
        "string" -> CangJieHighlightingColors.STRING
        "number" -> CangJieHighlightingColors.NUMBER
//        "regexp" ->
        "operator" -> CangJieHighlightingColors.OPERATOR_SIGN
        "type" -> CangJieHighlightingColors.TYPE_ALIAS
        "enum", "class", "interface", "struct" -> CangJieHighlightingColors.CLASS

        "member", "function" ->
            CangJieHighlightingColors.FUNCTION_DECLARATION

        "parameter" -> CangJieHighlightingColors.PARAMETER
        "typeParameter" -> CangJieHighlightingColors.TYPE_PARAMETER
        "property" -> CangJieHighlightingColors.LOCAL_VARIABLE
        "variable" ->
            CangJieHighlightingColors.LOCAL_VARIABLE

        else -> super.getTextAttributesKey(tokenType, tokenModifiers, file)
    }


}
