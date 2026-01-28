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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.types.CangJieType
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

    // =====================================================
    // HTML 渲染辅助函数
    // =====================================================

    /**
     * 将描述符渲染为 HTML 代码块
     */
    private fun DeclarationDescriptor.renderAsCode(): String {
        val text = Renderers.FQ_NAMES_IN_TYPES.render(this, RenderingContext.of(this))
        return "<code>$text</code>"
    }

    /**
     * 将类型渲染为 HTML 代码块
     */
    private fun CangJieType.renderAsCode(): String {
        val text = Renderers.RENDER_TYPE.render(this, RenderingContext.of(this))
        return "<code>$text</code>"
    }

    /**
     * 将描述符集合渲染为 HTML 列表
     */
    private fun Collection<DeclarationDescriptor>.renderAsList(): String = buildString {
        append("<ul>")
        for (descriptor in this@renderAsList) {
            append("<li>${descriptor.renderAsCode()}</li>")
        }
        append("</ul>")
    }

    /**
     * 将 ResolvedCall 集合渲染为 HTML 列表
     */
    private fun Collection<ResolvedCall<*>>.renderCallsAsList(): String = buildString {
        append("<ul>")
        for (call in this@renderCallsAsList) {
            val descriptor = call.resultingDescriptor
            append("<li>${descriptor.renderAsCode()}</li>")
        }
        append("</ul>")
    }

    override fun register() {
        DiagnosticRendererRegistry.configureIde {

            // =====================================================
            // 声明相关错误
            // =====================================================

            // 重复声明错误 - 使用 HTML 列表格式化所有冲突的声明
            register(REDECLARATION) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { declarations: Collection<DeclarationDescriptor> ->
                        declarations.renderAsList()
                    }
                )
            }

            // =====================================================
            // 解析相关错误
            // =====================================================

            // 无适用的候选 - 使用 HTML 列表展示所有候选
            register(NONE_APPLICABLE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { calls: Collection<ResolvedCall<*>> ->
                        calls.renderCallsAsList()
                    }
                )
            }

            // 重载解析模糊 - 使用 HTML 列表展示所有模糊的候选
            register(OVERLOAD_RESOLUTION_AMBIGUITY) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { calls: Collection<ResolvedCall<*>> ->
                        calls.renderCallsAsList()
                    }
                )
            }

            // 无法完成解析 - 使用 HTML 列表展示候选
            register(CANNOT_COMPLETE_RESOLVE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { calls: Collection<ResolvedCall<*>> ->
                        calls.renderCallsAsList()
                    }
                )
            }

            // 可调用引用解析模糊 - 使用 HTML 列表展示候选
            register(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { descriptors: Collection<CallableDescriptor> ->
                        @Suppress("UNCHECKED_CAST")
                        (descriptors as Collection<DeclarationDescriptor>).renderAsList()
                    }
                )
            }

            // =====================================================
            // 可见性相关错误
            // =====================================================

            // 不可见的成员 - 使用结构化展示可见性信息
            register(INVISIBLE_MEMBER) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { descriptor: DeclarationDescriptor ->
                        descriptor.renderAsCode()
                    },
                    renderer { visibility: DescriptorVisibility ->
                        "<code>${visibility.externalDisplayName}</code>"
                    },
                    renderer { container: DeclarationDescriptor ->
                        container.renderAsCode()
                    }
                )
            }

            // 不可见的引用 - 使用结构化展示可见性信息
            register(INVISIBLE_REFERENCE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { descriptor: DeclarationDescriptor ->
                        descriptor.renderAsCode()
                    },
                    renderer { visibility: DescriptorVisibility ->
                        "<code>${visibility.externalDisplayName}</code>"
                    },
                    renderer { container: DeclarationDescriptor ->
                        container.renderAsCode()
                    }
                )
            }

            // 不可见的 setter
            register(INVISIBLE_SETTER) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { descriptor: DeclarationDescriptor ->
                        descriptor.renderAsCode()
                    },
                    renderer { visibility: DescriptorVisibility ->
                        "<code>${visibility.externalDisplayName}</code>"
                    },
                    renderer { container: DeclarationDescriptor ->
                        container.renderAsCode()
                    }
                )
            }

            // =====================================================
            // 类型相关错误
            // =====================================================

            // 类型不匹配 - 使用表格对比期望和实际类型
            register(TYPE_MISMATCH) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { expected: CangJieType ->
                        expected.renderAsCode()
                    },
                    renderer { actual: CangJieType ->
                        actual.renderAsCode()
                    }
                )
            }

            // 上界违反 - 使用表格对比期望和实际类型
            register(UPPER_BOUND_VIOLATED) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { upperBound: CangJieType ->
                        upperBound.renderAsCode()
                    },
                    renderer { actualType: CangJieType ->
                        actualType.renderAsCode()
                    }
                )
            }

            // 不兼容的类型 - 使用表格对比
            register(INCOMPATIBLE_TYPES) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { type1: CangJieType ->
                        type1.renderAsCode()
                    },
                    renderer { type2: CangJieType ->
                        type2.renderAsCode()
                    }
                )
            }

            // 接收者类型不匹配
            register(RECEIVER_TYPE_MISMATCH) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { expected: CangJieType ->
                        expected.renderAsCode()
                    },
                    renderer { actual: CangJieType ->
                        actual.renderAsCode()
                    }
                )
            }

            // for 循环中的类型不匹配
            register(TYPE_MISMATCH_IN_FOR_LOOP) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { expected: CangJieType ->
                        expected.renderAsCode()
                    },
                    renderer { actual: CangJieType ->
                        actual.renderAsCode()
                    }
                )
            }

            // 多个超类型导致类型不匹配 - 使用列表展示
            register(TYPE_MISMATCH_MULTIPLE_SUPERTYPES) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { types: List<CangJieType> ->
                        buildString {
                            append("<ul>")
                            for (type in types) {
                                append("<li>${type.renderAsCode()}</li>")
                            }
                            append("</ul>")
                        }
                    }
                )
            }

            // =====================================================
            // 继承相关错误
            // =====================================================

            // 抽象成员未实现错误 - 使用列表渲染所有未实现的成员
            register(ABSTRACT_MEMBER_NOT_IMPLEMENTED) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    Renderers.RENDER_TYPE_STATMENT,  // 渲染类名 {0}
                    renderer { members: Collection<CallableMemberDescriptor> ->
                        @Suppress("UNCHECKED_CAST")
                        (members as Collection<DeclarationDescriptor>).renderAsList()
                    }
                )
            }

            // 冲突的继承成员 - 使用列表展示冲突的成员
            register(CONFLICTING_INHERITED_MEMBERS) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { containingDescriptor: InheritableDescriptor ->
                        (containingDescriptor as DeclarationDescriptor).renderAsCode()
                    },
                    renderer { members: Collection<CallableMemberDescriptor> ->
                        @Suppress("UNCHECKED_CAST")
                        (members as Collection<DeclarationDescriptor>).renderAsList()
                    }
                )
            }

            // 重写时返回类型不匹配
            register(RETURN_TYPE_MISMATCH_ON_OVERRIDE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { descriptor: CallableMemberDescriptor ->
                        descriptor.renderAsCode()
                    },
                    renderer { data: DeclarationWithDiagnosticComponents ->
                        data.declaration.renderAsCode()
                    }
                )
            }

            // 继承时返回类型不匹配
            register(RETURN_TYPE_MISMATCH_ON_INHERITANCE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { member1: CallableMemberDescriptor ->
                        member1.renderAsCode()
                    },
                    renderer { member2: CallableMemberDescriptor ->
                        member2.renderAsCode()
                    }
                )
            }

            // 重写 final 成员
            register(OVERRIDING_FINAL_MEMBER) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { overriddenMember: CallableMemberDescriptor ->
                        overriddenMember.renderAsCode()
                    },
                    renderer { containingClass: DeclarationDescriptor ->
                        containingClass.renderAsCode()
                    }
                )
            }

            // 没有可重写的内容
            register(NOTHING_TO_OVERRIDE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { descriptor: CallableMemberDescriptor ->
                        descriptor.renderAsCode()
                    }
                )
            }

            // 无法更改访问权限
            register(CANNOT_CHANGE_ACCESS_PRIVILEGE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { visibility: DescriptorVisibility ->
                        "<code>${visibility.externalDisplayName}</code>"
                    },
                    renderer { member: CallableMemberDescriptor ->
                        member.renderAsCode()
                    },
                    renderer { container: DeclarationDescriptor ->
                        container.renderAsCode()
                    }
                )
            }

            // 无法削弱访问权限
            register(CANNOT_WEAKEN_ACCESS_PRIVILEGE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { visibility: DescriptorVisibility ->
                        "<code>${visibility.externalDisplayName}</code>"
                    },
                    renderer { member: CallableMemberDescriptor ->
                        member.renderAsCode()
                    },
                    renderer { container: DeclarationDescriptor ->
                        container.renderAsCode()
                    }
                )
            }

            // =====================================================
            // Match 表达式相关错误
            // =====================================================

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

            // =====================================================
            // 属性相关错误
            // =====================================================

            // 重写时属性类型不匹配
            register(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { member1: CallableMemberDescriptor ->
                        member1.renderAsCode()
                    },
                    renderer { member2: CallableMemberDescriptor ->
                        member2.renderAsCode()
                    }
                )
            }

            // 继承时属性类型不匹配
            register(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { member1: CallableMemberDescriptor ->
                        member1.renderAsCode()
                    },
                    renderer { member2: CallableMemberDescriptor ->
                        member2.renderAsCode()
                    }
                )
            }

            // let 被 var 重写
            register(LET_OVERRIDDEN_BY_VAR) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { varProperty: PropertyDescriptor ->
                        varProperty.renderAsCode()
                    },
                    renderer { letProperty: PropertyDescriptor ->
                        letProperty.renderAsCode()
                    }
                )
            }

            // var 被 let 重写
            register(VAR_OVERRIDDEN_BY_LET) {
                message { IDECangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { letProperty: PropertyDescriptor ->
                        letProperty.renderAsCode()
                    },
                    renderer { varProperty: PropertyDescriptor ->
                        varProperty.renderAsCode()
                    }
                )
            }
        }
    }
}