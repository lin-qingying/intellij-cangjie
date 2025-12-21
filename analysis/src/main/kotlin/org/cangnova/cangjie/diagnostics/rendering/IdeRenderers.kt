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

/**
 * IDE 诊断渲染器配置
 *
 * 为 IDE 环境配置 HTML 富文本格式的诊断渲染器。
 * 如果不配置特定诊断的 IDE 渲染器，会自动回退到默认渲染器。
 *
 * ## 何时需要配置 IDE 渲染器
 *
 * 1. **HTML 格式化**：需要使用 HTML 标签（表格、列表等）
 * 2. **增强可读性**：通过富文本提高错误信息的可读性
 * 3. **复杂信息展示**：类型对比、多条件错误等
 *
 * ## 使用示例
 *
 * ```kotlin
 * DiagnosticRendererRegistry.configureIde {
 *     register(TYPE_MISMATCH) {
 *         message { IDECangJieDiagnosisBundle.rawMessage(it) }
 *         // IDE Bundle 中可以使用 HTML：
 *         // <table>
 *         //   <tr><td>Expected:</td><td><b>{0}</b></td></tr>
 *         //   <tr><td>Found:</td><td><b>{1}</b></td></tr>
 *         // </table>
 *     }
 * }
 * ```
 */
class IdeRenderers : DiagnosticRendererProvider {
    override fun register() {
        DiagnosticRendererRegistry.configureIde {
            // TODO: 在这里配置需要 HTML 格式的诊断渲染器

            // 示例：类型不匹配错误可以使用表格格式
            // register(TYPE_MISMATCH) {
            //     message { IDECangJieDiagnosisBundle.rawMessage(it) }
            //     renderers(Renderers.RENDER_TYPE, Renderers.RENDER_TYPE)
            // }
        }
    }
}