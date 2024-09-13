package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
import com.huawei.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.model.FixVariableConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.huawei.cangjie.resolve.calls.model.FunctionExpression
import com.huawei.cangjie.resolve.calls.model.LambdaCangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import com.huawei.cangjie.resolve.calls.model.ResolvedAtom
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeVariableMarker
import com.huawei.cangjie.types.util.unCapture as unCaptureCangJieType

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

