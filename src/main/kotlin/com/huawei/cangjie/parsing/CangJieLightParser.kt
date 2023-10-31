package com.huawei.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.util.diff.FlyweightCapableTreeStructure


object CangJieLightParser {
    fun parse(builder: PsiBuilder): FlyweightCapableTreeStructure<LighterASTNode> {
        val ktParsing: CangJieParsing = CangJieParsing.createForTopLevelNonLazy(
            SemanticWhitespaceAwarePsiBuilderImpl(builder)
        )
        ktParsing.parseFile()
        return builder.lightTree
    }
}

