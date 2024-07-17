package com.huawei.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.util.diff.FlyweightCapableTreeStructure


object CangJieLightParser {
    fun parse(builder: PsiBuilder): FlyweightCapableTreeStructure<LighterASTNode> {
        val cjParsing: CangJieParsing = CangJieParsing.createForTopLevelNonLazy(
            SemanticWhitespaceAwarePsiBuilderImpl(builder)
        )
        cjParsing.parseFile()
        return builder.lightTree
    }
}

