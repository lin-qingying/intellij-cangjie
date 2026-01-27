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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.name.Name

/**
 * 仓颉调用接口
 *
 * 表示源代码中的一个调用表达式（函数调用、变量访问、可调用引用等）
 * 这是解析原子（ResolutionAtom）的一个特化，包含了调用的所有语法信息
 */
interface CangJieCall : ResolutionAtom {
    /** 调用类型（函数调用、变量访问、枚举访问等） */
    val callKind: CangJieCallKind

    /**
     * 显式接收者
     *
     * 例如：
     * - `obj.foo()` 中的 `obj`
     * - `Class.staticMethod()` 中的 `Class`
     */
    val explicitReceiver: ReceiverCangJieCallArgument?

    /**
     * 调用扩展的分发接收者
     *
     * 用于处理扩展函数的 invoke 调用
     * 例如：`a.(foo)()` 中的 `(foo)` 是 dispatchReceiverForInvokeExtension
     */
    val dispatchReceiverForInvokeExtension: ReceiverCangJieCallArgument? get() = null

    /** 调用的名称（函数名、变量名等） */
    val name: Name

    /**
     * 上层原子的类型参数
     *
     * 用于处理嵌套泛型调用的情况
     */
    val topTypeArguments: List<TypeArgument>

    /**
     * 类型参数列表
     *
     * 例如：`foo<Int, String>()` 中的 `<Int, String>`
     */
    val typeArguments: List<TypeArgument>

    /**
     * 括号内的参数列表
     *
     * 例如：`foo(1, 2, "hello")` 中的 `1, 2, "hello"`
     */
    val argumentsInParenthesis: List<CangJieCallArgument>

    /**
     * 外部参数（通常是 lambda 表达式）
     *
     * 例如：`foo(1, 2) { x -> x * 2 }` 中的 `{ x -> x * 2 }`
     */
    val externalArgument: CangJieCallArgument?

    /**
     * 是否是隐式 invoke 调用
     *
     * 例如：对于实现了 invoke 操作符的对象 `obj`，`obj()` 是隐式 invoke 调用
     */
    val isForImplicitInvoke: Boolean
}

/**
 * 检查接收者参数的不变性约束
 *
 * 确保接收者参数满足以下条件：
 * 1. 不能是展开参数（spread）
 * 2. 不能有参数名
 */
private fun SimpleCangJieCallArgument.checkReceiverInvariants() {
    assert(!isSpread) {
        "Receiver cannot be a spread: $this"
    }
    assert(argumentName == null) {
        "Argument name should be null for receiver: $this, but it is $argumentName"
    }
}

/**
 * 检查调用的不变性约束
 *
 * 验证调用对象的各个组成部分是否符合语义规则，
 * 不同类型的调用有不同的约束条件
 */
fun CangJieCall.checkCallInvariants() {
    // 显式接收者不能是 lambda 或可调用引用
    assert(explicitReceiver !is LambdaCangJieCallArgument && explicitReceiver !is CallableReferenceCangJieCallArgument) {
        "Lambda argument or callable reference is not allowed as explicit receiver: $explicitReceiver"
    }

    // 检查接收者参数的不变性
    (explicitReceiver as? SimpleCangJieCallArgument)?.checkReceiverInvariants()
    (dispatchReceiverForInvokeExtension as? SimpleCangJieCallArgument)?.checkReceiverInvariants()

    // 根据调用类型检查不同的约束
    when (callKind) {
        // 函数调用和 invoke 调用的约束
        CangJieCallKind.FUNCTION, CangJieCallKind.INVOKE -> {
            // 外部参数不能是展开参数
            assert(externalArgument == null || !externalArgument!!.isSpread) {
                "External argument cannot nave spread element: $externalArgument"
            }
            // 外部参数不能有参数名
            assert(externalArgument?.argumentName == null) {
                "Illegal external argument with name: $externalArgument"
            }
            // invoke 的分发接收者不能是安全调用（?.）
            assert(dispatchReceiverForInvokeExtension == null || !dispatchReceiverForInvokeExtension!!.isSafeCall) {
                "Dispatch receiver for invoke cannot be safe: $dispatchReceiverForInvokeExtension"
            }
        }

        // 变量访问的约束
        CangJieCallKind.VARIABLE -> {
            // 变量访问不能有外部参数
            assert(externalArgument == null) {
                "External argument is not allowed not for function call: $externalArgument."
            }
            // 变量访问不能有括号内的参数
            assert(argumentsInParenthesis.isEmpty()) {
                "Arguments in parenthesis should be empty for not function call: $this "
            }
            // 变量访问不能有 invoke 分发接收者
            assert(dispatchReceiverForInvokeExtension == null) {
                "Dispatch receiver for invoke should be null for not function call: $dispatchReceiverForInvokeExtension"
            }
        }

        // 枚举 case 访问的约束
        CangJieCallKind.CASE_ENUM -> {
            // 枚举 case 访问暂无特殊约束
        }

        // 枚举构造函数调用的约束
        CangJieCallKind.ENUM_CONSTRUCTOR -> {
            // 枚举构造函数暂无特殊约束
        }

        // 可调用引用的约束
        CangJieCallKind.CALLABLE_REFERENCE -> {
            // 可调用引用不能有值参数
            assert(argumentsInParenthesis.isEmpty()) {
                "Callable references can't have value arguments"
            }
            // 注释：可调用引用可以有显式类型参数（与 Kotlin 不同）
            // assert(typeArguments.isEmpty()) {
            //     "Callable references can't have explicit type arguments"
            // }
            // 可调用引用不能有外部参数
            assert(externalArgument == null) {
                "External argument is not allowed not for function call: $externalArgument."
            }
        }

        // 不支持的调用类型
        CangJieCallKind.UNSUPPORTED -> error("Call with UNSUPPORTED kind")
    }
}