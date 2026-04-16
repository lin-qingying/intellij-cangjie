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

package org.cangnova.cangjie.debugger.protobuf.console

import com.intellij.openapi.util.Key

/**
 * 控制台输出提供者接口
 *
 * 用于向控制台输出内容的抽象接口，支持依赖注入
 * 这允许不同的组件向同一个控制台输出，而不需要直接依赖具体的控制台实现
 */
interface ConsoleOutputProvider {
    /**
     * 向控制台打印文本
     *
     * @param text 要打印的文本内容
     * @param outputType 输出类型键（stdout, stderr等）
     */
    fun printToConsole(text: String, outputType: Key<*>)
}

/**
 * 空实现 - 用于不需要输出的场景
 */
object NoOpConsoleOutputProvider : ConsoleOutputProvider {
    override fun printToConsole(text: String, outputType: Key<*>) {
        // 不做任何事
    }
}