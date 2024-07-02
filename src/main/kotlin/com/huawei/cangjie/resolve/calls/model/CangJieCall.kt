package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.name.Name


interface CangJieCall : ResolutionAtom {
    val callKind: CangJieCallKind
//
    val explicitReceiver: ReceiverCangJieCallArgument?
//
//    // a.(foo)() -- (foo) is dispatchReceiverForInvoke
    val dispatchReceiverForInvokeExtension: ReceiverCangJieCallArgument? get() = null

    val name: Name

    val typeArguments: List<TypeArgument>

    val argumentsInParenthesis: List<CangJieCallArgument>
//
    val externalArgument: CangJieCallArgument?

    val isForImplicitInvoke: Boolean
}
