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
import org.cangnova.cangjie.name.*

/**
 * CjImportInfo接口定义了导入信息的数据结构，用于在代码中表示和操作导入语句。
 */
interface CjImportInfo {
    /**
     * 导入内容的密封类，表示导入语句的两种不同形式。
     */
    sealed class ImportContent {
        /**
         * 基于表达式的导入内容，用于表示使用表达式形式的导入。
         *
         * @property expression 表达式，用于定义导入的内容。
         */
        class ExpressionBased(val expression: CjExpression) : ImportContent()

        /**
         * 基于全限定名的导入内容，用于表示使用全限定名形式的导入。
         *
         * @property fqName 全限定名，用于定义导入的内容。
         */
        class FqNameBased(val fqName: FqName) : ImportContent()
    }

    /**
     * 表示是否为“全部导入”类型，即是否导入指定包下的所有内容。
     */
    val isAllUnder: Boolean

    /**
     * 导入内容，可以是基于表达式的导入或基于全限定名的导入。
     */
    val importContent: ImportContent?

    /**
     * 导入的全限定名，用于标识导入的具体类或包。
     */
    val importedFqName: FqName?

    //    val importedFqNames: MutableList<FqName>?
    /**
     * 别名，用于在导入时给导入项指定一个不同的名称。
     */
    val aliasName: String?

    /**
     * 导入的名称，表示导入项在代码中使用的名称。
     */
    val importedName: Name?
        get() {
            return computeNameAsString()?.takeIf(CharSequence::isNotEmpty)?.let(Name::identifier)
        }

    /**
     * 计算导入名称的字符串表示形式。
     *
     * @return 导入名称的字符串表示形式，如果无法计算则返回null。
     */
    private fun computeNameAsString(): String? {
        if (isAllUnder) return null
        aliasName?.let { return it }
        return when (val importContent = importContent) {
            is ImportContent.ExpressionBased -> CjPsiUtil.getLastReference(importContent.expression)
                ?.referencedName

            is ImportContent.FqNameBased -> importContent.fqName.takeUnless(FqName::isRoot)?.shortName()?.asString()
            null -> null
        }
    }
}
