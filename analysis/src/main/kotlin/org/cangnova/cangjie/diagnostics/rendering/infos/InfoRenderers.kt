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

package org.cangnova.cangjie.diagnostics.rendering.infos

import org.cangnova.cangjie.diagnostics.rendering.*

/**
 * 信息诊断渲染器配置
 *
 * 此文件仅配置需要特殊处理的复杂信息诊断。
 * 大多数简单信息诊断会被 DiagnosticRendererRegistry 自动推断，无需在此配置。
 *
 * ## INFO 级别诊断的特点
 *
 * - 不是错误或警告，仅提供信息
 * - 通常用于提示、建议或额外的上下文信息
 * - 不会阻止编译或影响代码执行
 *
 * ## 何时需要显式配置
 *
 * 1. **插件信息**：需要展示插件相关的详细信息
 * 2. **复杂提示**：需要根据上下文生成动态提示
 * 3. **多语言支持**：需要特殊的国际化处理
 * 4. **富文本格式**：需要 HTML 或特殊格式化
 */
class InfoRenderers : DiagnosticRendererProvider {
    override fun register() {
        DiagnosticRendererRegistry.configure {

            // 大多数信息诊断使用自动推断，无需配置
            // 只有复杂信息诊断需要在此添加配置

            // 示例：如果有复杂信息诊断，按以下格式添加
            // register(PLUGIN_INFO) {
            //     message { CangJieDiagnosisBundle.rawMessage(it) }
            //     parameterExtractor { diagnostic ->
            //         val rendered = diagnostic.a // RenderedDiagnostic
            //         arrayOf(rendered.text)
            //     }
            // }
        }
    }
}
