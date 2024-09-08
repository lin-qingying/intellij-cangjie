package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariable
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstructor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.CommonSupertypes
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.checker.TypeIntersector
import com.huawei.cangjie.types.singleBestRepresentative
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.addIfNotNull

class TypeBoundsImpl(override val typeVariable: TypeVariable) : TypeBounds {
    override val bounds = ArrayList<TypeBounds.Bound>()
    var isFixed: Boolean = false
        private set
    fun setFixed() {
        isFixed = true
    }
    private var resultValues: Collection<CangJieType>? = null

    fun addBound(bound: TypeBounds.Bound) {
        resultValues = null
        assert(bound.typeVariable == typeVariable) {
            "$bound is added for incorrect type variable ${bound.typeVariable.name}. Expected: ${typeVariable.name}"
        }
        bounds.add(bound)
    }
    fun filter(condition: (ConstraintPosition) -> Boolean): TypeBoundsImpl {
        val result = TypeBoundsImpl(typeVariable)
        result.bounds.addAll(bounds.filter { condition(it.position) })
        return result
    }
    private fun filterBounds(
        bounds: Collection<TypeBounds.Bound>,
        kind: TypeBounds.BoundKind,
        errorValues: MutableCollection<CangJieType>? = null
    ): Set<CangJieType> {
        val result = LinkedHashSet<CangJieType>()
        for (bound in bounds) {
            if (bound.kind == kind) {
                if (!ErrorUtils.containsErrorType(bound.constrainingType)) {
                    result.add(bound.constrainingType)
                } else {
                    errorValues?.add(bound.constrainingType)
                }
            }
        }
        return result
    }

    private fun checkOnlyInputTypes(bounds: Collection<TypeBounds.Bound>, possibleAnswer: CangJieType): Boolean {
        if (!typeVariable.hasOnlyInputTypesAnnotation()) return true

        // Only type mentioned in bounds might be the result
        val typesInBoundsSet =
            bounds.filter { it.isProper && it.constrainingType.constructor.isDenotable }.map { it.constrainingType }
                .toSet()
        // Flexible types are equal to inflexible
        if (typesInBoundsSet.any { CangJieTypeChecker.DEFAULT.equalTypes(it, possibleAnswer) }) return true

        // For non-denotable number types only, no valid types are mentioned, so common supertype is valid
        val numberLowerBounds =
            filterBounds(bounds, TypeBounds.BoundKind.LOWER_BOUND).filter { it.constructor is IntegerValueTypeConstructor }
        val superTypeOfNumberLowerBounds = commonSupertypeForNumberTypes(numberLowerBounds)
        return possibleAnswer == superTypeOfNumberLowerBounds
    }

    private fun tryPossibleAnswer(bounds: Collection<TypeBounds.Bound>, possibleAnswer: CangJieType?): Boolean {
        if (possibleAnswer == null) return false
        // a captured type might be an answer
        if (!possibleAnswer.constructor.isDenotable && !possibleAnswer.isCaptured()) return false

        if (!checkOnlyInputTypes(bounds, possibleAnswer)) return false

        for (bound in bounds) {
            when (bound.kind) {
                TypeBounds.BoundKind.LOWER_BOUND -> if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(
                        bound.constrainingType,
                        possibleAnswer
                    )
                ) {
                    return false
                }

                TypeBounds.BoundKind.UPPER_BOUND -> if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(
                        possibleAnswer,
                        bound.constrainingType
                    )
                ) {
                    return false
                }

                TypeBounds.BoundKind.EXACT_BOUND -> if (!CangJieTypeChecker.DEFAULT.equalTypes(
                        bound.constrainingType,
                        possibleAnswer
                    )
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun getIntersectionOfSupertypes(types: Collection<CangJieType>): Set<CangJieType> {
        val upperBounds = HashSet<CangJieType>()
        for (type in types) {
            val supertypes = type.constructor.supertypes
            if (upperBounds.isEmpty()) {
                upperBounds.addAll(supertypes)
            } else {
                upperBounds.retainAll(supertypes.toSet())
            }
        }
        return upperBounds
    }

    private fun commonSupertypeForNumberTypes(numberLowerBounds: Collection<CangJieType>): CangJieType? {
        if (numberLowerBounds.isEmpty()) return null
        val intersectionOfSupertypes = getIntersectionOfSupertypes(numberLowerBounds)
        return TypeUtils.getDefaultPrimitiveNumberType(intersectionOfSupertypes) ?: CommonSupertypes.commonSupertype(
            numberLowerBounds
        )
    }

    private fun computeValues(): Collection<CangJieType> {
        val values = LinkedHashSet<CangJieType>()
        val bounds = bounds.filter { it.isProper }

        if (bounds.isEmpty()) {
            return listOf()
        }
        val hasStrongBound = bounds.any { it.position.isStrong() }
        if (!hasStrongBound) {
            return listOf()
        }

        val exactBounds = filterBounds(bounds, TypeBounds.BoundKind.EXACT_BOUND, values)
        val bestFit = exactBounds.singleBestRepresentative()
        if (bestFit != null) {
            if (tryPossibleAnswer(bounds, bestFit)) {
                return listOf(bestFit)
            }
        }
        values.addAll(exactBounds)

        val (numberLowerBounds, generalLowerBounds) =
            filterBounds(
                bounds,
                TypeBounds.BoundKind.LOWER_BOUND,
                values
            ).partition { it.constructor is IntegerValueTypeConstructor }

        val superTypeOfLowerBounds = CommonSupertypes.commonSupertypeForNonDenotableTypes(generalLowerBounds)
        if (tryPossibleAnswer(bounds, superTypeOfLowerBounds)) {
            return setOf(superTypeOfLowerBounds!!)
        }
        values.addIfNotNull(superTypeOfLowerBounds)

        //todo
        //fun <T> foo(t: T, consumer: Consumer<T>): T
        //foo(1, c: Consumer<Any>) - infer Int, not Any here

        val superTypeOfNumberLowerBounds = commonSupertypeForNumberTypes(numberLowerBounds)
        if (tryPossibleAnswer(bounds, superTypeOfNumberLowerBounds)) {
            return setOf(superTypeOfNumberLowerBounds!!)
        }
        values.addIfNotNull(superTypeOfNumberLowerBounds)

        if (superTypeOfLowerBounds != null && superTypeOfNumberLowerBounds != null) {
            val superTypeOfAllLowerBounds =
                CommonSupertypes.commonSupertypeForNonDenotableTypes(
                    listOf(
                        superTypeOfLowerBounds,
                        superTypeOfNumberLowerBounds
                    )
                )
            if (tryPossibleAnswer(bounds, superTypeOfAllLowerBounds)) {
                return setOf(superTypeOfAllLowerBounds!!)
            }
        }

        val upperBounds = filterBounds(bounds, TypeBounds.BoundKind.UPPER_BOUND, values)
        if (upperBounds.isNotEmpty()) {
            val intersectionOfUpperBounds = TypeIntersector.intersectTypes(upperBounds)
            if (intersectionOfUpperBounds != null && tryPossibleAnswer(bounds, intersectionOfUpperBounds)) {
                return setOf(intersectionOfUpperBounds)
            }
        }

        values.addAll(filterBounds(bounds, TypeBounds.BoundKind.UPPER_BOUND))

        if (values.size == 1 && typeVariable.hasOnlyInputTypesAnnotation() && !tryPossibleAnswer(
                bounds,
                values.first()
            )
        ) return listOf()

        return values
    }

    override val values: Collection<CangJieType>
        get() {
            if (resultValues == null) {
                resultValues = computeValues()
            }
            return resultValues!!
        }
}
