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
 * 默认错误消息渲染器
 *
 * 用于命令行编译器和构建日志输出的基础文本渲染。
 *
 * ## 渲染策略
 *
 * - **纯文本格式**: 不包含 HTML 或其他富文本标记
 * - **简洁明了**: 直接展示错误信息，不添加额外装饰
 * - **回退兼容**: 作为所有渲染场景的基础实现
 *
 * ## 使用场景
 *
 * 1. 命令行编译器输出 (cjc)
 * 2. 构建系统日志 (Gradle, Maven)
 * 3. CI/CD 管道输出
 * 4. 问题工具窗口 (作为 PROBLEMS_VIEW 上下文的回退)
 *
 * ## 扩展点
 *
 * 如需为特定平台或模块添加自定义渲染逻辑，可以：
 * 1. 通过 [DiagnosticRendererRegistry.configure] 注册特定诊断的渲染器
 * 2. 在国际化 Bundle 中定义消息模板
 *
 * @see IdeErrorMessages IDE 富文本渲染器
 * @see DiagnosticRendererRegistry 渲染器注册中心
 */
object DefaultErrorMessages : DiagnosticMessageRenderer {

    /**
     * 渲染诊断消息为纯文本格式
     *
     * @param diagnostic 诊断对象
     * @param context 渲染上下文（对此渲染器无影响，始终输出纯文本）
     * @return 纯文本错误消息
     */
    override fun render(diagnostic: Diagnostic, context: MessageRenderingContext): String {
        // 优先使用注册表中的渲染器
        val renderer = DiagnosticRendererRegistry.getRenderer(diagnostic.factory)
        if (renderer != null) {
            @Suppress("UNCHECKED_CAST")
            val typedRenderer = renderer as DiagnosticRenderer<Any>
            @Suppress("UNCHECKED_CAST")
            return typedRenderer.render(diagnostic as Any)
        }

        // 回退到工厂的默认渲染器
        diagnostic.factory.defaultRenderer?.let {
            @Suppress("UNCHECKED_CAST")
            val typedRenderer = it as DiagnosticRenderer<Any>
            @Suppress("UNCHECKED_CAST")
            return typedRenderer.render(diagnostic as Any)
        }

        // 最终回退：显式错误
        return "${diagnostic.factory.name} (error: could not render message)"
    }

    /**
     * 便捷方法：直接渲染为默认格式
     */
    fun render(diagnostic: Diagnostic): String = render(diagnostic, MessageRenderingContext.COMPILER)
}
