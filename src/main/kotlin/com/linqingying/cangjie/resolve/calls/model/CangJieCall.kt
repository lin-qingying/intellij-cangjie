package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.name.Name


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
CangJieCallKind.ENUM ->{

}
        CangJieCallKind.CALLABLE_REFERENCE -> {
            assert(argumentsInParenthesis.isEmpty()) {
                "Callable references can't have value arguments"
            }
            assert(typeArguments.isEmpty()) {
                "Callable references can't have explicit type arguments"
            }
            assert(externalArgument == null) {
                "External argument is not allowed not for function call: $externalArgument."
            }
        }

        CangJieCallKind.UNSUPPORTED -> error("Call with UNSUPPORTED kind")
    }
}
