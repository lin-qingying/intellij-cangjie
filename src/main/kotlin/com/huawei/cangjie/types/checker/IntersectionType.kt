package com.huawei.cangjie.types.checker

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.types.*

import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.util.TypeUtils

fun intersectTypes(types: List<SimpleType>) = intersectTypes(types as List<UnwrappedType>) as SimpleType

fun intersectTypes(types: List<UnwrappedType>): UnwrappedType {
    when (types.size) {
        0 -> error("Expected some types")
        1 -> return types.single()
    }
    var hasFlexibleTypes = false
    var hasErrorType = false
    val lowerBounds = types.map {
        hasErrorType = hasErrorType || it.isError
        when (it) {
            is SimpleType -> it
            is FlexibleType -> {
                if (it.isDynamic()) return it

                hasFlexibleTypes = true
                it.lowerBound
            }
        }
    }
    if (hasErrorType) {
        return ErrorUtils.createErrorType(ErrorTypeKind.INTERSECTION_OF_ERROR_TYPES, types.toString())
    }

    if (!hasFlexibleTypes) {
        return TypeIntersector.intersectTypes(lowerBounds)
    }

    val upperBounds = types.map { it.upperIfFlexible() }
    /**
     * We should save this rules:
     *  - if for each type from types type is subtype of A, then intersectionType should be subtype of A
     *  - same for type B which is subtype of all types.
     *
     *  Note: when we construct intersection type of dynamic(or Raw type) & other type, we can get non-dynamic type.  // todo discuss
     */
    return CangJieTypeFactory.flexibleType(
        TypeIntersector.intersectTypes(lowerBounds),
        TypeIntersector.intersectTypes(upperBounds)
    )
}

object TypeIntersector {
    fun intersectTypes(types: Collection<CangJieType>): CangJieType? {
        assert(!types.isEmpty()) { "Attempting to intersect empty collection of types, this case should be dealt with on the call site." }

        if (types.size == 1) {
            return types.iterator().next()
        }

        // Intersection of T1..Tn is an intersection of their non-null versions,
        //   made nullable is they all were nullable
        var nothingOrNullableNothing: CangJieType? = null
        var allNullable = true
        val nullabilityStripped: MutableList<CangJieType> = java.util.ArrayList<CangJieType>(types.size)
        for (type in types) {
            if (type.isError) continue

            if (CangJieBuiltIns.isNothing (type)) {
                nothingOrNullableNothing = type
            }
            allNullable = allNullable and type.isMarkedOption
            nullabilityStripped.add(TypeUtils.makeNotNullable(type))
        }

        if (nothingOrNullableNothing != null) {
            return TypeUtils.makeNullableAsSpecified(nothingOrNullableNothing, allNullable)
        }

        if (nullabilityStripped.isEmpty()) {
            // All types were errors
            return ErrorUtils.createErrorType(ErrorTypeKind.INTERSECTION_OF_ERROR_TYPES, types.toString())
        }

        val typeChecker: CangJieTypeChecker = CangJieTypeChecker.DEFAULT
        // Now we remove types that have subtypes in the list
        val resultingTypes: MutableList<CangJieType> = java.util.ArrayList<CangJieType>()
        outer@ for (type in nullabilityStripped) {
//            if (!TypeUtils.canHaveSubtypes(typeChecker, type)) {
//                var relativeToAll = true
//                for (other in nullabilityStripped) {
//                    // It makes sense to check for subtyping (other <: type), despite that
//                    // type is not supposed to be open, for there're enums
//                    val mayBeEqual: Boolean =
//                      TypeIntersector.TypeUnifier.mayBeEqual(type, other)
//                    val relative = typeChecker.isSubtypeOf(type, other) || typeChecker.isSubtypeOf(other, type)
//                    if (!mayBeEqual && !relative) {
//                        return null
//                    } else if (!relative) {
//                        // To build T & (final A), instead of returning just A as intersection
//                        relativeToAll = false
//                        break
//                    }
//                }
//                if (relativeToAll) return TypeUtils.makeNullableAsSpecified(type, allNullable)
//            }
            for (other in nullabilityStripped) {
                if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                    continue@outer
                }
            }

            // Don't add type if it is already present, to avoid trivial type intersections in result
            for (other in resultingTypes) {
                if (typeChecker.equalTypes(other, type)) {
                    continue@outer
                }
            }
            resultingTypes.add(type)
        }

        if (resultingTypes.isEmpty()) {
            // If we ended up here, it means that all types from `nullabilityStripped` were excluded by the code above
            // most likely, this is because they are all semantically interchangeable (e.g. List<Foo>! and List<Foo>),
            // in that case, we can safely select the best representative out of that set and return it
            // TODO: maybe return the most specific among the types that are subtypes to all others in the `nullabilityStripped`?
            // TODO: e.g. among {Int, Int?, Int!}, return `Int` (now it returns `Int!`).
            var bestRepresentative = nullabilityStripped.singleBestRepresentative()

            if (bestRepresentative == null) {
                bestRepresentative =  hackForTypeIntersector(nullabilityStripped)
            }

            if (bestRepresentative == null) {
                return null
            }
            return TypeUtils.makeNullableAsSpecified(bestRepresentative, allNullable)
        }

