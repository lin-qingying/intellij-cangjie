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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.parentOrNull
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjPackageDirective
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypes2
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolverFacade

/**
 * 判断全限定名是否可以添加根前缀
 *
 * 根前缀（Root Prefix）是 IDE 解析模式中的特殊机制，用于区分用户定义的符号和标准库符号。
 * 通过在全限定名前添加根前缀，可以确保符号解析的准确性，避免命名冲突。
 *
 * **不能添加根前缀的情况**：
 * - 全限定名已经包含根前缀（避免重复添加）
 * - 全限定名是顶层包（如 `std`、`os` 等）
 *
 * **根前缀格式**：
 * 通常为 `::` 或特定的标识符，定义在 [QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT] 中。
 *
 * **使用场景**：
 * - IDE 自动导入时生成完整的限定名
 * - 代码补全时展示符号的完整路径
 * - 快速修复（Quick Fix）生成导入语句
 *
 * @receiver FqName 要检查的全限定名
 * @return Boolean true 表示可以添加根前缀，false 表示不能添加
 */
fun FqName.canAddRootPrefix(): Boolean {
    // 如果全限定名不以根前缀开头，并且不是顶层包（parentOrNull()?.isRoot == false）
    return !asString().startsWith(QualifiedExpressionResolverFacade.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT)
            && parentOrNull()?.isRoot == false
}

/**
 * 判断当前 PSI 元素是否可以添加根前缀
 *
 * 某些 PSI 元素位置不应该添加根前缀，例如：
 * - 导入语句中的路径（import 语句已经是绝对路径）
 * - 包声明中的路径（package 语句定义的是当前文件的包名）
 *
 * **实现逻辑**：
 * 检查当前元素的父元素链，如果包含 [CjImportItem] 或 [CjPackageDirective]，
 * 则不应该添加根前缀。
 *
 * **使用场景**：
 * - 在生成自动导入建议时，判断是否需要添加根前缀
 * - 在代码补全时，根据上下文决定是否显示根前缀
 * - 在重构操作中，确保生成的限定名格式正确
 *
 * @receiver CjElement 要检查的 PSI 元素
 * @return Boolean true 表示可以添加根前缀，false 表示不能添加
 */
fun CjElement.canAddRootPrefix(): Boolean {
    // 获取当前元素的父元素，如果父元素是 CjImportItem 或 CjPackageDirective 类型，则返回 false
    return getParentOfTypes2<CjImportItem, CjPackageDirective>() == null
}

/**
 * 如果需要，为全限定名添加根前缀
 *
 * 此函数根据当前全限定名和目标元素的上下文，决定是否添加根前缀。
 *
 * **添加条件**：
 * 1. 当前全限定名可以添加根前缀（通过 [canAddRootPrefix] 检查）
 * 2. 目标元素的上下文允许添加根前缀（如果提供了 [targetElement]）
 *
 * **添加规则**：
 * - 如果满足条件，在全限定名前添加 [QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT]
 * - 否则返回原始全限定名
 *
 * **使用示例**：
 * ```kotlin
 * val fqName = FqName("std.collection.ArrayList")
 * val prefixed = fqName.withRootPrefixIfNeeded()
 * // 结果可能是 "::std.collection.ArrayList" 或 "std.collection.ArrayList"（取决于上下文）
 * ```
 *
 * @receiver FqName 要处理的全限定名
 * @param targetElement 目标 PSI 元素（用于检查上下文），null 表示不检查元素上下文
 * @return FqName 处理后的全限定名（可能添加了根前缀）
 */
fun FqName.withRootPrefixIfNeeded(targetElement: CjElement? = null): FqName {
    // 如果当前 FqName 可以添加根前缀，并且目标元素也可以添加根前缀
    if (canAddRootPrefix() && targetElement?.canAddRootPrefix() != false) {
        // 返回带有根前缀的 FqName
        return FqName(QualifiedExpressionResolverFacade.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT + asString())
    }

    // 否则返回当前 FqName
    return this
}