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

package cn.cangnova.cangjie.types.expressions

import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.ValueArgument
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.CangJieTypeInfo
/**
 * 表达式类型检查接口，提供多种类型信息获取方法
 */
interface ExpressionTypingFacade {
    /**
     * 安全获取表达式的类型信息，适用于任何情况
     * @param expression 待检查的表达式
     * @param context 表达式类型检查上下文，可能为空
     * @return 表达式的类型信息
     */
    fun safeGetTypeInfo(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo

    /**
     * 获取表达式的类型信息，不保证安全性，使用时需注意上下文
     * @param expression 待检查的表达式
     * @param context 表达式类型检查上下文，可能为空
     * @return 表达式的类型信息
     */
    fun getTypeInfo(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo

    /**
     * 该方法只会解析枚举类型的表达式类型信息
     * @param expression 待检查的表达式，应为枚举类型
     * @param context 表达式类型检查上下文，可能为空
     * @return 表达式的类型信息
     */
    fun getTypeInfoByEnum(expression: CjExpression, context: ExpressionTypingContext): CangJieTypeInfo

    /**
     * 该方法只会解析case枚举类型的表达式类型信息
     * @param expression 待检查的表达式，应为case枚举类型
     * @param context 表达式类型检查上下文，可能为空
     * @param argument 枚举参数，用于处理多枚举情况
     * @param isReportError 是否报告错误，用于控制错误处理逻辑
     * @return 表达式的类型信息
     */

    fun getTypeInfoByCaseEnum(
        expression: CjExpression, argument: List<  ValueArgument>, context: ExpressionTypingContext,
        isReportError: Boolean
    ): CangJieTypeInfo

    /**
     * 重载方法，简化调用过程
     * @param expression 待检查的表达式，应为case枚举类型
     * @param argument 枚举参数，用于处理多枚举情况
     * @param context 表达式类型检查上下文，可能为空
     * @return 表达式的类型信息
     */
    fun getTypeInfoByCaseEnum(
        expression: CjExpression, argument: List<ValueArgument>, context: ExpressionTypingContext

    ): CangJieTypeInfo {
        return getTypeInfoByCaseEnum(expression, argument, context, true)
    }

    /**
     * 获取表达式的类型信息，根据是否为声明语句调整类型检查逻辑
     * @param expression 待检查的表达式
     * @param context 表达式类型检查上下文，可能为空
     * @param isStatement 表达式是否为声明语句
     * @return 表达式的类型信息
     */
    fun getTypeInfo(expression: CjExpression, context: ExpressionTypingContext, isStatement: Boolean): CangJieTypeInfo
    fun getTypeInfo(
        scope: LexicalScope,
        function: CjExpression,

        dataFlowInfo: DataFlowInfo,
        expectedReturnType: CangJieType?,
        trace: BindingTrace,
        localContext: ExpressionTypingContext?
    ): CangJieTypeInfo

}
