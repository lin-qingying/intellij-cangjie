/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.dap.core

/**
 * 表达式求值引擎接口
 *
 * 负责表达式的求值、验证和补全
 */
interface EvaluationEngine {

    /**
     * 求值表达式
     */
    suspend fun evaluate(
        expression: String,
        context: EvaluationContext
    ): Result<EvaluationResult>

    /**
     * 验证表达式语法
     */
    fun validate(expression: String): ValidationResult

    /**
     * 获取表达式补全建议
     */
    suspend fun getCompletions(
        expression: String,
        position: Int,
        context: EvaluationContext
    ): Result<List<CompletionItem>>
}

/**
 * 求值上下文
 */
data class EvaluationContext(
    val frameId: Long,
    val threadId: Long,
    val type: EvaluationType
)

/**
 * 验证结果
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * 补全项
 */
data class CompletionItem(
    val label: String,
    val detail: String?,
    val documentation: String?
)