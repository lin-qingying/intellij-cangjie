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

import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.ABSTRACT_FUNCTION_WITHOUT_RETURN_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import org.cangnova.cangjie.diagnostics.infos.errors.NO_ELSE_IN_MATCH_BY_PATTERN
import org.cangnova.cangjie.diagnostics.infos.errors.REDECLARATION
import org.cangnova.cangjie.types.expressions.match.Pattern

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
internal class IdeRenderers : DiagnosticRendererProvider {
    override fun register() {
        DiagnosticRendererRegistry.configureIde {
            // 重复声明错误 - 使用 HTML 列表格式化所有冲突的声明
            register(REDECLARATION) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { declarations: Collection<DeclarationDescriptor> ->
                        // 使用 HTML 列表格式渲染所有冲突的声明
                        buildString {
                            append("<ul>")
                            for (declaration in declarations) {
                                val declarationText = Renderers.FQ_NAMES_IN_TYPES.render(
                                    declaration,
                                    RenderingContext.of(declaration)
                                )
                                append("<li><code>$declarationText</code></li>")
                            }
                            append("</ul>")
                        }
                    }
                )
            }

            // match 表达式穷举性错误 - 使用 HTML 列表格式化缺失的模式
            register(NO_ELSE_IN_MATCH_BY_PATTERN) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { patterns: List<Pattern> ->
                        buildString {
                            append("<ul>")
                            for (pattern in patterns) {
                                append("<li><code>case ${pattern.text(null)}</code></li>")
                            }
                            append("</ul>")
                        }
                    }
                )
            }
            // 抽象成员未实现错误 - 使用列表渲染所有未实现的成员
            register(ABSTRACT_MEMBER_NOT_IMPLEMENTED) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    Renderers.RENDER_TYPE_STATMENT,  // 渲染类名 {0}
                    renderer { members: Collection<CallableMemberDescriptor> ->
                        // 使用 HTML 列表格式渲染所有未实现的成员
                        buildString {
                            append("<ul>")
                            for (member in members) {
                                val memberText = Renderers.FQ_NAMES_IN_TYPES.render(
                                    member,
                                    RenderingContext.of(member)
                                )
                                append("<li><code>$memberText</code></li>")
                            }
                            append("</ul>")
                        }
                    }
                )
            }


            // 示例：类型不匹配错误可以使用表格格式
            // register(TYPE_MISMATCH) {
            //     message { IDECangJieDiagnosisBundle.rawMessage(it) }
            //     renderers(Renderers.RENDER_TYPE, Renderers.RENDER_TYPE)
            // }
        }
    }
}