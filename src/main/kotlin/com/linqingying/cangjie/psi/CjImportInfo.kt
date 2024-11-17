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

package com.linqingying.cangjie.psi

import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.QualifiedExpressionResolver.ExpressionQualifierPart
import com.linqingying.cangjie.resolve.QualifiedExpressionResolver.QualifierPart
import com.linqingying.cangjie.types.expressions.isWithoutValueArguments
import com.intellij.util.SmartList

fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
    when (this) {
        is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
        is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
    }

fun CjExpression.asQualifierPartList(doubleColonLHS: Boolean = false): List<ExpressionQualifierPart> {
    val result = SmartList<ExpressionQualifierPart>()

    fun addQualifierPart(expression: CjExpression?): Boolean {
        if (expression is CjSimpleNameExpression) {
            result.add(ExpressionQualifierPart(expression))
            return true
        }
        if (doubleColonLHS && expression is CjCallExpression && expression.isWithoutValueArguments) {
            val simpleName = expression.calleeExpression
            if (simpleName is CjSimpleNameExpression) {
                result.add(
                    ExpressionQualifierPart(
                        simpleName.getReferencedNameAsName(),
                        simpleName,
                        expression.typeArgumentList
                    )
                )
                return true
            }
        }
        return false
    }

    var expression: CjExpression? = this
    while (true) {
        if (addQualifierPart(expression)) break
        if (expression !is CjQualifiedExpression) break

        addQualifierPart(expression.selectorExpression)

        expression = expression.receiverExpression
    }

    return result.asReversed()
}


interface CjImportInfo {
    sealed class ImportContent {
        class ExpressionBased(val expression: CjExpression) : ImportContent()
        class FqNameBased(val fqName: FqName) : ImportContent()
    }

    val isAllUnder: Boolean
    val importContent: ImportContent?
    val importedFqName: FqName?

    //    val importedFqNames: MutableList<FqName>?
    val aliasName: String?

    //    修饰符
    val modifierVisibility: DescriptorVisibility get() = DescriptorVisibilities.PRIVATE

    val importedName: Name?
        get() {
            return computeNameAsString()?.takeIf(CharSequence::isNotEmpty)?.let(Name::identifier)
        }

    private fun computeNameAsString(): String? {
        if (isAllUnder) return null
        aliasName?.let { return it }
        return when (val importContent = importContent) {
            is ImportContent.ExpressionBased -> CjPsiUtil.getLastReference(importContent.expression)
                ?.getReferencedName()

            is ImportContent.FqNameBased -> importContent.fqName.takeUnless(FqName::isRoot)?.shortName()?.asString()
            null -> null
        }
    }
}
