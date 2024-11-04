package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_GTEQ
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_LT
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_LTEQ
import com.linqingying.cangjie.utils.OperatorNameConventions.CONTAINS
import com.linqingying.cangjie.utils.OperatorNameConventions.EQUALS
import com.linqingying.cangjie.utils.OperatorNameConventions.GET
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_GT

import com.linqingying.cangjie.utils.OperatorNameConventions.INVOKE
import com.linqingying.cangjie.utils.OperatorNameConventions.NOT_EQUALS
import com.linqingying.cangjie.utils.OperatorNameConventions.SET
import com.linqingying.cangjie.utils.exceptions.OperatorConventions
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
