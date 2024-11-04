package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.utils.CangJieExceptionWithAttachments


class CheckMatchException(message: String) : CangJieExceptionWithAttachments(message)
typealias Matrix = List<List<Pattern>>
//
///** Calculates the pattern matrix by splitting or-patterns across different rows */
//@Throws(CheckMatchException::class)
//fun List<CjMatchEntry>.calculateMatrix(context: ExpressionTypingContext): Matrix =
//    flatMap { arm -> arm.conditions.map { listOf(it.lower) } }
//
//
//
//
//// lower_pattern_unadjusted
//private val CjCasePattern.kind: PatternKind
//    get() = when (this) {
//        is CjBindingPattern -> {
//            if (pat != null) TODO("Support `x @ pat`")
//            when (val resolved = patBinding.reference.resolve()) {
//                is CjEnumEntry -> PatternKind.Enum(resolved.parentEnum, resolved, emptyList())
//
//                else -> PatternKind.Binding(patBinding.type, patBinding.name.orEmpty())
//            }
//        }
//
//        is CjWildcardPattern -> PatternKind.Wild
//
//
//        is CjConstantPattern -> {
//            val ty = expr.type
//            if (ty is TyAdt) {
//                if (ty.item is RsEnumItem) {
//                    val path = (expr as RsPathExpr).path
//                    val variant = path.reference?.resolve() as? RsEnumVariant
//                        ?: throw CheckMatchException("Can't resolve ${path.text}")
//                    PatternKind.Variant(ty.item, variant, emptyList())
//                } else {
//                    throw CheckMatchException("Unresolved constant")
//                }
//            } else {
//                val value = expr.value ?: throw CheckMatchException("Can't evaluate constant ${expr.text}")
//                PatternKind.Const(value)
//            }
//        }
//
//
//        else -> TODO()
//    }
