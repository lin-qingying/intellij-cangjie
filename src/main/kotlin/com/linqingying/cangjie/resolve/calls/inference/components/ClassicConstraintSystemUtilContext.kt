package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
import com.linqingying.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
import com.linqingying.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import com.linqingying.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE
import com.linqingying.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import com.linqingying.cangjie.resolve.calls.inference.model.*
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.model.CangJieTypeMarker
import com.linqingying.cangjie.types.model.TypeVariableMarker
import com.linqingying.cangjie.types.util.unCapture as unCaptureCangJieType

class ClassicConstraintSystemUtilContext(
    val cangjieTypeRefiner: CangJieTypeRefiner,
    val builtIns: CangJieBuiltIns,
) : ConstraintSystemUtilContext {
//    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
//        return this is TypeVariableFromCallableDescriptor && this.originalTypeParameter.shouldBeFlexible()
//    }
//
    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        require(this is NewTypeVariable)
        return hasOnlyInputTypesAnnotation()
    }

    override fun CangJieTypeMarker.unCapture(): CangJieTypeMarker {
        require(this is CangJieType)
        return unCaptureCangJieType().unwrap()
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return TypeVariableForLambdaReturnType(
            builtIns,
            TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
        )
    }


    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return TypeVariableForCallableReferenceReturnType(
            builtIns,
            TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
        )
    }

    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
    return this is TypeVariableFromCallableDescriptor && this.originalTypeParameter.shouldBeFlexible()
}
    override fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>? {
        require(declaration is ResolvedAtom)
        return when (val atom = declaration.atom) {
            is FunctionExpression -> {
                val receiverType = atom.receiverType
                if (receiverType != null) listOf(receiverType) + atom.parametersTypes else atom.parametersTypes.toList()
            }

            is LambdaCangJieCallArgument -> atom.parametersTypes?.toList()
            else -> null
        }
    }
    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*> {
        require(argument is ResolvedAtom)
        return ArgumentConstraintPositionImpl(argument.atom as CangJieCallArgument)
    }

    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        require(argument is ResolvedAtom)
        val atom = argument.atom as PostponableCangJieCallArgument
        return TypeVariableForLambdaParameterType(
            atom,
            index,
            builtIns,
            TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + (index + 1)
        )
    }

    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return TypeVariableForCallableReferenceParameterType(
            builtIns,
            TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + (index + 1)
        )
    }
    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        require(this is ResolvedAtom)
        return this.atom is FunctionExpression
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean {
        require(this is ResolvedAtom)
        val atom = this.atom
        return atom is FunctionExpression && atom.receiverType != null
    }

    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        require(this is ResolvedAtom)
        val atom = this.atom
        return atom is LambdaCangJieCallArgument && atom !is FunctionExpression
    }

    override fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T> {
        require(atom is ResolvedAtom)
        @Suppress("UNCHECKED_CAST")
        return FixVariableConstraintPositionImpl(variable, atom) as FixVariableConstraintPosition<T>
    }




}

