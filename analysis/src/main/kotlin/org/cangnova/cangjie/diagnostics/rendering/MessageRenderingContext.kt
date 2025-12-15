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

package org.cangnova.cangjie.diagnostics.rendering

import org.cangnova.cangjie.diagnostics.Diagnostic

/**
 * 消息渲染上下文
 *
 * 定义了在不同场景下如何渲染诊断消息的策略
 */
enum class MessageRenderingContext {
    /**
     * 命令行编译器输出
     * - 纯文本格式
     * - 简洁的错误信息
     * - 适用于 CLI、构建日志
     */
    COMPILER,

    /**
     * IDE 用户界面
     * - 支持 HTML 富文本
     * - 更详细的错误描述
     * - 可能包含表格、列表等格式化内容
     * - 适用于代码高亮悬浮提示、问题窗口
     */
    IDE,

    /**
     * 问题工具窗口
     * - 介于 COMPILER 和 IDE 之间
     * - 简洁但可能包含基本格式
     * - 适用于问题列表、构建输出窗口
     */
    PROBLEMS_VIEW
}

/**
 * 诊断消息渲染器
 *
 * 支持多场景的消息渲染策略
 */
interface DiagnosticMessageRenderer {
    /**
     * 渲染诊断消息
     *
     * @param diagnostic 诊断对象
     * @param context 渲染上下文（命令行、IDE UI 等）
     * @return 渲染后的消息文本
     */
    fun render(diagnostic: Diagnostic, context: MessageRenderingContext = MessageRenderingContext.COMPILER): String

    companion object {
        /**
         * 默认渲染器实例
         */
        val DEFAULT: DiagnosticMessageRenderer = DefaultErrorMessages

        /**
         * IDE 渲染器实例
         */
        val IDE: DiagnosticMessageRenderer = IdeErrorMessages
    }
}
