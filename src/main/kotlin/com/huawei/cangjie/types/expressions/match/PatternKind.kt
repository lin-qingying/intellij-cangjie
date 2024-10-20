package com.huawei.cangjie.types.expressions.match

import com.huawei.cangjie.psi.CjEnum
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.types.CangJieType

sealed class PatternKind {
    object Error : PatternKind()

    object Wild : PatternKind()

    /**
     * 绑定模式
     * let x:type
     */
    data class Binding(val type: CangJieType, val name: String) : PatternKind()

    /**
     * 常量模式
     */
    data class Const(val value: ConstantValue<*>) : PatternKind()


    /**
     * 枚举模式
     */
    data class Enum(val enum: CjEnum, val entry: CjEnumEntry, val subPatterns: List<Pattern>) : PatternKind()


}
