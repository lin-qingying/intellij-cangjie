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

package org.cangnova.cangjie.resolve.calls

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.calls.util.DelegatingCall
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver

object CallTransformer {
    fun stripCallArguments(call: Call): Call = object : DelegatingCall(call) {
//        override fun getValueArgumentList(): CjValueArgumentList? = null
//
//        override fun getValueArguments(): List<ValueArgument> = emptyList()
//
//        override fun getFunctionLiteralArguments(): List<LambdaArgument> = emptyList()
//
//        override fun getTypeArguments(): List<CjTypeProjection> = emptyList()
//
//        override fun getTypeArgumentList(): CjTypeArgumentList? = null


        override val callElement: CjElement
            get() {
                val calleeExpression = calleeExpression
                require(calleeExpression != null) { "No callee expression: ${callElement.text}" }
                return calleeExpression
            }
    }

    fun stripReceiver(variableCall: Call): Call = object : DelegatingCall(variableCall) {
//        override fun getCallOperationNode(): ASTNode? = null
//
//        override fun getExplicitReceiver(): ReceiverValue? = null
    }

    class CallForImplicitInvoke(
        private val explicitExtensionReceiver: Receiver?,
        private val calleeExpressionAsDispatchReceiver: ExpressionReceiver,
        private val outerCall: Call,
        val itIsVariableAsFunctionCall: Boolean
    ) : DelegatingCall(outerCall) {
        //        private val fakeInvokeExpression: CjSimpleNameExpression =
//            CjPsiFactory(outerCall.callElement.project, false)
//                .createExpression(OperatorNameConventions.INVOKE.asString()) as CjSimpleNameExpression
        override val callOperationNode: ASTNode?
            get() = if (explicitExtensionReceiver != null) super.callOperationNode else null

        override val explicitReceiver: Receiver?
            get() = explicitExtensionReceiver
        override val dispatchReceiver: ReceiverValue?
            get() = calleeExpressionAsDispatchReceiver
//        override fun getCalleeExpression(): CjExpression = fakeInvokeExpression


        override val callType: Call.CallType
            get() = Call.CallType.INVOKE

        fun getOuterCall(): Call = outerCall
    }
}
