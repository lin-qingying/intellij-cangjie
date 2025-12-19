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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportDirectiveItem

/**
 * 重导出声明的包装类
 *
 * 在仓颉语言中，重导出（re-export）是通过带有可见性修饰符的 import 语句实现的：
 * - `import std.core.String` - 默认 private，仅当前文件内可访问
 * - `internal import std.core.String` - 当前包及子包可访问
 * - `protected import std.core.String` - 当前模块内可访问
 * - `public import std.core.String` - 外部可访问（真正的重导出）
 *
 * 此类保存重导出的原始描述符和相关元信息，用于在作用域解析时
 * 正确处理重导出的可见性和来源追踪。
 *
 * @property originalDescriptor 被重导出的原始描述符
 * @property visibility 重导出的可见性级别
 * @property sourceFile 定义重导出的源文件
 * @property sourcePackage 定义重导出的包
 * @property importDirective 定义重导出的导入语句
 * @property aliasName 重导出的别名（如果有）
 */
data class ReexportedDeclaration(
    /** 被重导出的原始描述符 */
    val originalDescriptor: DeclarationDescriptor,

    /** 重导出的可见性级别 */
    val visibility: DescriptorVisibility,

    /** 定义重导出的源文件 */
    val sourceFile: CjFile,

    /** 定义重导出的包 */
    val sourcePackage: PackageFragmentDescriptor,

    /** 定义重导出的导入语句 */
    val importDirective: CjImportDirectiveItem,

    /** 重导出的别名（如果有） */
    val aliasName: Name? = null
) {
    /**
     * 有效名称
     *
     * 如果有别名则使用别名，否则使用原始描述符的名称
     */
    val effectiveName: Name
        get() = aliasName ?: originalDescriptor.name

    /**
     * 原始描述符的完全限定名
     */
    val originalFqName: FqName
        get() = when (val desc = originalDescriptor) {
            is org.cangnova.cangjie.descriptors.ClassifierDescriptor -> desc.fqNameSafe
            is org.cangnova.cangjie.descriptors.CallableDescriptor -> desc.fqNameSafe
            else -> sourcePackage.fqName.child(originalDescriptor.name)
        }

    /**
     * 检查此重导出是否对指定的访问者可见
     *
     * 可见性规则：
     * - PUBLIC: 任何位置都可见
     * - PROTECTED: 同一模块内可见
     * - INTERNAL: 同一包或子包可见
     * - PRIVATE: 仅同一文件可见（不应该出现在重导出中）
     *
     * @param fromPackage 访问者所在的包
     * @param fromModule 访问者所在的模块（用于 protected 检查）
     * @return 是否可见
     */
    fun isVisibleFrom(
        fromPackage: PackageFragmentDescriptor?,
        fromModule: org.cangnova.cangjie.descriptors.ModuleDescriptor? = null
    ): Boolean {
        return when (visibility) {
            DescriptorVisibilities.PUBLIC -> true

            DescriptorVisibilities.PROTECTED -> {
                // 检查是否在同一模块
                fromModule != null && fromModule == sourcePackage.containingDeclaration
            }

            DescriptorVisibilities.INTERNAL -> {
                // 检查是否在同一包或子包
                if (fromPackage == null) return false
                val sourceFqName = sourcePackage.fqName
                val fromFqName = fromPackage.fqName
                fromFqName == sourceFqName || fromFqName.asString().startsWith(sourceFqName.asString() + ".")
            }

            else -> false // PRIVATE 不应该在重导出中
        }
    }

    override fun toString(): String {
        return "ReexportedDeclaration(${effectiveName.asString()} -> ${originalDescriptor.name.asString()}, visibility=$visibility)"
    }
}

/**
 * 重导出声明的 FqName 扩展
 */
private val DeclarationDescriptor.fqNameSafe: FqName
    get() = org.cangnova.cangjie.resolve.DescriptorUtils.getFqNameSafe(this)
