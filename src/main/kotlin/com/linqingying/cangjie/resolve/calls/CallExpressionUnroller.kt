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

package com.linqingying.cangjie.resolve.calls

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjQualifiedExpression
import com.intellij.lang.ASTNode

data class CallExpressionElement internal constructor(val qualified: CjQualifiedExpression) {

    val receiver: CjExpression
        get() = qualified.receiverExpression

    val selector: CjExpression?
        get() = qualified.selectorExpression

    val safe: Boolean
        get() = qualified.operationSign == CjTokens.SAFE_ACCESS

    val node: ASTNode
        get() = qualified.operationTokenNode
}
fun unrollToLeftMostQualifiedExpression(expression: CjQualifiedExpression): List<CjQualifiedExpression> {
    val unrolled = arrayListOf<CjQualifiedExpression>()

    var finger = expression
    while (true) {
        unrolled.add(finger)
        val receiver = finger.receiverExpression
        if (receiver !is CjQualifiedExpression) {
            break
        }
        finger = receiver
    }

    return unrolled.asReversed()
}
