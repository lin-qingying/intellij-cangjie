package com.huawei.cangjie.utils

import com.huawei.cangjie.name.Name


object OperatorNameConventions {

    @JvmField
    val INVOKE = Name.identifier("operator_invoke")


    fun Name.asOperatorString(): String {
        return when (this) {
            INVOKE ->   "()"
            else -> throw IllegalArgumentException("Unknown operator name: $this")
        }

    }


}
