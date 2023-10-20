package com.huawei.cangjie.doc.psi.impl

import com.huawei.cangjie.doc.lexer.CDocTokens
import com.huawei.cangjie.doc.psi.CDoc

import com.intellij.psi.impl.source.tree.LazyParseablePsiElement

class CDocImpl(buffer: CharSequence?):LazyParseablePsiElement(CDocTokens.CDOC, buffer),CDoc
