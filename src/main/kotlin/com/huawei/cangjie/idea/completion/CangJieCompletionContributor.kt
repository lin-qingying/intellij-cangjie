package com.huawei.cangjie.idea.completion

import com.huawei.cangjie.lexer.CjTokens
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder

class CangJieCompletionContributor: CompletionContributor() {


    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {


//        val file = parameters.originalFile

//        file.children.forEach {
//            println("child: $it")
//        }

        result.addAllElements(CjTokens.KEYWORDALL.types.map { LookupElementBuilder.create(it.toString()) })
        super.fillCompletionVariants(parameters, result)
    }



}
