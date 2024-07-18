package com.huawei.cangjie.idea.completion.back.smart

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjBinaryExpressionWithTypeRHS
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjTypeReference
import com.huawei.cangjie.psi.CjUserType
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
