package com.linqingying.cangjie.ide.completion.back

import com.intellij.codeInsight.completion.CompletionResultSet

class LookupElementSink(
    private val resultSet: CompletionResultSet,
    private val parameters: CangJieCompletionParameters,
    private val groupPriority: Int = 0,
)
