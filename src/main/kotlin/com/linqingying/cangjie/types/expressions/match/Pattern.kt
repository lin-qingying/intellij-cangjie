package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils

private fun CjEnumEntry.initializer(subPatterns: List<Pattern>, ctx: CjElement?): String = when {

    typeEntry != null -> subPatterns.joinToString(",", "(", ")") { it.text(ctx) }
    else -> ""
}

data class Pattern(val type: CangJieType, val kind: PatternKind) {

    fun text(ctx: CjElement?): String =
        when (kind) {
            is PatternKind.Wild -> "_"

            is PatternKind.Binding -> kind.name

            is PatternKind.Enum -> {
                val entryName = kind.entry.name.orEmpty()

                val initializer = kind.entry.initializer(kind.subPatterns, ctx)
                "$entryName$initializer"
            }

            is PatternKind.Type -> {
                kind.name

            }

            is PatternKind.Const -> kind.value.toString()
            PatternKind.Error -> ""
            is PatternKind.Tuple -> kind.subPatterns.joinToString(",", "(", ")") { it.text(ctx) }
        }

    val constructors: List<Constructor>?
        get() = when (kind) {
            is PatternKind.Wild, is PatternKind.Binding -> null
            is PatternKind.Enum -> listOf(Constructor.Enum(kind.entry))

            is PatternKind.Const -> listOf(Constructor.ConstantValue(kind.value))
            is PatternKind.Tuple -> listOf(Constructor.Single)

            is PatternKind.Error -> null
            is PatternKind.Type -> listOf(Constructor.Type(kind.type))
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
