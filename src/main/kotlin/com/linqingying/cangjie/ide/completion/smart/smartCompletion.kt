package com.linqingying.cangjie.ide.completion.smart

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjBinaryExpressionWithTypeRHS
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjTypeReference
import com.linqingying.cangjie.psi.CjUserType
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.lookup.LookupElement
import java.lang.module.ModuleDescriptor


interface InheritanceItemsSearcher {
    fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit)
}

  class SmartCompletion(

) {


    companion object {
        val OLD_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("nonFunctionReplacementOffset")
        val MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("multipleArgumentsReplacementOffset")
    }
}
