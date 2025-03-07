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

package cn.cangnova.cangjie.utils

import com.intellij.util.asSafely
import cn.cangnova.cangjie.psi.CjBinaryExpression
import cn.cangnova.cangjie.psi.CjDotQualifiedExpression
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.psiUtil.callExpression
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.calls.util.getType
import cn.cangnova.cangjie.types.util.builtIns


internal fun CjExpression.isRangeExpression(context: Lazy<BindingContext>? = null): Boolean =
    getRangeBinaryExpressionType(context) != null

internal fun CjExpression.isComparable(context: BindingContext): Boolean {
    val valType = getType(context) ?: return false
    return DescriptorUtils.isSubtypeOfClass(valType, valType.builtIns.comparable)
}

internal fun CjExpression.getRangeBinaryExpressionType(context: Lazy<BindingContext>? = null): RangeCjExpressionType? {
    val binaryExprName = asSafely<CjBinaryExpression>()?.operationReference?.referencedNameAsName?.asString()
    val dotQualifiedName = asSafely<CjDotQualifiedExpression>()?.callExpression?.calleeExpression?.text
    val name = binaryExprName ?: dotQualifiedName
    return when {
        binaryExprName == ".." -> RangeCjExpressionType.RANGE_TO

        else -> null
    }/*?.takeIf {
        val notNullContext = context?.value ?: safeAnalyze(BodyResolveMode.PARTIAL)
        getResolvedCall(notNullContext)?.resultingDescriptor?.fqNameOrNull()?.asString()?.startsWith("std.core.") == true
    }*/
}

enum class RangeCjExpressionType {
    RANGE_TO/*, RANGE_UNTIL, DOWN_TO, UNTIL*/
}
