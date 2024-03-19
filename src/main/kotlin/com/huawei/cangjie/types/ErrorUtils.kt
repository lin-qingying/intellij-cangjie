package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.DeclarationDescriptor

object ErrorUtils {

//    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor

    @JvmStatic
    fun isError(candidate: DeclarationDescriptor?): Boolean =
        candidate != null /*
        && (isErrorClass(candidate) || isErrorClass(candidate.containingDeclaration)
                            || candidate === errorModule
                )*/

}