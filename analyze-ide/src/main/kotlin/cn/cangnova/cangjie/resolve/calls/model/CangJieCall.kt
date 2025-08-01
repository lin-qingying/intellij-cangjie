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

package cn.cangnova.cangjie.resolve.calls.model


interface CangJieCall : ResolutionAtom {
    val callKind: CangJieCallKind

    //
    val explicitReceiver: ReceiverCangJieCallArgument?

    //
//    // a.(foo)() -- (foo) is dispatchReceiverForInvoke
    val dispatchReceiverForInvokeExtension: ReceiverCangJieCallArgument? get() = null

    val name: Name

    //    上层原子的类型参数
    val topTypeArguments: List<TypeArgument>
    val typeArguments: List<TypeArgument>

    val argumentsInParenthesis: List<CangJieCallArgument>

    //
    val externalArgument: CangJieCallArgument?

    val isForImplicitInvoke: Boolean
}

private fun SimpleCangJieCallArgument.checkReceiverInvariants() {
    assert(!isSpread) {
        "Receiver cannot be a spread: $this"
    }
    assert(argumentName == null) {
        "Argument name should be null for receiver: $this, but it is $argumentName"
    }
}

fun CangJieCall.checkCallInvariants() {
    assert(explicitReceiver !is LambdaCangJieCallArgument && explicitReceiver !is CallableReferenceCangJieCallArgument) {
        "Lambda argument or callable reference is not allowed as explicit receiver: $explicitReceiver"
    }

    (explicitReceiver as? SimpleCangJieCallArgument)?.checkReceiverInvariants()
    (dispatchReceiverForInvokeExtension as? SimpleCangJieCallArgument)?.checkReceiverInvariants()

    when (callKind) {
        CangJieCallKind.FUNCTION, CangJieCallKind.INVOKE -> {
            assert(externalArgument == null || !externalArgument!!.isSpread) {
                "External argument cannot nave spread element: $externalArgument"
            }
            assert(externalArgument?.argumentName == null) {
                "Illegal external argument with name: $externalArgument"
            }
            assert(dispatchReceiverForInvokeExtension == null || !dispatchReceiverForInvokeExtension!!.isSafeCall) {
                "Dispatch receiver for invoke cannot be safe: $dispatchReceiverForInvokeExtension"
            }
        }

        CangJieCallKind.VARIABLE -> {
            assert(externalArgument == null) {
                "External argument is not allowed not for function call: $externalArgument."
            }
            assert(argumentsInParenthesis.isEmpty()) {
                "Arguments in parenthesis should be empty for not function call: $this "
            }
            assert(dispatchReceiverForInvokeExtension == null) {
                "Dispatch receiver for invoke should be null for not function call: $dispatchReceiverForInvokeExtension"
            }

        }
        CangJieCallKind.CASE_ENUM -> {

        }

        /*CangJieCallKind.ENUM , */CangJieCallKind.ENUM_ENTRY -> {

        }

        CangJieCallKind.CALLABLE_REFERENCE -> {
            assert(argumentsInParenthesis.isEmpty()) {
                "Callable references can't have value arguments"
            }
//            assert(typeArguments.isEmpty()) {
//                "Callable references can't have explicit type arguments"
//            }
            assert(externalArgument == null) {
                "External argument is not allowed not for function call: $externalArgument."
            }
        }

        CangJieCallKind.UNSUPPORTED -> error("Call with UNSUPPORTED kind")
    }
}
