package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.ForkPointData
import com.huawei.cangjie.resolve.calls.inference.model.IncorporationConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.huawei.cangjie.types.model.*

class VariableFixationFinder(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
//    private val languageVersionSettings: LanguageVersionSettings,
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
        val postponedTypeVariables: List<TypeVariableMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * See [org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize]
         */
        val outerSystemVariablesPrefixSize: Int

        val outerTypeVariables: Set<TypeConstructorMarker>?
            get() =
                when {
                    outerSystemVariablesPrefixSize > 0 -> allTypeVariables.keys.take(outerSystemVariablesPrefixSize)
                        .toSet()

                    else -> null
                }

        /**
         * If not null, that property means that we should assume temporary them all as proper types when fixating some variables.
         *
         * By default, if that property is null, we assume all `allTypeVariables` as not proper.
         *
         * Currently, that is only used for `provideDelegate` resolution, see
         * [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
         */
        val typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>?

        fun isReified(variable: TypeVariableMarker): Boolean
    }
}
fun TypeSystemInferenceExtensionContext.extractProjectionsForAllCapturedTypes(baseType: CangJieTypeMarker): Set<CangJieTypeMarker> {
//    if (baseType.isFlexible()) {
//        val flexibleType = baseType.asFlexibleType()!!
//        return buildSet {
//            addAll(extractProjectionsForAllCapturedTypes(flexibleType.lowerBound()))
//            addAll(extractProjectionsForAllCapturedTypes(flexibleType.upperBound()))
//        }
//    }
//    val simpleBaseType = baseType.asSimpleType()?.originalIfDefinitelyNotNullable()
//
//    return buildSet {
//        val projectionType = if (simpleBaseType is CapturedTypeMarker) {
//            val typeArgument = simpleBaseType.typeConstructorProjection().takeIf { !it.isStarProjection() } ?: return@buildSet
//            typeArgument.getType().also(::add)
//        } else baseType
//        val argumentsCount = projectionType.argumentsCount().takeIf { it != 0 } ?: return@buildSet
//
//        for (i in 0 until argumentsCount) {
//            val typeArgument = projectionType.getArgument(i).takeIf { !it.isStarProjection() } ?: continue
//            addAll(extractProjectionsForAllCapturedTypes(typeArgument.getType()))
//        }
//    }
    return emptySet()
}
fun TypeSystemInferenceExtensionContext.containsTypeVariable(type: CangJieTypeMarker, typeVariable: TypeConstructorMarker): Boolean {
    if (type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }) return true

    val typeProjections = extractProjectionsForAllCapturedTypes(type)

    return typeProjections.any { typeProjectionsType ->
        typeProjectionsType.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }
    }
}
