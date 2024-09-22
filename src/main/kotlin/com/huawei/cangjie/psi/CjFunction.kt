package com.huawei.cangjie.psi


interface CjFunction : CjDeclarationWithBody, CjCallableDeclaration {
    val isLocal: Boolean
    val isStatic: Boolean
        get() = false
    val isOperator: Boolean
        get() = false
}

