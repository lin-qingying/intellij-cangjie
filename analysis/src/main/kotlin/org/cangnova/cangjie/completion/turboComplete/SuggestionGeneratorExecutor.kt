package org.cangnova.cangjie.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionParameters

import org.cangnova.cangjie.completion.addingPolicy.PolicyController





interface SuggestionGeneratorExecutor : SuggestionGeneratorConsumer {
  val parameters: CompletionParameters
  val policyController: PolicyController

  fun createNoneKindPolicy(): ElementsAddingPolicy

  fun executeAll()
}