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

package org.cangnova.cangjie.resolve.calls.util

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.ReadOnly
import com.intellij.lang.ASTNode

open class DelegatingCall(private val delegate: Call) : Call {

    override val callOperationNode: ASTNode?
        get() = delegate.callOperationNode
    override val explicitReceiver: Receiver?
        get() = delegate.explicitReceiver


    override val dispatchReceiver: ReceiverValue?
        get() = delegate.dispatchReceiver
    override val calleeExpression: CjExpression?
        //
        get() = delegate.calleeExpression

    override var noValueArgument: Boolean = false

    override val valueArgumentList: CjValueArgumentList?
        get() {
            if (noValueArgument) return null
            return delegate.valueArgumentList
        }


    @get:ReadOnly
    override val valueArguments: List<ValueArgument>
        get() {
            if (noValueArgument) return emptyList()
            return delegate.valueArguments
        }


    override val functionLiteralArguments: List<LambdaArgument>
        get() = delegate.functionLiteralArguments
    override var noTypeParameter: Boolean = false

    override val typeArguments: List<CjTypeProjection>
        get() {
            if (noTypeParameter) return emptyList()
            return delegate.typeArguments
        }


    override val typeArgumentList: CjTypeArgumentList?
        //
        get() {
            if (noTypeParameter) return null
            return delegate.typeArgumentList
        }


    override val callElement: CjElement
        get() = delegate.callElement

    override val callType: Call.CallType
        get() = delegate.callType

    override fun toString(): String {
        return "*$delegate"
    }
}
