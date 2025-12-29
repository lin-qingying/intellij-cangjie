/*
 * Copyright 2024 LinQingYing. and contributors.
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
import org.cangnova.cangjie.psi.CjFile

/**
 * 导入优先级接口
 *
 * 用于比较不同导入的优先级，决定代码补全时的排序顺序。
 * 实现 [Comparable] 接口，支持导入优先级的比较。
 *
 * **使用场景**：
 * - 代码补全时，优先显示更相关的符号
 * - 自动导入优化时，选择最佳导入方式
 */
interface ImportComparablePriority : Comparable<ImportComparablePriority>

/**
 * 可导入全限定名分类器
 *
 * 根据文件的导入状态，对全限定名进行分类。
 * 用于代码补全、自动导入等功能中，判断符号的导入优先级。
 *
 * **分类依据**：
 * - 当前包中的符号（最高优先级，无需导入）
 * - 默认导入的符号（如标准库）
 * - 精确导入的符号（已通过 import 导入）
 * - 通配符导入的符号（通过 import * 导入）
 * - 同包符号（同一个包中有其他符号被导入）
 * - 未导入的符号（需要添加导入）
 *
 * **使用场景**：
 * - 代码补全：优先显示已导入的符号
 * - 自动导入：判断是否需要添加导入语句
 * - 导入优化：清理未使用的导入
 *
 * **性能优化**：
 * 在构造函数中预计算所有导入信息，避免重复遍历导入列表。
 *
 * @property file 要分析的仓颉文件
 * @property isImportedByDefault 判断符号是否默认导入的函数（如标准库符号）
 */
class ImportableFqNameClassifier(private val file: CjFile, private val isImportedByDefault: (FqName) -> Boolean) {
    /**
     * 精确导入的全限定名集合
     *
     * 包含所有通过精确导入语句导入的符号，例如：
     * - `import std.collection.ArrayList`
     *
     * 不包括通配符导入和别名导入。
     */
    private val preciseImports = HashSet<FqName>()

    /**
     * 精确导入的包集合
     *
     * 包含所有有精确导入的包名。
     * 用于判断同包符号（siblingImported）：如果一个包中有符号被精确导入，
     * 那么该包中的其他符号也更可能需要导入。
     */
    private val preciseImportPackages = HashSet<FqName>()

    /**
     * 通配符导入的包集合
     *
     * 包含所有通过通配符导入的包，例如：
     * - `import std.collection.*`
     *
     * 该包下的所有符号都可以直接使用，无需精确导入。
     */
    private val allUnderImports = HashSet<FqName>()

    /**
     * 排除的导入集合
     *
     * 包含所有使用别名导入的符号。
     * 这些符号虽然被导入，但使用的是别名，所以原始名称被视为"未导入"。
     * 例如：`import std.collection.ArrayList as List`
     *
     * TODO: 在代码补全中支持别名导入
     */
    private val excludedImports = HashSet<FqName>()

    init {
        // 遍历文件的所有导入语句,构建导入信息
        for (import in file.importDirectives.flatMap { it.importItems }) {
            val importPath = import.importPath ?: continue
            val fqName = importPath.fqName
            when {
                // 通配符导入：import foo.*
                importPath.isAllUnder -> allUnderImports.add(fqName)

                // 精确导入且无别名：import foo.Bar
                !importPath.hasAlias() -> {
                    preciseImports.add(fqName)
                    preciseImportPackages.add(fqName.parent())
                }

                // 别名导入：import foo.Bar as Baz
                else -> excludedImports.add(fqName)
                // TODO: support aliased imports in completion
            }
        }
    }

