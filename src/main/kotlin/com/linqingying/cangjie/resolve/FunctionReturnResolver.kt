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

package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.linqingying.cangjie.diagnostics.Errors.TYPE_MISMATCH_MULTIPLE_SUPERTYPES
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjBlockExpression
import com.linqingying.cangjie.psi.CjFunction
import com.linqingying.cangjie.psi.CjThisExpression
import com.linqingying.cangjie.resolve.calls.CallResolver
import com.linqingying.cangjie.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import com.linqingying.cangjie.resolve.calls.util.CallMaker
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.SimpleClassicTypeSystemContext
import com.linqingying.cangjie.types.error.ErrorType
import com.linqingying.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo


//分析方法返回值
class FunctionReturnResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val typeResolver: TypeResolver,
    val expressionTypingServices: ExpressionTypingServices,
    val languageVersionSettings: LanguageVersionSettings
) {
    private class ReturnOfTypeParameterDescriptor(
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,

        name: Name,
        index: Int,
        storageManager: StorageManager
    ) : AbstractTypeParameterDescriptor(
        storageManager,
        containingDeclaration,
        annotations,

        name,
        Variance.INVARIANT,
        index,
        SourceElement.NO_SOURCE,

        SupertypeLoopChecker.EMPTY,

        ) {
        private val upperBounds: List<CangJieType> = ArrayList<CangJieType>(1).apply {
            add(containingDeclaration.builtIns.defaultBound)
        }


        override fun reportSupertypeLoopError(type: CangJieType) {

        }

        override fun getTypeConstructor(): TypeConstructor {
            return object : TypeConstructor {
                override fun getSupertypes(): List<CangJieType> {
                    return upperBounds
                }

                override fun equals(other: Any?): Boolean {
                    return this.hashCode() == other.hashCode()
                }

                override fun hashCode(): Int {
                    return -728150917
                }

                override fun getBuiltIns(): CangJieBuiltIns {
                    return containingDeclaration.builtIns
                }

                override fun isDenotable(): Boolean {
                    return false
                }

                override fun toString(): String {
                    return "T"
                }

                override fun getDeclarationDescriptor(): ClassifierDescriptor {
                    return this@ReturnOfTypeParameterDescriptor
                }

//                override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
//                    return false
//                }

                @TypeRefinement
                override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
                    return this
                }

                override fun isFinal(): Boolean {
                    return true
                }

                override fun getParameters(): List<TypeParameterDescriptor> {
                    return emptyList()
                }

            }
        }

        override fun getUpperBounds(): List<CangJieType> {
            return upperBounds
        }

        override fun resolveUpperBounds(): List<CangJieType> {
            return upperBounds
        }


        companion object {
            fun createWithDefaultBound(
                containingDeclaration: DeclarationDescriptor,
                annotations: Annotations,  //            boolean reified,
                variance: Variance,
                name: Name,
                index: Int,
                storageManager: StorageManager
            ): TypeParameterDescriptor {
                val typeParameterDescriptor = ReturnOfTypeParameterDescriptor(
                    containingDeclaration,
                    annotations,

                    name,
                    index,

                    storageManager
                )
//                typeParameterDescriptor.addUpperBound(containingDeclaration.builtIns.defaultBound)
//                typeParameterDescriptor.setInitialized()
                return typeParameterDescriptor
            }
        }

    }

    private inner class ReturnOfFunctionDescriptor : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.returnOfName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {
        init {
            val arrayType = module.builtIns.arrayType

            val t = ReturnOfTypeParameterDescriptor.createWithDefaultBound(
                this,
                Annotations.EMPTY,
                Variance.INVARIANT,
                Name.identifier("T"),
                0,

                module.builtIns.storageManager
            )
//            arrayType.arguments
            initialize(
                null, null, listOf(), listOf(
                    t
                ), listOf(
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this,
                        null,
                        0,
                        Annotations.EMPTY,
                        Name.identifier("elements"),
                        false,
                        arrayType,
                        false,
                        SourceElement.NO_SOURCE,
                        { emptyList() }
                    )), t.defaultType,
                Modality.FINAL,
                PUBLIC

            )
        }

    }

    fun resolveFunctionReturn(
        function: CjFunction,
        context: ExpressionTypingContext,
    ): CangJieType? {
        return resolveFunctionReturn(function.bodyBlockExpression!!, context)
    }

    fun resolveFunctionReturn(
        blockExpression: CjBlockExpression,
        context: ExpressionTypingContext,
    ): CangJieType? {


        val returns = blockExpression.getStatementsWithoutReturnKeyword()
        if (returns.isEmpty()) {
            return module.builtIns.unitType
        }

        val typeInfos = mutableListOf<CangJieTypeInfo>()

        val context = context.replaceIsSaveTypeInfo(false)

//        清除缓存
//         TODO 如果其他方法使用了该方法作为返回，但是由于该方法更新了返回值，其其他方法没有更新，所以出现检查没有执行
//        TODO 这里重构还是写一种更新检查的线程
        (context.trace as? DelegatingBindingTrace)?.clearTraceCache()
        returns.forEach {


            it?.let {
                val typeInfo = expressionTypingServices.getTypeInfo(it, context)


                if (typeInfo.type is ErrorType) {
//                typeInfo.type.intersectedTypes.forEach {
//                    typeInfos.add(createTypeInfo(it))
//                }
//                已经报告过错误
                    return typeInfo.type
                } else {
                    typeInfos.add(typeInfo)

                }
            }
        }


        val resultType = typeInfos.mapNotNull {
            it.type
        }.let {
            if (it.isEmpty()) return null

            SimpleClassicTypeSystemContext.commonSuperType(
                it
            )
        }
        if (resultType is MultipleSupertypeTypeInferenceFailure) {
            context.trace.report(TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(blockExpression, resultType.intersectedTypes))
        }
        //        语句中是否只有this表达式
        fun isOnlyThisExpression(): Boolean {
            returns.forEach {
                if (it !is CjThisExpression) {
                    return false
                }
            }
            return true
        }

        if (isOnlyThisExpression()) {
            (resultType as? CangJieType)?.let {
                return ThisType(it as SimpleType)
            }
        }
        return resultType as? CangJieType
        val call = CallMaker.makeCallForBlock(blockExpression)

        val functionDescriptors = listOf(ReturnOfFunctionDescriptor())

        val resolutionResults = callResolver.resolveBloackReturnCallWithGivenDescriptor(
            context,
            blockExpression,
            call,
            functionDescriptors
        )
//        if (!resolutionResults.isSingleResult) {
//            return noTypeInfo(context)
//        }


//        context.trace.record(COLLECTION_LITERAL_CALL, expression, resolutionResults.resultingCall)
        return resolutionResults.resultingDescriptor.returnType

    }
}
