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

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.messages.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * 代码洞察（Code Insight）国际化消息 Bundle
 *
 * 用于管理代码洞察相关功能的本地化消息，包括：
 * - 代码补全提示
 * - 意图操作（Intention Actions）
 * - 代码检查警告抑制
 * - 声明类型描述
 * - 工具提示文本
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 获取简单消息
 * val message = CangJieCodeInsightBundle.message("declaration.kind.class")
 * // 输出: "class"
 *
 * // 获取带参数的消息
 * val suppressMsg = CangJieCodeInsightBundle.message(
 *     "intention.suppress.text",
 *     "UNUSED_VARIABLE",
 *     "fun",
 *     "myFunction"
 * )
 * // 输出: "Suppress 'UNUSED_VARIABLE' for fun myFunction"
 *
 * // 组合消息
 * val nameMsg = CangJieCodeInsightBundle.message(
 *     "declaration.name.0.of.1",
 *     "parameter",
 *     "myFunction"
 * )
 * // 输出: "parameter of myFunction"
 * ```
 *
 * @see AbstractCangJieBundle
 */
@NonNls
private const val BUNDLE = "messages.CangJieCodeInsightBundle"

object CangJieCodeInsightBundle : AbstractCangJieBundle(BUNDLE) {
    /**
     * 获取国际化消息
     *
     * @param key 消息键，必须在 CangJieCodeInsightBundle.properties 中定义
     * @param params 消息参数，用于替换占位符 {0}, {1}, {2} 等
     * @return 格式化后的本地化消息
     */
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
