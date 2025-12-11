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

package org.cangnova.cangjie.diagnostics.rendering.warnings

import org.cangnova.cangjie.diagnostics.rendering.*

/**
 * 警告诊断渲染器配置
 *
 * 此文件仅配置需要特殊处理的复杂警告。
 * 大多数简单警告会被 DiagnosticRendererRegistry 自动推断，无需在此配置。
 *
 * ## 何时需要显式配置
 *
 * 1. **自定义参数提取**：警告参数不是直接使用，需要转换
 * 2. **条件渲染**：根据参数值决定如何渲染
 * 3. **复杂消息格式**：需要特殊的消息格式化逻辑
 * 4. **参数数量不匹配**：消息模板参数数量与诊断参数不一致
 */
class WarningRenderers : DiagnosticRendererProvider {
    override fun register() {
        DiagnosticRendererRegistry.configure {

            // 大多数警告使用自动推断，无需配置
            // 只有复杂警告需要在此添加配置

            // 示例：如果有复杂警告，按以下格式添加
            // register(SOME_COMPLEX_WARNING) {
            //     message { CangJieDiagnosisBundle.rawMessage(it) }
            //     parameterExtractor { diagnostic ->
            //         arrayOf(
            //             diagnostic.field1,
            //             diagnostic.field2
            //         )
            //     }
            // }
        }
    }
}
