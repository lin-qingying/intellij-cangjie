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

package com.linqingying.lsp.api.customization

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.SemanticTokenModifiers
import org.eclipse.lsp4j.SemanticTokenTypes
import org.jetbrains.annotations.ApiStatus

/**
 * 参见 LSP 规范：[语义标记](https://microsoft.github.io/language-server-protocol/specification/#textDocument_semanticTokens)
 */

open class LspSemanticTokensSupport {

    /**
     * `True` 表示 IDE 应该向服务器发送
     * [textDocument/semanticTokens/full](https://microsoft.github.io/language-server-protocol/specification/#semanticTokens_fullRequest)
     * 请求，并使用接收到的信息在给定文件中进行语义代码高亮显示。
     */
    open fun shouldAskServerForSemanticTokens(psiFile: PsiFile): Boolean {
        // 纯文本文件和基于 TextMate 捆绑的语法高亮（来自 TextMate 捆绑文件）没有 PSI 和高级代码高亮。
        // 因此，对于这些文件，基于 LSP 的语义高亮显示非常有意义。
        // 其他文件很可能具有 PSI 和一流的支持，因此它们可能不需要基于 LSP 的高亮显示。
        // 插件可以覆盖此逻辑。
        return psiFile.language.id.let { it == "TEXT" || it == "textmate" }
    }

    /**
     * 插件支持的语义标记类型列表。此列表用于
     * [SemanticTokensClientCapabilities.tokenTypes](https://microsoft.github.io/language-server-protocol/specification/#semanticTokensClientCapabilities)
     * ，并在发送 [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize) 请求时使用。
     *
     * @see [com.linqingying.lsp.api.LspServerDescriptor.clientCapabilities]
     */
    open val tokenTypes: List<String> = listOf(
        SemanticTokenTypes.Namespace,
        SemanticTokenTypes.Type,
        SemanticTokenTypes.Class,
        SemanticTokenTypes.Enum,
        SemanticTokenTypes.Interface,
        SemanticTokenTypes.Struct,
        SemanticTokenTypes.TypeParameter,
        SemanticTokenTypes.Parameter,
        SemanticTokenTypes.Variable,
        SemanticTokenTypes.Property,
        SemanticTokenTypes.EnumMember,
        SemanticTokenTypes.Event,
        SemanticTokenTypes.Function,
        SemanticTokenTypes.Method,
        SemanticTokenTypes.Macro,
        SemanticTokenTypes.Keyword,
        SemanticTokenTypes.Modifier,
        SemanticTokenTypes.Comment,
        SemanticTokenTypes.String,
        SemanticTokenTypes.Number,
        SemanticTokenTypes.Regexp,
        SemanticTokenTypes.Operator,
        SemanticTokenTypes.Decorator,
    )

    /**
     * 插件支持的语义标记修饰符列表。此列表用于
     * [SemanticTokensClientCapabilities.tokenModifiers](https://microsoft.github.io/language-server-protocol/specification/#semanticTokensClientCapabilities)
     * ，并在发送 [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize) 请求时使用。
     *
     * @see [com.linqingying.lsp.api.LspServerDescriptor.clientCapabilities]
     */
    open val tokenModifiers: List<String> = listOf(
        SemanticTokenModifiers.Declaration,
        SemanticTokenModifiers.Definition,
        SemanticTokenModifiers.Readonly,
        SemanticTokenModifiers.Static,
        SemanticTokenModifiers.Deprecated,
        SemanticTokenModifiers.Abstract,
        SemanticTokenModifiers.Async,
        SemanticTokenModifiers.Modification,
        SemanticTokenModifiers.Documentation,
        SemanticTokenModifiers.DefaultLibrary,
    )

    /**
     * 插件可能需要覆盖此函数，以确保所有 LSP 服务器支持的标记类型都能合理地映射到 [TextAttributesKey]。
     *
     * 可以不使用 LSP 服务器中的信息来提供基本的语法高亮，但例如，使用 TextMate 捆绑文件提供的语法高亮。
     * 在这种情况下，对于相应的标记类型，可以返回 `null`。
     */
    open fun getTextAttributesKey(tokenType: String, modifiers: List<String>): TextAttributesKey? = when (tokenType) {
        SemanticTokenTypes.Namespace -> DefaultLanguageHighlighterColors.IDENTIFIER
        SemanticTokenTypes.Type -> DefaultLanguageHighlighterColors.IDENTIFIER
        SemanticTokenTypes.Class -> DefaultLanguageHighlighterColors.CLASS_NAME
        SemanticTokenTypes.Enum -> DefaultLanguageHighlighterColors.CLASS_NAME
        SemanticTokenTypes.Interface -> DefaultLanguageHighlighterColors.INTERFACE_NAME
        SemanticTokenTypes.Struct -> DefaultLanguageHighlighterColors.IDENTIFIER
        SemanticTokenTypes.TypeParameter -> DefaultLanguageHighlighterColors.IDENTIFIER
        SemanticTokenTypes.Parameter -> DefaultLanguageHighlighterColors.PARAMETER
        SemanticTokenTypes.Variable -> DefaultLanguageHighlighterColors.LOCAL_VARIABLE
        SemanticTokenTypes.Property ->
            if (modifiers.contains(SemanticTokenModifiers.Static)) DefaultLanguageHighlighterColors.STATIC_FIELD
            else DefaultLanguageHighlighterColors.INSTANCE_FIELD

        SemanticTokenTypes.EnumMember ->
            if (modifiers.contains(SemanticTokenModifiers.Static)) DefaultLanguageHighlighterColors.STATIC_FIELD
            else DefaultLanguageHighlighterColors.INSTANCE_FIELD

        SemanticTokenTypes.Event -> DefaultLanguageHighlighterColors.IDENTIFIER
        SemanticTokenTypes.Function -> DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        SemanticTokenTypes.Method ->
            if (modifiers.contains(SemanticTokenModifiers.Static)) DefaultLanguageHighlighterColors.STATIC_METHOD
            else DefaultLanguageHighlighterColors.INSTANCE_METHOD

        SemanticTokenTypes.Macro -> DefaultLanguageHighlighterColors.IDENTIFIER
        SemanticTokenTypes.Keyword -> DefaultLanguageHighlighterColors.KEYWORD
        SemanticTokenTypes.Modifier -> HighlighterColors.TEXT
        SemanticTokenTypes.Comment -> DefaultLanguageHighlighterColors.LINE_COMMENT
        SemanticTokenTypes.String -> DefaultLanguageHighlighterColors.STRING
        SemanticTokenTypes.Number -> DefaultLanguageHighlighterColors.NUMBER
        SemanticTokenTypes.Regexp -> HighlighterColors.TEXT
        SemanticTokenTypes.Operator -> DefaultLanguageHighlighterColors.OPERATION_SIGN
        SemanticTokenTypes.Decorator -> HighlighterColors.TEXT
        else -> null
    }
}

