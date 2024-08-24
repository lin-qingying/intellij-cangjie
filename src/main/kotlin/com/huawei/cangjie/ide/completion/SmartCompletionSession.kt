package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet

class SmartCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    resultSet: CompletionResultSet
) : CompletionSession(configuration, parameters, resultSet) {
    override fun doComplete() {

    }

}
