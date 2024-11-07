package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.types.CangJieType

sealed class PatternKind {
    object Error : PatternKind()

    object Wild : PatternKind()

    /**
     * 绑定模式
     * let x:type
     */
    data class Binding(val type: CangJieType, val name: String) : PatternKind()
    data class Type(val type: CangJieType, val name: String) : PatternKind()

    /**
     * 常量模式
     */
    data class Const(val value: ConstantValue<*>) : PatternKind()

    /**
     * 元组模式
     */
    data class Tuple(  val subPatterns: List<Pattern>) : PatternKind()

    /**
     * 枚举模式
     */
    data class Enum(val enum: CjEnum, val entry: CjEnumEntry, val subPatterns: List<Pattern>) : PatternKind()


}
