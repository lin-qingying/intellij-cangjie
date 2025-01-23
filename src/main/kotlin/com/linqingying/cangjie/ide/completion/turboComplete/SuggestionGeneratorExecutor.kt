package com.linqingying.cangjie.ide.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionParameters

import com.linqingying.cangjie.ide.completion.addingPolicy.PolicyController





interface SuggestionGeneratorExecutor : SuggestionGeneratorConsumer {
  val parameters: CompletionParameters
  val policyController: PolicyController

  fun createNoneKindPolicy(): ElementsAddingPolicy

  fun executeAll()
}