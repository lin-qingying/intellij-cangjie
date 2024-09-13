
package com.huawei.cangjie.resolve.controlFlow.pseudocode

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.util.TypeUtils
import com.intellij.util.SmartFMap

interface TypePredicate : (CangJieType) -> Boolean {
    override fun invoke(typeToCheck: CangJieType): Boolean
}

data class SingleType(val targetType: CangJieType) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = CangJieTypeChecker.DEFAULT.equalTypes(typeToCheck, targetType)
    override fun toString(): String = targetType.render()
}

data class AllSubtypes(val upperBound: CangJieType) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = CangJieTypeChecker.DEFAULT.isSubtypeOf(typeToCheck, upperBound)

    override fun toString(): String = "{<: ${upperBound.render()}}"
}

data class ForAllTypes(val typeSets: List<TypePredicate>) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = typeSets.all { it(typeToCheck) }

    override fun toString(): String = "AND{${typeSets.joinToString(", ")}}"
}

data class ForSomeType(val typeSets: List<TypePredicate>) : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = typeSets.any { it(typeToCheck) }

    override fun toString(): String = "OR{${typeSets.joinToString(", ")}}"
}

object AllTypes : TypePredicate {
    override fun invoke(typeToCheck: CangJieType): Boolean = true

    override fun toString(): String = "*"
}

// todo: simplify computed type predicate when possible
fun and(predicates: Collection<TypePredicate>): TypePredicate =
    when (predicates.size) {
        0 -> AllTypes
        1 -> predicates.first()
        else -> ForAllTypes(predicates.toList())
    }

fun or(predicates: Collection<TypePredicate>): TypePredicate? =
    when (predicates.size) {
        0 -> null
        1 -> predicates.first()
        else -> ForSomeType(predicates.toList())
    }

fun CangJieType.getSubtypesPredicate(): TypePredicate = when {
    CangJieBuiltIns.isAny (this) && isMarkedOption -> AllTypes
    TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, this) -> AllSubtypes(this)
    else -> SingleType(this)
}


private fun CangJieType.render(): String = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(this)

fun <T : Any> TypePredicate.expectedTypeFor(keys: Iterable<T>): Map<T, TypePredicate> =
    keys.fold(SmartFMap.emptyMap<T, TypePredicate>()) { map, key -> map.plus(key, this) }
