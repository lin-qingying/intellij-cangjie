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

package org.cangnova.cangjie.resolve.qualified

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorWithVisibility
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibilityUtils
import org.cangnova.cangjie.resolve.qualified.QualifierPosition.*

/**
 * 限定符位置枚举
 *
 * 表示限定名称在代码中出现的位置，不同位置应用不同的解析规则和可见性检查。
 *
 * ## 位置说明
 *
 * ### PACKAGE_HEADER - 包声明位置
 * ```kotlin
 * package com.example
 * ```
 * - 只验证包路径的有效性
 * - 不进行可见性检查
 * - 不解析为描述符，仅记录包名
 *
 * ### IMPORT - 导入语句位置
 * ```kotlin
 * import com.example.Foo
 * import com.example.bar.*
 * ```
 * - 解析包、类、类型别名
 * - 执行严格的可见性检查（private 符号只能在同一文件内导入）
 * - 禁止导入成员函数和变量
 * - 检查是否从单例对象全导入
 *
 * ### TYPE - 类型引用位置
 * ```kotlin
 * var x: com.example.Foo
 * fun foo(): com.example.Bar
 * ```
 * - 只解析类型（类、接口、类型别名）
 * - 不解析值（变量、函数）
 * - 应用标准可见性规则
 * - 检查枚举条目不能作为类型
 *
 * ### EXPRESSION - 表达式位置
 * ```kotlin
 * com.example.Foo.bar()
 * val x = com.example.obj
 * ```
 * - 值优先于类型（变量/函数优先于类/包）
 * - 需要区分限定符和成员访问
 * - 支持隐式伴生对象引用
 * - 应用标准可见性规则
 */
 enum class QualifierPosition {
    /** 包声明位置：`package com.example` */
    PACKAGE_HEADER,

    /** 导入语句位置：`import com.example.Foo` */
    IMPORT,

    /** 类型引用位置：`var x: com.example.Foo` */
    TYPE,

    /** 表达式位置：`com.example.Foo.bar()` */
    EXPRESSION
}

/**
 * 可见性检查函数
 *
 * 根据描述符的可见性修饰符和解析位置，判断符号是否对当前位置可见。
 *
 * ## 可见性规则
 *
 * ### 导入位置（IMPORT）
 * - **private** 符号：只能在同一文件内导入
 * - **其他可见性**：如果需要在导入中检查，则执行标准检查
 *
 * ### 其他位置（TYPE, EXPRESSION, PACKAGE_HEADER）
 * - 使用标准可见性规则：
 *   - **public**：所有地方可见
 *   - **protected**：子类和同一包中可见
 *   - **internal**：同一模块中可见
 *   - **private**：同一文件或同一类中可见
 *
 * ## 特殊情况
 * - 如果描述符不是 `DeclarationDescriptorWithVisibility`，始终返回 true
 * - 如果 `shouldBeVisibleFrom` 为 null，始终返回 true
 *
 * @param descriptor 要检查的描述符（被访问的符号）
 * @param shouldBeVisibleFrom 可见性检查的起点（访问符号的位置）
 * @param position 解析位置，决定应用哪种可见性规则
 * @param languageVersionSettings 语言版本设置，影响某些可见性特性
 * @return 是否可见：true 表示可见，false 表示不可见
 */
internal fun isVisible(
    descriptor: DeclarationDescriptor,
    shouldBeVisibleFrom: DeclarationDescriptor?,
    position: QualifierPosition,
    languageVersionSettings: LanguageVersionSettings
): Boolean {
    // 不是带可见性的描述符，或没有检查起点，默认可见
    if (descriptor !is DeclarationDescriptorWithVisibility || shouldBeVisibleFrom == null) return true

    val visibility = descriptor.visibility

    // 导入位置的特殊规则
    if (position == IMPORT) {
        // private 符号只能在同一文件内导入
        if (DescriptorVisibilities.isPrivate(visibility)) {
            return DescriptorVisibilities.inSameFile(descriptor, shouldBeVisibleFrom)
        }
        // 如果可见性不要求在导入中检查，直接返回 true
        if (!visibility.mustCheckInImports()) return true
    }

    // 标准可见性检查（忽略接收器）
    return DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
        descriptor,
        shouldBeVisibleFrom,
        languageVersionSettings
    )
}
