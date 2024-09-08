package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import com.huawei.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeVariableMarker
import com.huawei.cangjie.types.model.TypeVariableTypeConstructorMarker

private typealias Context = ConstraintSystemCompletionContext
private typealias ResolvedAtomProvider = (TypeVariableMarker) -> Any?

class PostponedArgumentInputTypesResolver(
    private val resultTypeResolver: ResultTypeResolver,
    private val variableFixationFinder: VariableFixationFinder,
    private val resolutionTypeSystemContext: ConstraintSystemUtilContext,
//    private val languageVersionSettings: LanguageVersionSettings,
)
{
    fun collectParameterTypesAndBuildNewExpectedTypes(
        c: Context,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        completionMode: ConstraintSystemCompletionMode,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        topLevelTypeVariables: Set<TypeVariableTypeConstructorMarker>
    ): Boolean = with(resolutionTypeSystemContext) {
        // We can collect parameter types from declaration in any mode, they can't change during completion.
        for (argument in postponedArguments) {
            if (argument !is LambdaWithTypeVariableAsExpectedTypeMarker) continue
            if (argument.parameterTypesFromDeclaration != null) continue
            argument.updateParameterTypesFromDeclaration(extractLambdaParameterTypesFromDeclaration(argument))
        }

        return postponedArguments.any { argument ->
            /*
             * We can build new functional expected types in partial mode only for anonymous functions,
             * because more exact type can't appear from constraints in full mode (anonymous functions have fully explicit declaration).
             * It can be so for lambdas: for instance, an extension function type can appear in full mode (it may not be known in partial mode).
             *
             * TODO: investigate why we can't do it for anonymous functions in full mode always (see `diagnostics/tests/resolve/resolveWithSpecifiedFunctionLiteralWithId.kt`)
             */
//            if (completionMode == ConstraintSystemCompletionMode.PARTIAL && !argument.isFunctionExpression())
//                return@any false
//            if (argument.revisedExpectedType != null) return@any false
//            val parameterTypesInfo =
//                c.extractParameterTypesInfo(argument, postponedArguments, dependencyProvider) ?: return@any false
//            val newExpectedType =
//                c.buildNewFunctionalExpectedType(argument, parameterTypesInfo, dependencyProvider, topLevelTypeVariables)
//                    ?: return@any false
//
//            argument.reviseExpectedType(newExpectedType)

            true
        }
    }


    private fun PostponedResolvedAtomMarker.expectedFunctionType(c: Context): CangJieTypeMarker? = with(c) {
        val expectedType = (this@expectedFunctionType as? PostponedAtomWithRevisableExpectedType)?.revisedExpectedType ?: expectedType
        expectedType?.takeIf { it.isFunctionWithAny() }
    }
    private fun Context.getAllDeeplyRelatedTypeVariables(
        type: CangJieTypeMarker,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
    ): Collection<TypeVariableTypeConstructorMarker> {
        val collectedVariables = mutableSetOf<TypeVariableTypeConstructorMarker>()
        getAllDeeplyRelatedTypeVariables(type, variableDependencyProvider, collectedVariables)
        return collectedVariables
    }
    private fun Context.getAllDeeplyRelatedTypeVariables(
        type: CangJieTypeMarker,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
        typeVariableCollector: MutableSet<TypeVariableTypeConstructorMarker>
    ) {
        val typeConstructor = type.typeConstructor()

        when {
            typeConstructor is TypeVariableTypeConstructorMarker -> {
                val relatedVariables = variableDependencyProvider.getDeeplyDependentVariables(typeConstructor).orEmpty()
                typeVariableCollector.add(typeConstructor)
                typeVariableCollector.addAll(relatedVariables.filterIsInstance<TypeVariableTypeConstructorMarker>())
            }
            type.argumentsCount() > 0 -> {
                for (typeArgument in type.lowerBoundIfFlexible().asArgumentList()) {

                        getAllDeeplyRelatedTypeVariables(typeArgument.getType(), variableDependencyProvider, typeVariableCollector)

                }
            }
        }
    }
    private fun Context.findNextVariableForParameterType(
        type: CangJieTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: CangJieTypeMarker,
    ): VariableFixationFinder.VariableForFixation? {
        val outerTypeVariables = outerTypeVariables.orEmpty()
        val relatedVariables = type.extractArgumentsForFunctionTypeOrSubtype()
            .flatMap { getAllDeeplyRelatedTypeVariables(it, dependencyProvider) }
            .filter { it !in outerTypeVariables }

        return variableFixationFinder.findFirstVariableForFixation(
            this,
            relatedVariables,
            postponedArguments,
            ConstraintSystemCompletionMode.FULL,
            topLevelType,
        )
    }
    private fun Context.fixNextReadyVariableForParameterType(
        type: CangJieTypeMarker,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: CangJieTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        resolvedAtomByTypeVariableProvider: ResolvedAtomProvider,
    ): Boolean = with(resolutionTypeSystemContext) {
        val variableForFixation = findNextVariableForParameterType(type, dependencyProvider, postponedArguments, topLevelType)

        if (variableForFixation == null || !variableForFixation.isReady)
            return false

        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        val resultType =
            resultTypeResolver.findResultType(
                this@fixNextReadyVariableForParameterType,
                variableWithConstraints,
                TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
            )
        val variable = variableWithConstraints.typeVariable

        fixVariable(
            variable,
            resultType,
            createFixVariableConstraintPosition(variable, resolvedAtomByTypeVariableProvider(variable))
        )

        return true
    }
    fun fixNextReadyVariableForParameterTypeIfNeeded(
        c:  Context,
        argument: PostponedResolvedAtomMarker,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: CangJieTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        resolvedAtomProvider: ResolvedAtomProvider,
    ): Boolean {
        val expectedType = argument.expectedFunctionType(c) ?: return false

        return c.fixNextReadyVariableForParameterType(
            expectedType,
            postponedArguments,
            topLevelType,
            dependencyProvider,
            resolvedAtomProvider,
        )
    }
}
