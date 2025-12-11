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

@file:Suppress("unused")

package org.cangnova.cangjie.diagnostics.rendering.errors

import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.rendering.*
import org.cangnova.cangjie.types.isError


/**
 * 错误诊断渲染器配置
 *
 * 此文件仅配置需要特殊处理的复杂诊断。
 * 大多数简单诊断会被 DiagnosticRendererRegistry 自动推断，无需在此配置。
 *
 * ## 何时需要显式配置
 *
 * 1. **自定义参数提取**：诊断参数不是直接使用，需要转换
 * 2. **条件渲染**：根据参数值决定如何渲染
 * 3. **复杂消息格式**：需要特殊的消息格式化逻辑
 * 4. **参数数量不匹配**：消息模板参数数量与诊断参数不一致
 */
class ErrorRenderers : DiagnosticRendererProvider {
    override fun register() {
        DiagnosticRendererRegistry.configure {

            // ========================================
            // 二元运算符错误 - 需要提取 6 个参数
            // ========================================
            register(INVALID_BINARY_OPERATOR) {
                message { CangJieDiagnosisBundle.rawMessage(it) }
                parameterExtractor { diagnostic ->
                    // diagnostic.a 是 InvalidBinaryData 类型
                    val data = diagnostic.a
                    val context = RenderingContext.of(
                        data.operatorString,
                        data.leftType,
                        data.rightType
                    )
                    arrayOf(
                        CommonRenderers.STRING.render(data.operatorString, context),
                        Renderers.RENDER_TYPE.render(data.leftType, context),
                        Renderers.RENDER_TYPE.render(data.rightType, context),
                        CommonRenderers.STRING.render(data.operatorString, context),
                        Renderers.RENDER_TYPE.render(data.rightType, context),
                        Renderers.RENDER_TYPE.render(data.leftType, context)
                    )
                }
            }

            // ========================================
            // 类型投影导致的类型不匹配 - 需要从对象提取字段
            // ========================================
            register(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS) {
                message { CangJieDiagnosisBundle.rawMessage(it) }
                parameterExtractor { diagnostic ->
                    val obj = diagnostic.a  // TypeMismatchDueToTypeProjections 对象
                    val context = RenderingContext.of(
                        obj.expectedType,
                        obj.expressionType,
                        obj.receiverType,
                        obj.callableDescriptor
                    )
                    arrayOf(
                        Renderers.RENDER_TYPE.render(obj.expectedType, context),
                        Renderers.RENDER_TYPE.render(obj.expressionType, context),
                        Renderers.RENDER_TYPE.render(obj.receiverType, context),
                        Renderers.FQ_NAMES_IN_TYPES.render(obj.callableDescriptor, context)
                    )
                }
            }

            // ========================================
            // 函数调用期望 - 条件渲染
            // ========================================
            register(FUNCTION_EXPECTED) {
                message { CangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    Renderers.ELEMENT_TEXT,
                    ContextDependentRenderer { type, context ->
                        if (type.isError) {
                            ""
                        } else {
                            " of type '${Renderers.RENDER_TYPE.render(type, context)}'"
                        }
                    }
                )
            }

            // ========================================
            // 表达式期望 - 字符串格式化
            // ========================================
            register(EXPRESSION_EXPECTED) {
                message { CangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    renderer { expression ->
                        val expressionType = expression.toString()
                        expressionType.replaceFirstChar { it.uppercase() }
                    }
                )
            }

            // ========================================
            // 函数调用期望（带参数提示）- 条件渲染
            // ========================================
            register(FUNCTION_CALL_EXPECTED) {
                message { CangJieDiagnosisBundle.rawMessage(it) }
                renderers(
                    Renderers.ELEMENT_TEXT,
                    renderer { hasValueParameters ->
                        if (hasValueParameters) "..." else ""
                    }
                )
            }

            // ========================================
            // 解析到分类器 - 简单消息模板
            // ========================================
//    register(RESOLUTION_TO_CLASSIFIER) {
//        message { "{2}" }  // 只使用第三个参数
//        renderers(
//            Renderers.NAMED,
//            Renderers.TO_STRING,
//            CommonRenderers.STRING
//        )
//    }
        }
    }
}
