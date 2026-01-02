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

import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.messages.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.MissingResourceException

/**
 * IDE 诊断消息 Bundle
 *
 * 为 IDE 场景提供优化的诊断消息，支持 HTML 富文本格式。
 *
 * ## 与 CangJieDiagnosisBundle 的区别
 *
 * - **CangJieDiagnosisBundle**：纯文本，适用于命令行、构建日志
 * - **IDECangJieDiagnosisBundle**：HTML 富文本，适用于 IDE 悬浮提示、快速修复
 *
 * ## 消息格式
 *
 * IDE Bundle 中的消息可以包含 HTML 标签：
 * - `<b>`, `<i>`, `<code>` - 文本样式
 * - `<table>`, `<tr>`, `<td>` - 表格对比
 * - `<ul>`, `<li>` - 列表展示
 *
 * ## 示例
 *
 * ### properties 文件内容
 * ```properties
 * # messages/IDECangJieDiagnosisBundle.properties
 * TYPE_MISMATCH=<table><tr><td>Expected:</td><td><b>{0}</b></td></tr><tr><td>Found:</td><td><b>{1}</b></td></tr></table>
 * UNRESOLVED_REFERENCE=Unresolved reference: <b>{0}</b>
 * ```
 *
 * ### 使用示例
 * ```kotlin
 * // IDE 渲染器配置
 * DiagnosticRendererRegistry.configure(MessageRenderingContext.IDE) {
 *     bundle = IDECangJieDiagnosisBundle
 *     register(TYPE_MISMATCH) {
 *         // 自动使用 IDECangJieDiagnosisBundle 获取消息模板
 *     }
 * }
 * ```
 *
 * ## 回退机制
 *
 * 如果 IDE Bundle 中没有定义某个消息键，会自动回退到 [CangJieDiagnosisBundle]。
 *
 * @see CangJieDiagnosisBundle 默认诊断消息 Bundle
 * @see MessageBundle Bundle 接口定义
 */
@NonNls
private const val BUNDLE = "messages.IDECangJieDiagnosisBundle"
//只渲染html
object IDECangJieDiagnosisBundle : AbstractCangJieBundle(BUNDLE), MessageBundle {

    /**
     * 获取 IDE 格式的消息（带参数）
     *
     * 如果当前 Bundle 中没有该消息,会自动回退到 CangJieDiagnosisBundle
     *
     * @param key 消息键
     * @param params 消息参数
     * @return 格式化后的 HTML 消息
     */
    @Nls

    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return try {
            htmlMessage(key, *params)
        } catch (e: MissingResourceException) {
            // 回退到默认 Bundle
            CangJieDiagnosisBundle.message(key, *params)
        }
    }

    /**
     * 获取原始消息模板（不带参数）
     *
     * 如果当前 Bundle 中没有该消息,会自动回退到 CangJieDiagnosisBundle
     *
     * @param key 消息键
     * @return 消息模板
     */
    @Nls

    fun rawMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String): String {
        return try {
            getMessage(key)
        } catch (e: MissingResourceException) {
            // 回退到默认 Bundle
            CangJieDiagnosisBundle.rawMessage(key)
        }
    }

    /**
     * 获取原始消息模板（通过工厂）
     *
     * 如果当前 Bundle 中没有该消息,会自动回退到 CangJieDiagnosisBundle
     *
     * @param factory 诊断工厂
     * @return 消息模板
     */
    @Nls

    fun rawMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) factory: DiagnosticFactory<*>): String {
        return try {
            getMessage(factory.name)
        } catch (e: MissingResourceException) {
            // 回退到默认 Bundle
            CangJieDiagnosisBundle.rawMessage(factory)
        }
    }

    /**
     * 获取 HTML 格式的消息（带参数）
     *
     * IDE Bundle 中的消息默认已包含 HTML 标签，此方法主要用于兼容性。
     * 如果当前 Bundle 中没有该消息,会自动回退到 CangJieDiagnosisBundle
     *
     * @param key 消息键
     * @param params 消息参数
     * @return HTML 格式的消息
     */
    @Nls

    fun htmlMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return try {
            super<AbstractCangJieBundle>.getMessage(key, *params).withHtml()
        } catch (e: MissingResourceException) {
            // 回退到默认 Bundle
            CangJieDiagnosisBundle.htmlMessage(key, *params)
        }
    }

    // 实现 MessageBundle 接口
    @Nls
    override fun getMessage(@NonNls key: String): String {
        return try {
            htmlMessage(key)
        } catch (e: MissingResourceException) {
            // 回退到默认 Bundle
            CangJieDiagnosisBundle.getMessage(key)
        }
    }

    @Nls
    override fun getMessage(@NonNls factory: DiagnosticFactory<*>): String {
        return try {
            getMessage(factory.name)
        } catch (e: MissingResourceException) {
            // 回退到默认 Bundle
            CangJieDiagnosisBundle.getMessage(factory)
        }
    }
}
