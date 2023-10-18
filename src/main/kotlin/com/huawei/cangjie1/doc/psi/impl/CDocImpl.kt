package com.huawei.cangjie1.doc.psi.impl

import com.huawei.cangjie1.doc.lexer.CDocTokens
import com.huawei.cangjie1.doc.psi.CDoc

import com.intellij.psi.impl.source.tree.LazyParseablePsiElement

class CDocImpl(buffer: CharSequence?):LazyParseablePsiElement(CDocTokens.CDOC, buffer),CDoc
