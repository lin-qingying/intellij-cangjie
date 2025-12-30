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

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.ReadOnly
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.resolve.scopes.receivers.Receiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

/**
 * 函数调用接口
 *
 * 表示仓颉语言中的函数调用表达式，包含调用的各种信息：
 * - 调用操作符（普通调用、安全调用等）
 * - 接收者（显式接收者和分发接收者）
 * - 被调用的表达式
 * - 值参数和类型参数
 * - 调用类型（普通调用、数组访问、invoke 等）
 *
 * 该接口是仓颉语言语义分析的核心抽象，用于表示和解析各种形式的函数调用。
 */
interface Call {
    /**
     * 是否没有值参数
     *
     * 用于标记调用是否不带值参数，例如 `foo()` 或属性访问。
     */
    var noValueArgument: Boolean

    /**
     * 是否没有类型参数
     *
     * 控制 [typeArgumentList] 是否返回空值。
     * 用于标记调用是否不带显式类型参数。
     */
    var noTypeParameter: Boolean

    /**
     * 调用操作符节点
     *
     * 表示调用操作符的 AST 节点，例如：
     * - `.` (普通调用)
     * - `?.` (安全调用)
     * - `::` (引用)
     */
    val callOperationNode: ASTNode?

    /**
     * 是否在语义上等价于安全调用
     *
     * 检查调用操作符是否为安全访问操作符 (`?.`)。
     *
     * @return 如果是安全调用返回 true，否则返回 false
     */
    val isSemanticallyEquivalentToSafeCall: Boolean
        get() = callOperationNode != null && callOperationNode!!.elementType === CjTokens.SAFE_ACCESS

    /**
     * 显式接收者
     *
     * 例如在 `obj.method()` 中，`obj` 是显式接收者。
     * 如果没有显式接收者（例如直接调用 `method()`），则为 null。
     */
    val explicitReceiver: Receiver?

    /**
     * 分发接收者
     *
     * 在方法调用时，实际接收方法调用的对象。
     * 用于处理多重分发和成员函数调用的语义。
     */
    val dispatchReceiver: ReceiverValue?

    /**
     * 被调用的表达式
     *
     * 表示调用的目标，例如函数名、方法引用等。
     */
    val calleeExpression: CjExpression?

    /**
     * 值参数列表
     *
     * 包含所有值参数的列表节点。
     * 如果调用没有参数列表（例如属性访问），则为 null。
     */
    val valueArgumentList: CjValueArgumentList?

    /**
     * 值参数
     *
     * 调用时传递的所有值参数的列表。
     * 该列表是只读的，不应被修改。
     */
    @get:ReadOnly
    val valueArguments: List<ValueArgument>

    /**
     * Lambda 参数
     *
     * 作为参数传递的所有 lambda 表达式列表。
     * 该列表是只读的，不应被修改。
     */
    @get:ReadOnly
    val functionLiteralArguments: List<LambdaArgument>

    /**
     * 类型参数
     *
     * 调用时指定的所有类型参数列表。
     * 该列表是只读的，不应被修改。
     */
    @get:ReadOnly
    val typeArguments: List<CjTypeProjection>

    /**
     * 类型参数列表
     *
     * 包含所有类型参数的列表节点。
     * 如果调用没有显式类型参数，则为 null。
     */
    val typeArgumentList: CjTypeArgumentList?

    /**
     * 调用元素
     *
     * 表示整个调用的 PSI 元素。
     */
    val callElement: CjElement

    /**
     * 调用类型枚举
     *
     * 定义仓颉语言中支持的各种调用类型：
     * - [DEFAULT]: 普通函数或方法调用
     * - [ARRAY_GET_METHOD]: 数组取值操作 `array[index]`
     * - [ARRAY_SET_METHOD]: 数组赋值操作 `array[index] = value`
     * - [INVOKE]: 调用重载的 invoke 操作符
     * - [CONTAINS]: 调用 contains 操作符（用于 `in` 表达式）
     */
    enum class CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE, CONTAINS
    }

    /**
     * 调用类型
     *
     * 标识当前调用的类型，用于区分不同语义的调用形式。
     */
    val callType: CallType
}
