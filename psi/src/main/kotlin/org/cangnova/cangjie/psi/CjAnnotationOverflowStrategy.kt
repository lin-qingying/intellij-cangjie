/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode

/**
 * PSI元素类,表示溢出处理注解的溢出策略参数部分
 *
 * 示例:
 * ```cangjie
 * @OverflowThrowing[checked]  // checked是溢出策略参数
 * @OverflowWrapping[wrapping]  // wrapping是溢出策略参数
 * @OverflowSaturating[saturating]  // saturating是溢出策略参数
 * ```
 *
 * 此类用于封装溢出处理注解(@OverflowThrowing, @OverflowWrapping, @OverflowSaturating)
 * 中方括号内的溢出策略标识符。
 *
 * 支持的溢出策略包括:
 * - checked: 抛出异常(用于OverflowThrowing)
 * - wrapping: 环绕处理(用于OverflowWrapping)
 * - saturating: 饱和处理(用于OverflowSaturating)
 */
class CjAnnotationOverflowStrategy(node: ASTNode) : CjElementImpl(node) {
    /**
     * 获取溢出策略的文本值
     * @return 溢出策略字符串,如 "checked", "wrapping" 或 "saturating"
     */
    fun getOverflowStrategyText(): String? = text
}
