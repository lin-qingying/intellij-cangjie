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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.createFunctionType
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import cn.cangnova.cangjie.psi.CjBinaryExpression
import cn.cangnova.cangjie.resolve.calls.CallResolver
import cn.cangnova.cangjie.resolve.calls.util.CallMaker
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import cn.cangnova.cangjie.utils.OperatorNameConventions
import cn.cangnova.cangjie.utils.exceptions.CangJieTypeInfo

class FlowOperatorResolver(

    val builtIns: CangJieBuiltIns,
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val functionDescriptorResolver: FunctionDescriptorResolver,
    val languageVersionSettings: LanguageVersionSettings
) {
    inner class FlowFunctionDescriptor(
        name: Name,

        ) : SimpleFunctionDescriptorImpl(
        builtIns.builtInsModule,
        null,
        Annotations.EMPTY,
        name, CallableMemberDescriptor.Kind.DECLARATION,
        SourceElement.NO_SOURCE
    ) {

        fun init(
            typeParameters: List<TypeParameterDescriptor>,
            valueParameters: List<ValueParameterDescriptor>,
            returnType: CangJieType
        ) {
            initialize(
                null,
                null,
                emptyList(),
                typeParameters,
                valueParameters,
                returnType,
                Modality.FINAL,
                DescriptorVisibilities.PUBLIC,
            )
        }
    }

    val funcMap = mutableMapOf<Name, FlowFunctionDescriptor>().apply {

        val pipeline = FlowFunctionDescriptor(
            OperatorNameConventions.PIPELINE
        ).apply {
            //
//    func Pipeline<L, R>(left: L, right: (L) -> R): R {
//        return right(  left    )
//    }
            val L = TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("L"),
                0, builtIns.storageManager
            )
            val R = TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("R"),
                1, builtIns.storageManager
            )

            val typeParameters = mutableListOf(L, R)

            val left = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                this, null, 0, Annotations.EMPTY, Name.identifier("left"),
                false, L.defaultType, false, SourceElement.NO_SOURCE,
            )
            val reightType = createFunctionType(
                builtIns, Annotations.EMPTY, null, emptyList(), listOf(L.defaultType), listOf(L.name), R.defaultType
            )
            val reight = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                this, null, 1, Annotations.EMPTY, Name.identifier("right"),
                false, reightType, false, SourceElement.NO_SOURCE,
            )
            val valueParameters = mutableListOf(left, reight)
            val returnType = R.defaultType

            init(typeParameters, valueParameters, returnType)

        }
        put(pipeline.name, pipeline)


        val composition = FlowFunctionDescriptor(OperatorNameConventions.COMPOSITION).apply {
            //    func Composition<L, R, RR>(left: (L) -> R, right: (R) -> RR): (L) -> R {
//        return left
//    }

            val L = TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("L"),
                0, builtIns.storageManager
            )
            val R = TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("R"),
                1, builtIns.storageManager
            )
            val RR = TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, Variance.INVARIANT, Name.identifier("RR"),
                2, builtIns.storageManager
            )
            val typeParameters = mutableListOf(L, R, RR)
            val leftType = createFunctionType(
                builtIns, Annotations.EMPTY, null, emptyList(), listOf(L.defaultType), listOf(L.name), R.defaultType
            )


            val left = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                this, null, 0, Annotations.EMPTY, Name.identifier("left"),
                false, leftType, false, SourceElement.NO_SOURCE,
            )
            val rightType = createFunctionType(
                builtIns, Annotations.EMPTY, null, emptyList(), listOf(R.defaultType), listOf(R.name), RR.defaultType
            )
            val right = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                this, null, 1, Annotations.EMPTY, Name.identifier("right"),
                false, rightType, false, SourceElement.NO_SOURCE,
            )
            val valueParameters = mutableListOf(left, right)

            val returnType = leftType

            init(typeParameters, valueParameters, returnType)
        }
        put(composition.name, composition)
    }


    fun resolveFlowOperator(
        referencedName: Name,
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val func = funcMap[referencedName] ?: return noTypeInfo(context)


        val call = CallMaker.makeCallForBinaryExpression(expression)
        val resolutionResults =
            callResolver.resolveBinaryCall(context, call, expression, listOf(func))
        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }

//
//        context.trace.record(COLLECTION_LITERAL_CALL, expression, resolutionResults.resultingCall)
        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)


    }
}
