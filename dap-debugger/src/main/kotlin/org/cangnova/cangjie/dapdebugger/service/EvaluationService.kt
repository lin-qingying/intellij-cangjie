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

package org.cangnova.cangjie.dapdebugger.service

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.dapdebugger.core.*
import org.cangnova.cangjie.dapdebugger.exception.EvaluationException

/**
 * 表达式求值服务
 *
 * 提供表达式求值、验证和补全功能
 */
class EvaluationService(
    private val adapter: DebugAdapter
) : EvaluationEngine {

    companion object {
        private val LOG = Logger.getInstance(EvaluationService::class.java)
    }

    override suspend fun evaluate(
        expression: String,
        context: EvaluationContext
    ): Result<EvaluationResult> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Evaluating expression: $expression in context: $context")

                val result = adapter.evaluate(
                    expression = expression,
                    frameId = context.frameId,
                    context = context.type
                ).getOrThrow()

                LOG.debug("Evaluation result: ${result.value}")
                Result.success(result)
            } catch (e: Exception) {
                LOG.error("Failed to evaluate expression: $expression", e)
                Result.failure(EvaluationException("Evaluation failed", e))
            }
        }
    }

    override fun validate(expression: String): ValidationResult {
        // 实现表达式语法验证
        return when {
            expression.isBlank() -> {
                ValidationResult.Invalid("Expression cannot be empty")
            }

            expression.length > 1000 -> {
                ValidationResult.Invalid("Expression is too long")
            }

            else -> {
                // TODO: 实现更详细的语法验证
                ValidationResult.Valid
            }
        }
    }

    override suspend fun getCompletions(
        expression: String,
        position: Int,
        context: EvaluationContext
    ): Result<List<CompletionItem>> {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: 实现代码补全
                // 这需要调试适配器支持 completions 请求
                LOG.debug("Getting completions for: $expression at position $position")
                Result.success(emptyList())
            } catch (e: Exception) {
                LOG.error("Failed to get completions", e)
                Result.failure(EvaluationException("Get completions failed", e))
            }
        }
    }
}