package org.cangnova.cangjie.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionParameters
import org.cangnova.cangjie.completion.addingPolicy.PassDirectlyPolicy


import org.cangnova.cangjie.completion.addingPolicy.PolicyController




class ImmediateExecutor(override val parameters: CompletionParameters,
                        override val policyController: PolicyController
) : SuggestionGeneratorExecutor {
  override fun createNoneKindPolicy() = PassDirectlyPolicy()

  override fun executeAll() {}

  override fun pass(suggestionGenerator: SuggestionGenerator) {
    suggestionGenerator.generateCompletionVariants()
  }
}