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

package org.cangnova.cangjie.diagnostics.rendering.deprecation

import org.cangnova.cangjie.diagnostics.rendering.*

/**
 * 弃用诊断渲染器配置
 *
 * 此文件仅配置需要特殊处理的复杂弃用诊断。
 * 大多数简单弃用诊断会被 DiagnosticRendererRegistry 自动推断，无需在此配置。
 *
 * ## 弃用诊断的特殊性
 *
 * 每个 `DiagnosticFactoryForDeprecation` 会生成两个诊断工厂：
 * - `XXX_ERROR`：错误级别（在指定版本后报错）
 * - `XXX_WARNING`：警告级别（在指定版本前警告）
 *
 * 两者通常使用相同的渲染逻辑，系统会自动处理。
 *
 * ## 何时需要显式配置
 *
 * 1. **自定义弃用消息**：需要特殊的弃用说明格式
 * 2. **版本信息展示**：需要定制版本信息的显示方式
 * 3. **迁移建议**：需要提供详细的迁移指南
 * 4. **复杂参数处理**：弃用诊断参数需要特殊转换
 */
class DeprecationRenderers : DiagnosticRendererProvider {
    override fun register() {
        DiagnosticRendererRegistry.configure {

            // 大多数弃用诊断使用自动推断，无需配置
            // 只有复杂弃用诊断需要在此添加配置

            // 示例：如果有复杂弃用诊断，按以下格式添加
            // register(SOME_DEPRECATED_FEATURE_ERROR) {
            //     message { factory ->
            //         val baseMessage = CangJieDiagnosisBundle.rawMessage(factory.name)
            //         val version = factory.deprecatingFeature.sinceVersion
            //         if (version != null) {
            //             "$baseMessage. This will become an error in CangJie ${version.versionString}"
            //         } else {
            //             "$baseMessage. This will become an error in a future release"
            //         }
            //     }
            //     renderers(Renderers.RENDER_TYPE)
            // }
            //
            // register(SOME_DEPRECATED_FEATURE_WARNING) {
            //     message { factory ->
            //         val baseMessage = CangJieDiagnosisBundle.rawMessage(factory.name)
            //         "$baseMessage. This is deprecated and will be removed"
            //     }
            //     renderers(Renderers.RENDER_TYPE)
            // }
        }
    }
}
