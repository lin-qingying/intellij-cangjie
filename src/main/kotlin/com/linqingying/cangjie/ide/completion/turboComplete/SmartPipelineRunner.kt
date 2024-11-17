/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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
