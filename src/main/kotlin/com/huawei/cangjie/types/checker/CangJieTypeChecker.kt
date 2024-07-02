package com.huawei.cangjie.types.checker

import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor

interface CangJieTypeChecker {
    interface TypeConstructorEquality {
        fun equals(
            a: TypeConstructor,
            b: TypeConstructor
        ): Boolean
    }

    fun isSubtypeOf(
        subtype: CangJieType,
        supertype: CangJieType
    ): Boolean

    fun equalTypes(a: CangJieType, b: CangJieType): Boolean

    companion object {
        @JvmField
        val DEFAULT: CangJieTypeChecker = NewCangJieTypeChecker.Default
    }
}
