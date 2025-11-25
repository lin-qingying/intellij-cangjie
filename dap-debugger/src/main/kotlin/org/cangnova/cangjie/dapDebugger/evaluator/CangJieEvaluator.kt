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

package org.cangnova.cangjie.dapDebugger.evaluator

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import org.cangnova.cangjie.dapDebugger.stack.CangJieStackFrame
import org.cangnova.cangjie.dapDebugger.variables.CangJieVariable
import org.eclipse.lsp4j.debug.EvaluateArguments
import org.eclipse.lsp4j.debug.EvaluateArgumentsContext
import org.eclipse.lsp4j.debug.Variable

class CangJieEvaluator(private val stackFrame: CangJieStackFrame) : XDebuggerEvaluator() {

    override fun evaluate(expression: String, callback: XEvaluationCallback, expressionPosition: XSourcePosition?) {
        try {
            stackFrame.debugProcess.getConnection().getServer().evaluate(EvaluateArguments().apply {
                this.expression = expression
                this.frameId = stackFrame.stackFrame.id
                this.context = EvaluateArgumentsContext.WATCH
            }).thenAccept { response ->
                val variable = Variable().apply {
                    name = expression
                    value = response.result
                    type = response.type
                    variablesReference = response.variablesReference
                }
                callback.evaluated(CangJieVariable(stackFrame.debugProcess, expression, variable))
            }.exceptionally { throwable ->
                callback.errorOccurred(throwable.message ?: "Evaluation failed")
                null
            }
        } catch (e: Exception) {
            callback.errorOccurred(e.message ?: "Evaluation failed")
        }
    }
}
