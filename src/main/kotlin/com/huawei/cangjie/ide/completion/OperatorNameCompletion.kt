package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.OperatorNameConventions
import com.huawei.cangjie.utils.OperatorNameConventions.COMPARE_GTEQ
import com.huawei.cangjie.utils.OperatorNameConventions.COMPARE_LT
import com.huawei.cangjie.utils.OperatorNameConventions.COMPARE_LTEQ
import com.huawei.cangjie.utils.OperatorNameConventions.CONTAINS
import com.huawei.cangjie.utils.OperatorNameConventions.EQUALS
import com.huawei.cangjie.utils.OperatorNameConventions.GET
import com.huawei.cangjie.utils.OperatorNameConventions.COMPARE_GT

import com.huawei.cangjie.utils.OperatorNameConventions.INVOKE
import com.huawei.cangjie.utils.OperatorNameConventions.NOT_EQUALS
import com.huawei.cangjie.utils.OperatorNameConventions.SET
import com.huawei.cangjie.utils.exceptions.OperatorConventions
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

object OperatorNameCompletion {

    private val additionalOperatorPresentation = mapOf(
        SET to "[...] = ...",
        GET to "[...]",
        CONTAINS to "in ",
        COMPARE_GT to ">",
        COMPARE_LT to "<",
                COMPARE_GTEQ to ">=",
                COMPARE_LTEQ to "<=",
        EQUALS to "==",
        NOT_EQUALS to "!=",
        INVOKE to "(...)"
    )

    private fun buildLookupElement(opName: Name): LookupElement {
        val element = LookupElementBuilder.create(opName)

        val symbol =
            (OperatorConventions.getOperationSymbolForName(opName) as? CjSingleValueToken)?.value ?: additionalOperatorPresentation[opName]

        if (symbol != null) return element.withTypeText(symbol)
        return element
    }

    fun doComplete(collector: LookupElementsCollector, descriptorNameFilter: (String) -> Boolean) {
        collector.addElements(OperatorConventions.CONVENTION_NAMES.filter { descriptorNameFilter(it.asString()) }
            .map(this::buildLookupElement))
    }
}
