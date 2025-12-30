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

package org.cangnova.cangjie.utils

import com.sun.jdi.AbsentInformationException
import com.sun.jdi.InternalException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Method

/**
 * 安全地获取方法的参数列表
 *
 * 在调试过程中，获取方法参数信息可能失败（例如调试信息缺失或 JVM 内部错误）。
 * 此函数提供了一种安全的方式来获取参数列表，失败时返回 null 而不是抛出异常。
 *
 * **使用场景**：
 * - 调试器中展示方法参数
 * - 变量视图中显示局部变量
 * - 表达式求值器中访问参数值
 *
 * **可能返回 null 的情况**：
 * - 调试信息不完整（编译时未生成调试信息）
 * - JVM 内部错误
 * - 不支持的调试操作
 *
 * @receiver Method JDI 方法对象
 * @return List<LocalVariable>? 参数列表，如果无法获取则返回 null
 */
fun Method.safeArguments(): List<LocalVariable>? {
    return wrapAbsentInformationException { arguments() }
}

/**
 * 包装可能抛出调试信息异常的代码块
 *
 * 此函数捕获以下调试过程中常见的异常：
 * - [AbsentInformationException] - 调试信息缺失（编译时未包含调试符号）
 * - [InternalException] - JVM 调试器内部错误
 * - [UnsupportedOperationException] - 不支持的调试操作
 *
 * **设计理念**：
 * 调试器应该尽可能鲁棒，即使部分信息无法获取，也应该继续提供其他可用的调试功能。
 * 因此，对于非致命的调试信息获取失败，返回 null 而不是让整个调试会话崩溃。
 *
 * @param T 返回值类型
 * @param block 可能抛出异常的代码块
 * @return T? 代码块的执行结果，如果抛出异常则返回 null
 */
private inline fun <T> wrapAbsentInformationException(block: () -> T): T? {
    return try {
        block()
    } catch (e: AbsentInformationException) {
        null
    } /*catch (e: AbsentInformationEvaluateException) {
        null
    } */ catch (e: InternalException) {
        null
    } catch (e: UnsupportedOperationException) {
        null
    }
}