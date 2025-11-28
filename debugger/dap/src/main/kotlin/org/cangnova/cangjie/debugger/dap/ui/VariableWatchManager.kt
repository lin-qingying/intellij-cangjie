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

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.debugger.dap.core.EvaluationContext
import org.cangnova.cangjie.debugger.dap.core.EvaluationType
import org.cangnova.cangjie.debugger.dap.core.StackFrameInfo
import org.cangnova.cangjie.debugger.dap.core.Variable
import org.cangnova.cangjie.debugger.dap.session.ManagedDebugSession
import java.util.concurrent.ConcurrentHashMap

/**
 * 变量监视管理器
 *
 * 管理监视变量的表达式和值
 */
class VariableWatchManager(
    private val sessionProvider: () -> ManagedDebugSession?
) {

    companion object {
        private val LOG = Logger.getInstance(VariableWatchManager::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 监视的表达式列表
    private val watchExpressions = MutableStateFlow<List<WatchExpression>>(emptyList())
    val expressions: StateFlow<List<WatchExpression>> = watchExpressions.asStateFlow()

    // 当前变量的缓存
    private val variableCache = ConcurrentHashMap<String, Variable>()

    /**
     * 添加监视表达式
     */
    fun addWatchExpression(expression: String): Boolean {
        return try {
            if (expression.isBlank()) {
                LOG.warn("Cannot add empty watch expression")
                return false
            }

            if (watchExpressions.value.any { it.expression == expression }) {
                LOG.warn("Watch expression already exists: $expression")
                return false
            }

            val newExpression = WatchExpression(
                id = generateId(),
                expression = expression.trim(),
                enabled = true
            )

            val currentList = watchExpressions.value.toMutableList()
            currentList.add(newExpression)
            watchExpressions.value = currentList

            LOG.info("Added watch expression: $expression")
            true
        } catch (e: Exception) {
            LOG.error("Failed to add watch expression: $expression", e)
            false
        }
    }

    /**
     * 移除监视表达式
     */
    fun removeWatchExpression(expressionId: String): Boolean {
        return try {
            val currentList = watchExpressions.value.toMutableList()
            val removed = currentList.removeIf { it.id == expressionId }

            if (removed) {
                watchExpressions.value = currentList
                variableCache.remove(expressionId)
                LOG.info("Removed watch expression: $expressionId")
            }

            removed
        } catch (e: Exception) {
            LOG.error("Failed to remove watch expression: $expressionId", e)
            false
        }
    }

    /**
     * 更新监视表达式
     */
    fun updateWatchExpression(expressionId: String, newExpression: String): Boolean {
        return try {
            if (newExpression.isBlank()) {
                LOG.warn("Cannot update watch expression to empty value")
                return false
            }

            val currentList = watchExpressions.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == expressionId }

            if (index >= 0) {
                currentList[index] = currentList[index].copy(expression = newExpression.trim())
                watchExpressions.value = currentList
                variableCache.remove(expressionId)
                LOG.info("Updated watch expression: $expressionId -> $newExpression")
                true
            } else {
                LOG.warn("Watch expression not found: $expressionId")
                false
            }
        } catch (e: Exception) {
            LOG.error("Failed to update watch expression: $expressionId", e)
            false
        }
    }

    /**
     * 切换表达式启用状态
     */
    fun toggleExpression(expressionId: String): Boolean {
        return try {
            val currentList = watchExpressions.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == expressionId }

            if (index >= 0) {
                currentList[index] = currentList[index].copy(enabled = !currentList[index].enabled)
                watchExpressions.value = currentList
                LOG.info("Toggled watch expression status: $expressionId -> ${currentList[index].enabled}")
                true
            } else {
                LOG.warn("Watch expression not found: $expressionId")
                false
            }
        } catch (e: Exception) {
            LOG.error("Failed to toggle watch expression: $expressionId", e)
            false
        }
    }

    /**
     * 刷新所有监视变量
     */
    suspend fun refreshWatchVariables(frame: StackFrameInfo, threadId: Long): Result<List<WatchVariable>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Refreshing ${watchExpressions.value.size} watch variables")

                val session = sessionProvider()
                if (session == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Debug session not available")
                    )
                }

                val watchVariables = mutableListOf<WatchVariable>()

                watchExpressions.value.forEach { watchExpr ->
                    if (!watchExpr.enabled) {
                        // 跳过禁用的表达式
                        return@forEach
                    }

                    try {
                        val context = EvaluationContext(
                            frameId = frame.id,
                            threadId = threadId,
                            type = EvaluationType.WATCH
                        )

                        val result = session.evaluationEngine.evaluate(watchExpr.expression, context)
                        if (result.isSuccess) {
                            val evalResult = result.getOrThrow()
                            val varValue = Variable(
                                name = watchExpr.expression,
                                value = evalResult.value,
                                type = evalResult.type ?: "",
                                variablesReference = evalResult.variablesReference
                            )
                            variableCache[watchExpr.id] = varValue

                            watchVariables.add(
                                WatchVariable(
                                    id = watchExpr.id,
                                    expression = watchExpr.expression,
                                    variable = varValue,
                                    status = WatchStatus.VALID
                                )
                            )
                        } else {
                            // 处理错误
                            val error = result.exceptionOrNull()?.message ?: "Unknown error"
                            watchVariables.add(
                                WatchVariable(
                                    id = watchExpr.id,
                                    expression = watchExpr.expression,
                                    status = WatchStatus.ERROR,
                                    errorMessage = error
                                )
                            )
                        }
                    } catch (e: Exception) {
                        LOG.error("Failed to evaluate watch expression: ${watchExpr.expression}", e)
                        watchVariables.add(
                            WatchVariable(
                                id = watchExpr.id,
                                expression = watchExpr.expression,
                                status = WatchStatus.ERROR,
                                errorMessage = e.message ?: "Evaluation error"
                            )
                        )
                    }
                }

                LOG.debug("Refreshed ${watchVariables.size} watch variables")
                Result.success(watchVariables)
            } catch (e: Exception) {
                LOG.error("Failed to refresh watch variables", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 清除所有监视表达式
     */
    fun clearAllExpressions() {
        try {
            watchExpressions.value = emptyList()
            variableCache.clear()
            LOG.info("Cleared all watch expressions")
        } catch (e: Exception) {
            LOG.error("Failed to clear watch expressions", e)
        }
    }

    /**
     * 获取缓存的变量
     */
    fun getCachedVariable(expressionId: String): Variable? {
        return variableCache[expressionId]
    }

    private fun generateId(): String {
        return "watch_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * 清理资源
     */
    fun dispose() {
        scope.cancel()
        watchExpressions.value = emptyList()
        variableCache.clear()
    }
}

/**
 * 监视表达式
 */
data class WatchExpression(
    val id: String,
    val expression: String,
    val enabled: Boolean = true
)

/**
 * 监视变量
 */
data class WatchVariable(
    val id: String,
    val expression: String,
    val variable: Variable? = null,
    val status: WatchStatus = WatchStatus.VALID,
    val errorMessage: String? = null
)

/**
 * 监视状态
 */
enum class WatchStatus {
    VALID,       // 有效值
    ERROR,       // 计算错误
    DISABLED     // 已禁用
}