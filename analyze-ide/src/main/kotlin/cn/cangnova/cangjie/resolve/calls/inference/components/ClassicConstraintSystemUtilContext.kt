/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve.calls.inference.components

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
import cn.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
import cn.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import cn.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE
import cn.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import cn.cangnova.cangjie.resolve.calls.inference.model.*
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.model.CangJieTypeMarker
import cn.cangnova.cangjie.types.model.TypeVariableMarker
import cn.cangnova.cangjie.types.util.unCapture as unCaptureCangJieType

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

