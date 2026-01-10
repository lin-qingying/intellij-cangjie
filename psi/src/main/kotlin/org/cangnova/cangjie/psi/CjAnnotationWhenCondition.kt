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
 * PSI元素类，表示@When注解的条件编译条件部分
 *
 * 示例:
 * ```cangjie
 * @When[os == "windows"]  // os == "windows" 是条件
 * @When[target == "x86_64"]  // target == "x86_64" 是条件
 * @When[debug]  // debug 是条件
 * ```
 *
 * 此类用于封装@When注解中方括号内的条件表达式。
 * 条件可以是布尔表达式、比较表达式或标识符等。
 *
 * 常见的条件包括:
 * - 操作系统判断: `os == "windows"`, `os == "linux"`, `os == "macos"`
 * - 架构判断: `target == "x86_64"`, `target == "aarch64"`
 * - 编译模式: `debug`, `release`
 * - 自定义特性: 任意布尔表达式
 */
class CjAnnotationWhenCondition(node: ASTNode) : CjElementImpl(node) {
    /**
     * 获取条件表达式的文本值
     * @return 条件表达式字符串，如 "os == \"windows\"", "debug" 等
     */
    fun getConditionText(): String? = text

    /**
     * 获取条件表达式（如果是表达式类型）
     * @return 条件表达式的 PSI 元素，如果不是表达式返回 null
     */
    fun getConditionExpression(): CjExpression? {
        return findChildByClass(CjExpression::class.java)
    }
}
