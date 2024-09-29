package com.huawei.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightVisitor

class CangJieHighlightVisitor : AbstractCangJieHighlightVisitor() {


    override fun clone(): HighlightVisitor = CangJieHighlightVisitor()


}