    /**
     * 符号的导入分类
     *
     * 按优先级从高到低排列：
     * - [fromCurrentPackage] - 当前包中的符号（最高优先级，无需导入）
     * - [defaultImport] - 默认导入的符号（如标准库，无需显式导入）
     * - [preciseImport] - 精确导入的符号（已通过 import 语句导入）
     * - [allUnderImport] - 通配符导入的符号（通过 import * 导入）
     * - [siblingImported] - 同包符号（该包中有其他符号被导入）
     * - [topLevelPackage] - 顶层包（包名位于根级别）
     * - [notImported] - 未导入的符号（需要添加导入）
     * - [notToBeUsedInCangJie] - 不应在仓颉中使用的符号（保留字段）
     */
    enum class Classification {
        fromCurrentPackage,
        topLevelPackage,
        preciseImport,
        defaultImport,
        allUnderImport,
        siblingImported,
        notImported,
        notToBeUsedInCangJie
    }

    /**
     * 对全限定名进行分类
     *
     * 根据文件的导入状态和符号的位置，判断符号的导入分类。
     *
     * **包（Package）的分类逻辑**：
     * - 如果包被精确导入 → [Classification.preciseImport]
     * - 如果是顶层包（父级是根） → [Classification.topLevelPackage]
     * - 否则 → [Classification.notImported]
     *
     * **符号（非包）的分类逻辑**（按优先级）：
     * 1. 当前包中的符号 → [Classification.fromCurrentPackage]
     * 2. 默认导入的符号（如标准库） → [Classification.defaultImport]
     * 3. 精确导入的符号 → [Classification.preciseImport]
     * 4. 通配符导入的符号 → [Classification.allUnderImport]
     * 5. 同包符号（该包中有其他符号被精确导入） → [Classification.siblingImported]
     * 6. 否则 → [Classification.notImported]
     *
     * **使用示例**：
     * ```kotlin
     * val classifier = ImportableFqNameClassifier(file) { it.asString().startsWith("std.") }
     * val classification = classifier.classify(FqName("std.collection.ArrayList"), false)
     * // classification 可能是 Classification.defaultImport（如果在 std 包中）
     * // 或 Classification.preciseImport（如果已导入）
     * ```
     *
     * @param fqName 要分类的全限定名
     * @param isPackage 是否为包名（true）还是类型/函数名（false）
     * @return Classification 符号的导入分类
     */
    fun classify(fqName: FqName, isPackage: Boolean): Classification {
        if (isPackage) {
            return when {
                isImportedWithPreciseImport(fqName) -> Classification.preciseImport
                fqName.parent().isRoot -> Classification.topLevelPackage
                else -> Classification.notImported
            }
        }

        return when {

            fqName.parent() == file.packageFqName -> Classification.fromCurrentPackage

            isImportedByDefault(fqName) -> Classification.defaultImport

            isImportedWithPreciseImport(fqName) -> Classification.preciseImport

            isImportedWithAllUnderImport(fqName) -> Classification.allUnderImport

            hasPreciseImportFromPackage(fqName.parent()) -> Classification.siblingImported

            else -> Classification.notImported
        }
    }

    /**
     * 判断符号是否通过精确导入
     *
     * @param name 全限定名
     * @return Boolean true 表示已通过精确导入
     */
    private fun isImportedWithPreciseImport(name: FqName) = name in preciseImports

    /**
     * 判断符号是否通过通配符导入
     *
     * 符号通过通配符导入的条件：
     * 1. 符号所在的包被通配符导入
     * 2. 符号本身没有被别名导入（否则原始名称不可用）
     *
     * @param name 全限定名
     * @return Boolean true 表示已通过通配符导入
     */
    private fun isImportedWithAllUnderImport(name: FqName) = name.parent() in allUnderImports && name !in excludedImports

    /**
     * 判断包中是否有符号被精确导入
     *
     * 用于判断同包符号（siblingImported）：
     * 如果一个包中有符号被精确导入，该包中的其他符号也更可能需要导入，
     * 因此在代码补全时应该优先显示。
     *
     * @param packageName 包名
     * @return Boolean true 表示该包中有符号被精确导入
     */
    private fun hasPreciseImportFromPackage(packageName: FqName) = packageName in preciseImportPackages
}
