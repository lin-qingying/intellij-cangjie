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

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.resolve.scopes.receivers.Receiver
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.utils.ReadOnly
import com.intellij.lang.ASTNode
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

interface Call {
    // SAFE_ACCESS or DOT or so
    var  noValueArgument: Boolean

    //控制typeArgumentList返回空值
    var noTypeParameter: Boolean
    val callOperationNode: ASTNode?

    val isSemanticallyEquivalentToSafeCall: Boolean
        get() = callOperationNode != null && callOperationNode!!.elementType === CjTokens.SAFE_ACCESS


    val explicitReceiver: Receiver?


    val dispatchReceiver: ReceiverValue?


    val calleeExpression: CjExpression?


    val valueArgumentList: CjValueArgumentList?


    @get:ReadOnly
    val valueArguments: List<ValueArgument>


    @get:ReadOnly
    val functionLiteralArguments: List<LambdaArgument>


    @get:ReadOnly
    val typeArguments: List<CjTypeProjection>


    val typeArgumentList: CjTypeArgumentList?


    val callElement: CjElement

    enum class CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE, CONTAINS
    }


    val callType: CallType
}
