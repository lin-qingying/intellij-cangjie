package com.huawei.cangjie.builtins.functions


enum class FunctionClassKind {
    Function,
//    SuspendFunction,
//    KFunction,
//    KSuspendFunction,
    UNKNOWN;

    companion object {
        fun getFunctionClassKind(functionTypeKind: FunctionTypeKind): FunctionClassKind = when (functionTypeKind) {
            FunctionTypeKind.Function -> Function
//            FunctionTypeKind.SuspendFunction -> SuspendFunction
//            FunctionTypeKind.KFunction -> KFunction
//            FunctionTypeKind.KSuspendFunction -> KSuspendFunction
            else -> UNKNOWN
        }
    }
}
