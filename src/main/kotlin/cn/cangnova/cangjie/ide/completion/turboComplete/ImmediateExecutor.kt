// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cn.cangnova.cangjie.ide.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionParameters
import cn.cangnova.cangjie.ide.completion.addingPolicy.PassDirectlyPolicy


import cn.cangnova.cangjie.ide.completion.addingPolicy.PolicyController




class ImmediateExecutor(override val parameters: CompletionParameters,
                        override val policyController: PolicyController
) : SuggestionGeneratorExecutor {
  override fun createNoneKindPolicy() = PassDirectlyPolicy()

  override fun executeAll() {}

  override fun pass(suggestionGenerator: SuggestionGenerator) {
    suggestionGenerator.generateCompletionVariants()
  }
}