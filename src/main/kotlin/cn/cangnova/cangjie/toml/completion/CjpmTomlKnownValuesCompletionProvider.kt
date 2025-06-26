package cn.cangnova.cangjie.toml.completion

import cn.cangnova.cangjie.toml.StringValueInsertionHandler
import cn.cangnova.cangjie.toml.getClosestKeyValueAncestor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class CjpmTomlKnownValuesCompletionProvider(private val knownValues: List<String>) :
    CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val keyValue = getClosestKeyValueAncestor(parameters.position) ?: return
        result.addAllElements(knownValues.map {
            LookupElementBuilder.create(it).withInsertHandler(
                StringValueInsertionHandler(keyValue)
            )
        })
    }
}