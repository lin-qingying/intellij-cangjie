/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.debugger.core.EvaluationContext
import org.cangnova.cangjie.debugger.core.EvaluationType
import org.cangnova.cangjie.debugger.core.Variable

/**
 * 仓颉表达式求值器
 *
 * 处理调试器中的表达式求值
 */
class CangJieEvaluator(
    private val debugProcess: CangJieDebugProcess,
    private val frameId: Long,
    private val threadId: Long
) : XDebuggerEvaluator() {

    companion object {
        private val LOG = Logger.getInstance(CangJieEvaluator::class.java)
    }

    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val context = EvaluationContext(
                    frameId = frameId,
                    threadId = threadId,
                    type = EvaluationType.WATCH
                )

                val result = runBlocking {
                    debugProcess.getEvaluationService()
                        .evaluate(expression, context)
                        .getOrThrow()
                }

                val variable = Variable(
                    name = expression,
                    value = result.value,
                    type = result.type ?: "",
                    variablesReference = result.variablesReference
                )

                callback.evaluated(CangJieVariable(debugProcess, variable))
            } catch (e: Exception) {
                LOG.error("Failed to evaluate expression: $expression", e)
                callback.errorOccurred(e.message ?: "Evaluation failed")
            }
        }
    }
}