package com.huawei.cangjie1.doc.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.TokenSet

private val CDOC_TOKENS = TokenSet.create(CDocTokens.TEXT, CDocTokens.CODE_BLOCK_TEXT)

class CDocLexer : MergingLexerAdapter(FlexAdapter(_CDocLexer()), CDOC_TOKENS)
