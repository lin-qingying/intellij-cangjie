package com.linqingying.cangjie.ide.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PolicyObeyingResultSet
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.platform.ml.impl.turboComplete.ImmediateExecutor
import com.intellij.platform.ml.impl.turboComplete.KindCollector


interface SmartPipelineRunner {
    fun runPipeline(kindCollector: KindCollector, parameters: CompletionParameters, result: CompletionResultSet)

    private object ImmediatePipelineRunner : SmartPipelineRunner {
        override fun runPipeline(kindCollector: KindCollector, parameters: CompletionParameters, result: CompletionResultSet) {
            if (!kindCollector.shouldBeCalled(parameters)) {
                return
            }
            val policyController = PolicyController(result)
            val obeyingResult = PolicyObeyingResultSet(result, policyController)

            val executor = ImmediateExecutor(parameters, policyController)

            val policyWhileGenerating = executor.createNoneKindPolicy()
            policyController.invokeWithPolicy(policyWhileGenerating) {
                kindCollector.collectKinds(parameters, executor, obeyingResult)
            }
        }
    }

    companion object {
        private val EP_NAME = ExtensionPointName<SmartPipelineRunner>("com.linqingying.cangjie.turboComplete.smartPipelineRunner")

        fun getOneOrDefault(): SmartPipelineRunner {
            val runners = EP_NAME.extensionList
            require(runners.size <= 1) {
                "Found more than one SmartPipelineRunners: ${runners}"
            }
            return runners.firstOrNull() ?: return ImmediatePipelineRunner
        }
    }
}
