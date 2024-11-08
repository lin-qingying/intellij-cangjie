package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils

data class Pattern(val type: CangJieType, val kind: PatternKind) {
    val constructors: List<Constructor>?
        get() = when (kind) {
            is PatternKind.Wild, is PatternKind.Binding -> null
            is PatternKind.Enum -> listOf(Constructor.Enum(kind.entry))

            is PatternKind.Const -> listOf(Constructor.ConstantValue(kind.value))
            is PatternKind.Tuple ->listOf(Constructor.Single)

            is PatternKind.Error -> null
            is PatternKind.Type -> TODO()
        }

    /**
     * Returns the type of the pattern suitable for generating constructors
     *
     * @returns dereferenced [type] when [type] is a (multi)reference to enum
     * @returns [type] in other cases
     */
    val ergonomicType: CangJieType
        get() {

            return type
        }

    companion object {
        val Error = Pattern(ErrorUtils.errorVariableType, PatternKind.Error)

        fun wild(ty: CangJieType = ErrorUtils.errorVariableType): Pattern = Pattern(ty, PatternKind.Wild)

    }
}
