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

package org.cangnova.cangjie.protodebugger.exception

/**
 * 跳转到当前函数外部行异常类
 *
 * 该异常类用于表示调试器尝试跳转到当前函数外部代码行的非法操作。
 * 这种操作在调试过程中是不被允许的，因为它会破坏调用栈的完整性和程序的正常执行流程。
 *
 * 使用场景：
 * - 用户尝试跳转到其他函数的代码行
 * - "运行到光标处"功能的目标位置在当前函数之外
 * - 调试器导航操作的合法性验证
 * - 防止调试状态不一致的情况
 *
 * 主要原因：
 * - 跨函数跳转会绕过正常的函数调用机制
 * - 可能导致栈帧信息不匹配
 * - 违反了程序执行的逻辑流程
 * - 可能引起不可预测的行为
 *
 * @param s 异常的详细消息，描述具体的错误情况
 * @param throwable 导致异常的根本原因，可能为null
 */
class JumpToLineOutsideCurrentFunctionException : DebuggerCommandException {
    /**
     * 构造函数 - 仅包含错误消息
     *
     * @param s 异常的详细消息，描述跳转操作的错误情况
     */
    constructor(s: String) : super(s)

    /**
     * 构造函数 - 包含错误消息和根本原因
     *
     * @param s 异常的详细消息，描述跳转操作的错误情况
     * @param throwable 导致异常的根本原因异常
     */
    constructor(s: String, throwable: Throwable) : super(s, throwable)
}