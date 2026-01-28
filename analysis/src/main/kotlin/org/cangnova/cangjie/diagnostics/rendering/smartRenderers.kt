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

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.types.CangJieType

/**
 * 智能类型渲染器
 *
 * 用于在诊断信息中智能渲染仓颉类型。该渲染器会根据渲染上下文自适应地调整
 * 类型名称的显示策略，以便在错误信息中提供更清晰、更易理解的类型表示。
 *
 * @property baseRenderer 基础的描述符渲染器，用于执行实际的类型渲染操作
 */
class SmartTypeRenderer(private val baseRenderer: DescriptorRenderer) : DiagnosticParameterRenderer<CangJieType> {

    /**
     * 渲染仓颉类型
     *
     * 根据提供的渲染上下文，使用自适应的分类器命名策略来渲染类型。
     * 这确保了在不同的诊断场景下，类型名称能够以最合适的方式显示。
     *
     * @param obj 要渲染的仓颉类型对象
     * @param renderingContext 渲染上下文，包含自适应的分类器策略等信息
     * @return 渲染后的类型字符串表示
     */
    override fun render(obj: CangJieType, renderingContext: RenderingContext): String {
        // 创建一个自适应的渲染器，使用上下文中的分类器命名策略
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        // 使用自适应渲染器渲染类型
        return adaptiveRenderer.renderType(obj)
    }
}

/**
 * 智能描述符渲染器
 *
 * 用于在诊断信息中智能渲染声明描述符（如函数、类、变量等）。该渲染器会根据
 * 渲染上下文自适应地调整描述符的显示策略，以便在错误信息中提供更精确、
 * 更有针对性的声明信息。
 *
 * @property baseRenderer 基础的描述符渲染器，用于执行实际的描述符渲染操作
 */
class SmartDescriptorRenderer(private val baseRenderer: DescriptorRenderer) :
    DiagnosticParameterRenderer<DeclarationDescriptor> {

    /**
     * 渲染声明描述符
     *
     * 根据提供的渲染上下文，使用自适应的分类器命名策略来渲染声明描述符。
     * 这确保了在不同的诊断场景下，声明信息能够以最清晰的方式呈现给用户。
     *
     * @param obj 要渲染的声明描述符对象
     * @param renderingContext 渲染上下文，包含自适应的分类器策略等信息
     * @return 渲染后的声明描述符字符串表示
     */
    override fun render(obj: DeclarationDescriptor, renderingContext: RenderingContext): String {
        // 创建一个自适应的渲染器，使用上下文中的分类器命名策略
        val adaptiveRenderer = baseRenderer.withOptions {
            classifierNamePolicy = renderingContext.adaptiveClassifierPolicy
        }
        // 使用自适应渲染器渲染描述符
        return adaptiveRenderer.render(obj)
    }
}