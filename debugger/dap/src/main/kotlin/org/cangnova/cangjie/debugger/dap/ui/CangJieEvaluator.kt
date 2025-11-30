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

package org.cangnova.cangjie.debugger.dap.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.ExpressionInfo
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.cangnova.cangjie.debugger.dap.core.EvaluationContext
import org.cangnova.cangjie.debugger.dap.core.EvaluationType
import org.cangnova.cangjie.debugger.dap.core.Variable
import org.cangnova.cangjie.psi.psiUtil.findEvaluatableExpression

/**
 * 仓颉表达式求值器
 *
 * 处理调试器中的表达式求值（完全异步）
 */
class CangJieEvaluator(
    private val debugProcess: DapDebugProcess,
    private val frameId: Long,
    private val threadId: Long
) : XDebuggerEvaluator() {

    companion object {
        private val LOG = Logger.getInstance(CangJieEvaluator::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            scope.launch {
                try {
                    val session = debugProcess.getManagedSession()
                    if (session == null) {
                        callback.errorOccurred("Debug session not initialized")
                        return@launch
                    }

                    val context = EvaluationContext(
                        frameId = frameId,
                        threadId = threadId,
                        type = EvaluationType.WATCH
                    )

                    val result = session.evaluationEngine
                        .evaluate(expression, context)
                        .getOrThrow()

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

    /**
     * 获取在指定位置可以被求值的表达式范围
     *
     * 这个方法对于实现鼠标悬浮显示变量值的功能至关重要。
     * 当用户将鼠标悬浮在代码上时，IDE会调用此方法来确定哪个表达式应该被求值。
     *
     * @param project 当前项目
     * @param document 文档对象
     * @param offset 光标在文档中的偏移量
     * @param sideEffectsAllowed 是否允许有副作用的表达式（如函数调用）
     * @return 可以被求值的表达式的文本范围，如果没有有效表达式则返回null
     */
    override fun getExpressionRangeAtOffset(
        project: Project,
        document: Document,
        offset: Int,
        sideEffectsAllowed: Boolean
    ): TextRange? {
        // 获取PSI文件
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null

        // 查找偏移量处的元素
        val element = psiFile.findElementAt(offset) ?: return null

        // 查找包含该元素的表达式
        val expression = element.findEvaluatableExpression( sideEffectsAllowed)
        if (expression != null) {
            return expression.textRange
        }

        return null
    }
}