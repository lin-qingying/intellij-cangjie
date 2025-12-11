/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.diagnostics.infos.errors.TYPE_MISMATCH_MULTIPLE_SUPERTYPES
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjThisExpression
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices


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
        override val upperBounds: List<CangJieType> = ArrayList<CangJieType>(1).apply {
            add(containingDeclaration.builtIns.defaultBound)
        }


        override fun reportSupertypeLoopError(type: CangJieType) {

        }

        override val typeConstructor: TypeConstructor
            get() =
                object : TypeConstructor {


                    override val supertypes: Collection<CangJieType>
                        get() = upperBounds

                    override fun equals(other: Any?): Boolean {
                        return this.hashCode() == other.hashCode()
                    }

                    override fun hashCode(): Int {
                        return -728150917
                    }

                    override val builtIns: CangJieBuiltIns
                        get() = containingDeclaration.builtIns
                    override val isDenotable: Boolean
                        get() = TODO("Not yet implemented")


                    override fun toString(): String {
                        return "T"
                    }


                    override val declarationDescriptor: ClassifierDescriptor?
                        get() = this@ReturnOfTypeParameterDescriptor

//                override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
//                    return false
//                }


                    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
                        return this
                    }

                    override val isFinal: Boolean
                        get() = false

                    override val parameters: List<TypeParameterDescriptor>
                        get() = emptyList()


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


        val returns = blockExpression.statementsWithoutReturnKeyword
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


            it.let {
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
        return resolutionResults.getResultingDescriptor().returnType

    }
}