        if (resultingTypes.size == 1) {
            return TypeUtils.makeNullableAsSpecified(resultingTypes[0], allNullable)
        }

        return IntersectionTypeConstructor(resultingTypes).createType()
    }
    internal fun intersectTypes(types: List<SimpleType>): SimpleType {
        assert(types.size > 1) {
            "Size should be at least 2, but it is ${types.size}"
        }

        val inputTypes = java.util.ArrayList<SimpleType>()
        for (type in types) {
            if (type.constructor is IntersectionTypeConstructor) {
                inputTypes.addAll(type.constructor.supertypes.map {
                    it.upperIfFlexible().let { if (type.isMarkedOption) it.makeNullableAsSpecified(true) else it }
                })
            } else {
                inputTypes.add(type)
            }
        }
        val resultNullability = inputTypes.fold(ResultNullability.START, ResultNullability::combine)

        /**
         * resultNullability. Value description:
         * ACCEPT_NULL means that all types marked nullable
         *
         * NOT_NULL means that there is one type which is subtype of Any => all types can be made definitely not null,
         * making types definitely not null (not just not null) makes sense when we have intersection of type parameters like {T!! & S}
         *
         * UNKNOWN means, that we do not know, i.e. more precisely, all singleClassifier types marked nullable if any,
         * and other types is captured types or type parameters without not-null upper bound. Example: `String? & T` such types we should leave as is.
         */
        val correctNullability = inputTypes.mapTo(LinkedHashSet()) {
            if (resultNullability == ResultNullability.NOT_NULL) {
                (if (it is NewCapturedType) it.withNotNullProjection() else it).makeSimpleTypeDefinitelyNotNullOrNotNull()
            } else it
        }

        val resultAttributes = types.map { it.attributes }.reduce { x, y -> x.intersect(y) }
        return intersectTypesWithoutIntersectionType(correctNullability).replaceAttributes(resultAttributes)
    }

    // nullability here is correct
    private fun intersectTypesWithoutIntersectionType(inputTypes: Set<SimpleType>): SimpleType {
        if (inputTypes.size == 1) return inputTypes.single()

        // Any and Nothing should leave
        // Note that duplicates should be dropped because we have Set here.
        val errorMessage = { "This collections cannot be empty! input types: ${inputTypes.joinToString()}" }

        val filteredEqualTypes = filterTypes(inputTypes, ::isStrictSupertype)
        assert(filteredEqualTypes.isNotEmpty(), errorMessage)

        IntegerLiteralTypeConstructor.findIntersectionType(filteredEqualTypes)?.let { return it }

        val filteredSuperAndEqualTypes = filterTypes(filteredEqualTypes, NewCangJieTypeChecker.Default::equalTypes)
        assert(filteredSuperAndEqualTypes.isNotEmpty(), errorMessage)

        if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

        return IntersectionTypeConstructor(inputTypes).createType()
    }

    private fun filterTypes(
        inputTypes: Collection<SimpleType>,
        predicate: (lower: SimpleType, upper: SimpleType) -> Boolean
    ): Collection<SimpleType> {
        val filteredTypes = ArrayList(inputTypes)
        val iterator = filteredTypes.iterator()
        while (iterator.hasNext()) {
            val upper = iterator.next()
            val shouldFilter = filteredTypes.any { lower -> lower !== upper && predicate(lower, upper) }

            if (shouldFilter) iterator.remove()
        }
        return filteredTypes
    }

    private fun isStrictSupertype(subtype: CangJieType, supertype: CangJieType): Boolean {
        return with(NewCangJieTypeChecker.Default) {
            isSubtypeOf(subtype, supertype) && !isSubtypeOf(supertype, subtype)
        }
    }

    /**
     * Let T is type parameter with upper bound Any?. resultNullability(String? & T) = UNKNOWN => String? & T
     */
    private enum class ResultNullability {
        START {
            override fun combine(nextType: UnwrappedType) = nextType.resultNullability
        },
        ACCEPT_NULL {
            override fun combine(nextType: UnwrappedType) = nextType.resultNullability
        },

        // example: type parameter without not-null supertype
        UNKNOWN {
            override fun combine(nextType: UnwrappedType) =
                nextType.resultNullability.let {
                    if (it == ACCEPT_NULL) this else it
                }
        },
        NOT_NULL {
            override fun combine(nextType: UnwrappedType) = this
        };

        abstract fun combine(nextType: UnwrappedType): ResultNullability

        protected val UnwrappedType.resultNullability: ResultNullability
            get() = when {
                isMarkedOption -> ACCEPT_NULL
                this is DefinitelyNotNullType && this.original is StubTypeForBuilderInference -> NOT_NULL
                this is StubTypeForBuilderInference -> UNKNOWN
                NullabilityChecker.isSubtypeOfAny(this) -> NOT_NULL
                else -> UNKNOWN
            }
    }
}
