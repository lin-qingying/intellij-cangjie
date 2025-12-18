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

package org.cangnova.cangjie.quickDoc

import org.cangnova.cangjie.messages.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * CDoc 文档国际化消息 Bundle
 *
 * 用于管理仓颉文档（CDoc）相关功能的本地化消息，包括：
 * - 快速文档（Quick Documentation）提示
 * - CDoc 标签（@param, @return, @throws 等）
 * - 文档章节标题
 * - 文档渲染相关消息
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 获取章节标题
 * val title = CangJieCDocBundle.message("cdoc.section.title.parameters")
 * // 输出: "Parameters:"
 *
 * // 获取带参数的消息
 * val ordinalMsg = CangJieCDocBundle.message("quick.doc.text.enum.ordinal", 2)
 * // 输出: "ordinal = 2"
 *
 * // 获取错误消息
 * val noDocMsg = CangJieCDocBundle.message("quick.doc.no.documentation")
 * // 输出: "No documentation available"
 * ```
 *
 * @see AbstractCangJieBundle
 */
@NonNls
private const val BUNDLE = "messages.CangJieCDocBundle"

object CangJieCDocBundle : AbstractCangJieBundle(BUNDLE) {
    /**
     * 获取国际化消息
     *
     * @param key 消息键，必须在 CangJieCDocBundle.properties 中定义
     * @param params 消息参数，用于替换占位符 {0}, {1}, {2} 等
     * @return 格式化后的本地化消息
     */
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
