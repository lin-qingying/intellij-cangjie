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

package org.cangnova.cangjie.cli.messages

import java.io.PrintStream

/**
 * 打印消息收集器
 *
 * 将编译器消息直接打印到指定的输出流。
 * 支持详细模式，可以过滤掉详细级别的消息。
 *
 * @property errStream 错误输出流，用于输出消息
 * @property messageRenderer 消息渲染器，负责格式化消息
 * @property verbose 是否启用详细模式
 */
class PrintingMessageCollector(
    private val errStream: PrintStream,
    private val messageRenderer: MessageRenderer,
    private val verbose: Boolean
) : MessageCollector {

    /** 是否有错误 */
    private var _hasErrors = false

    override fun clear() {
        // 什么都不做，消息已经被报告了
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {
        // 如果不是详细模式，跳过详细级别的消息
        if (!verbose && CompilerMessageSeverity.VERBOSE.contains(severity)) return

        // 更新错误状态
        _hasErrors = _hasErrors || severity.isError

        // 渲染并打印消息
        errStream.println(messageRenderer.render(severity, message, location))
    }

    override fun hasErrors(): Boolean = _hasErrors
}
