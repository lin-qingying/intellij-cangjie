// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.linqingying.lsp.api.customization

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import org.eclipse.lsp4j.SemanticTokenModifiers
import org.eclipse.lsp4j.SemanticTokenTypes
import org.jetbrains.annotations.ApiStatus

/**
 * See LSP specification: [Semantic Tokens](https://microsoft.github.io/language-server-protocol/specification/#textDocument_semanticTokens)
 */

open class LspSemanticTokensSupport {
  /**
   * Semantic token types that the plugin supports. This list is used as
   * [SemanticTokensClientCapabilities.tokenTypes](https://microsoft.github.io/language-server-protocol/specification/#semanticTokensClientCapabilities)
   * when sending the [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize) request.
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
   * Semantic token modifiers that the plugin supports. This list is used as
   * [SemanticTokensClientCapabilities.tokenModifiers](https://microsoft.github.io/language-server-protocol/specification/#semanticTokensClientCapabilities)
   * when sending the [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize) request.
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
   * Plugins may need to override this function to make sure that all token types supported by the LSP server
   * are reasonably mapped to [TextAttributeKeys][TextAttributesKey].
   *
   * Basic syntax highlighting may be provided without using information from the LSP server, but for example, via TextMate bundles.
   * In this case, it's safe to return `null` from this function for the corresponding token types.
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
