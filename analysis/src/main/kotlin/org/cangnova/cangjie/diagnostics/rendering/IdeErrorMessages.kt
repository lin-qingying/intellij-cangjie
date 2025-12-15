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
import java.util.concurrent.ConcurrentHashMap

/**
 * IDE 错误消息渲染器
 *
 * 为 IntelliJ IDE 环境提供富文本格式的诊断消息渲染。
 *
 * ## 渲染策略
 *
 * - **HTML 富文本**: 支持表格、列表、样式等
 * - **增强可读性**: 使用格式化布局展示复杂错误（如类型不匹配）
 * - **回退机制**: 未配置 IDE 特定渲染器时，回退到 [DefaultErrorMessages]
 *
 * ## 使用场景
 *
 * 1. 代码编辑器悬浮提示 (Hover Tooltips)
 * 2. 错误高亮的详细信息
 * 3. 快速修复 (Quick Fix) 的说明文本
 * 4. 意图操作 (Intention Actions) 的描述
 *
 * ## HTML 格式示例
 *
 * ### 简单错误
 * ```
 * Unresolved reference: <b>foo</b>
 * ```
 *
 * ### 类型不匹配（表格对比）
 * ```html
 * <table>
 * <tr><td>Required:</td><td><b>String</b></td></tr>
 * <tr><td>Found:</td><td><b>Int</b></td></tr>
 * </table>
 * ```
 *
 * ### 推断错误（嵌套列表）
 * ```html
 * Type inference failed:
 * <ul>
 *   <li>Cannot infer type parameter T</li>
 *   <li>Not enough information to infer parameter T</li>
 * </ul>
 * ```
 *
 * ## 扩展方式
 *
 * ### 1. 通过注册表配置 IDE 特定渲染器
 *
 * ```kotlin
 * IdeErrorMessages.configureIdeRenderers {
 *     register(Errors.TYPE_MISMATCH) { diagnostic ->
 *         \"\"\"
 *         <table>
 *         <tr><td>Expected:</td><td><b>${diagnostic.a}</b></td></tr>
 *         <tr><td>Actual:</td><td><b>${diagnostic.b}</b></td></tr>
 *         </table>
 *         \"\"\".trimIndent()
 *     }
 * }
 * ```
 *
 * ### 2. 通过 DiagnosticRendererProvider 扩展点
 *
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *   <diagnosticRendererProvider implementation="...IdeRendererProvider"/>
 * </extensions>
 * ```
 *
 * @see DefaultErrorMessages 基础文本渲染器
 * @see DiagnosticRendererRegistry 渲染器注册中心
 */
object IdeErrorMessages : DiagnosticMessageRenderer {

    /**
     * IDE 特定的渲染器映射
     *
     * 存储为 IDE 场景优化的富文本渲染器。
     * 如果诊断在此映射中没有对应的渲染器，会回退到 DefaultErrorMessages。
     */
    private val ideSpecificRenderers = ConcurrentHashMap<String, (Diagnostic) -> String>()

    /**
     * 渲染诊断消息为 IDE 友好的格式
     *
     * ## 渲染优先级
     *
     * 1. IDE 特定渲染器（此对象注册的富文本渲染器）
     * 2. [DefaultErrorMessages] 的纯文本渲染
     *
     * @param diagnostic 诊断对象
     * @param context 渲染上下文（建议使用 [MessageRenderingContext.IDE]）
     * @return HTML 格式的错误消息，或纯文本回退消息
     */
    override fun render(diagnostic: Diagnostic, context: MessageRenderingContext): String {
        // 1. 查找 IDE 特定的渲染器
        val factoryName = diagnostic.factory.name
        ideSpecificRenderers[factoryName]?.let { renderer ->
            return renderer(diagnostic)
        }

        // 2. 回退到 DefaultErrorMessages
        return DefaultErrorMessages.render(diagnostic, context)
    }

    /**
     * 便捷方法：直接渲染为 IDE 格式
     */
    fun render(diagnostic: Diagnostic): String = render(diagnostic, MessageRenderingContext.IDE)

    /**
     * 配置 IDE 特定的渲染器
     *
     * @param block 配置块
     */
    fun configureIdeRenderers(block: IdeRendererConfiguration.() -> Unit) {
        val config = IdeRendererConfiguration()
        config.block()
        config.applyTo(this)
    }

    /**
     * 注册 IDE 特定的渲染器
     *
     * @param factoryName 诊断工厂名称
     * @param renderer 渲染函数
     */
    internal fun registerIdeRenderer(factoryName: String, renderer: (Diagnostic) -> String) {
        ideSpecificRenderers[factoryName] = renderer
    }

    /**
     * IDE 渲染器配置 DSL
     */
    class IdeRendererConfiguration {
        private val renderers = mutableMapOf<String, (Diagnostic) -> String>()

        /**
         * 注册 IDE 特定渲染器
         *
         * @param factoryName 诊断工厂名称
         * @param renderer 渲染函数，应返回 HTML 格式的消息
         */
        fun register(factoryName: String, renderer: (Diagnostic) -> String) {
            renderers[factoryName] = renderer
        }

        /**
         * 应用配置到 IdeErrorMessages
         */
        internal fun applyTo(messages: IdeErrorMessages) {
            renderers.forEach { (name, renderer) ->
                messages.registerIdeRenderer(name, renderer)
            }
        }
    }
}
