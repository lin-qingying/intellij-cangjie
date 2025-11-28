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

package org.cangnova.cangjie.dapdebugger.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.frame.*
import kotlinx.coroutines.*
import org.cangnova.cangjie.dapdebugger.core.*
import org.cangnova.cangjie.dapdebugger.service.EvaluationService
import org.cangnova.cangjie.dapdebugger.service.VariableService

/**
 * 变量查看器
 *
 * 提供变量的层级展示和管理功能
 */
class VariableViewer(
    private val project: Project,
    private val variableService: VariableService,
    private val evaluationService: EvaluationService
) {

    companion object {
        private val LOG = Logger.getInstance(VariableViewer::class.java)
    }

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 获取指定栈帧的所有变量
     */
    suspend fun getFrameVariables(frame: StackFrameInfo): Result<List<VariableGroup>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting variables for frame: ${frame.id}")

                // 获取作用域
                val scopesResult = variableService.getScopes(frame.id)
                if (scopesResult.isFailure) {
                    return@withContext Result.failure(scopesResult.exceptionOrNull()!!)
                }

                val scopes = scopesResult.getOrThrow()
                val variableGroups = mutableListOf<VariableGroup>()

                // 为每个作用域获取变量
                scopes.forEach { scope ->
                    val variablesResult = variableService.getVariables(scope.variablesReference)
                    if (variablesResult.isSuccess) {
                        val variables = variablesResult.getOrThrow()
                        variableGroups.add(
                            VariableGroup(
                                name = scope.name,
                                variables = variables,
                                scope = scope
                            )
                        )
                    }
                }

                LOG.debug("Retrieved ${variableGroups.size} variable groups with ${variableGroups.sumOf { it.variables.size }} total variables")
                Result.success(variableGroups)
            } catch (e: Exception) {
                LOG.error("Failed to get frame variables", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 展开变量获取子变量
     */
    suspend fun expandVariable(variable: Variable): Result<List<Variable>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Expanding variable: ${variable.name}")

                if (variable.variablesReference == 0L) {
                    // 没有子变量
                    return@withContext Result.success(emptyList())
                }

                val childVariables = variableService.getVariables(variable.variablesReference).getOrThrow()

                LOG.debug("Expanded ${variable.name} to ${childVariables.size} child variables")
                Result.success(childVariables)
            } catch (e: Exception) {
                LOG.error("Failed to expand variable: ${variable.name}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 设置变量值
     */
    suspend fun setVariableValue(variable: Variable, newValue: String): Result<Variable> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Setting variable value: ${variable.name} = $newValue")

                val updatedVariable = variableService.setVariable(
                    variablesReference = variable.variablesReference,
                    name = variable.name,
                    value = newValue
                ).getOrThrow()

                LOG.debug("Variable value updated successfully")
                Result.success(updatedVariable)
            } catch (e: Exception) {
                LOG.error("Failed to set variable value: ${variable.name} = $newValue", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 计算表达式
     */
    suspend fun evaluateExpression(expression: String, frameId: Long): Result<Variable> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Evaluating expression: $expression")

                val context = EvaluationContext(
                    frameId = frameId,
                    threadId = 1L, // 默认线程ID
                    type = EvaluationType.WATCH
                )

                val result = evaluationService.evaluate(expression, context).getOrThrow()

                val variable = Variable(
                    name = expression,
                    value = result.value,
                    type = result.type ?: "unknown",
                    variablesReference = result.variablesReference,
                    presentationHint = result.presentationHint?.kind
                )

                LOG.debug("Expression evaluated: $expression = ${result.value}")
                Result.success(variable)
            } catch (e: Exception) {
                LOG.error("Failed to evaluate expression: $expression", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 创建 XValue 节点用于 UI 显示
     */
    fun createXValueNode(variable: Variable): XValue {
        return CangJieVariableNode(variable)
    }

    /**
     * 清理资源
     */
    fun dispose() {
        scope.cancel()
    }
}

/**
 * 变量组
 */
data class VariableGroup(
    val name: String,
    val variables: List<Variable>,
    val scope: Scope
)

/**
 * 仓颉变量节点
 *
 * 实现 XNamedValue 接口用于 IntelliJ 调试器集成
 */
class CangJieVariableNode(
    private val variable: Variable
) : XNamedValue(variable.name) {

    private val LOG = Logger.getInstance(CangJieVariableNode::class.java)

    override fun computeChildren(node: XCompositeNode) {
        if (variable.variablesReference <= 0) {
            // 没有子变量，不添加任何内容
            return
        }

        // 暂时不实现异步加载，避免复杂性问题
        // 可以在后续版本中完善
    }

    override fun canNavigateToSource(): Boolean = false

    override fun canNavigateToTypeSource(): Boolean = false

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        // 设置变量值的显示
        node.setPresentation(null, null, variable.value, variable.variablesReference > 0)
    }
}