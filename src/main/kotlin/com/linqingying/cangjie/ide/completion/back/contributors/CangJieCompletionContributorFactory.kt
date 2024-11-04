package com.linqingying.cangjie.ide.completion.back.contributors

import com.linqingying.cangjie.ide.completion.back.context.CangJieBasicCompletionContext
import com.intellij.codeInsight.completion.addingPolicy.PolicyController

class CangJieCompletionContributorFactory(
    private val basicContext: CangJieBasicCompletionContext,
    private val resultController: PolicyController,
)
