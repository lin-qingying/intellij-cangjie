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

import org.cangnova.cangjie.protodebugger.ipc.DebuggerCommandTimedOutException

/**
 * 调试器表达式求值超时异常类
 *
 * 该异常类用于表示调试器表达式求值操作超时的情况。
 * 当表达式过于复杂或执行时间过长时，调试器会抛出此异常以防止无限等待。
 *
 * 使用场景：
 * - 复杂表达式求值超时
 * - 死循环或无限递归的检测
 * - 调试器响应时间控制
 * - 防止调试器阻塞
 *
 * @param expression 导致超时的表达式字符串
 */
class DebuggerEvaluationTimedOutException(val expression: String) :
    DebuggerCommandTimedOutException("Evaluation timed out: $expression")
