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
import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import cn.cangnova.cangjie.psi.CjRangeExpression
import cn.cangnova.cangjie.resolve.BindingContext.RANGE_LITERAL_CALL
import cn.cangnova.cangjie.resolve.calls.CallResolver
import cn.cangnova.cangjie.resolve.calls.util.CallMaker
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo


class RangeLiteralResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val functionDescriptorResolver: FunctionDescriptorResolver,
    val languageVersionSettings: LanguageVersionSettings
) {
    fun resolveRangeLiteral(
        rangeExpression: CjRangeExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val call = CallMaker.makeCallForRangeLiteral(rangeExpression)
        if (call.valueArguments.size < 2) {
            return noTypeInfo(context)
        }


//        val factory= CjPsiFactory(rangeExpression.project)
//
//      val function =  factory.createFunction("public func rangeOf<T>(start:T,end:T,layer :Int64 ):Range<T> where  T <:   Countable<T> & Comparable<T> & Equatable<T> {}  ")
//
//      val  functionDescriptor = functionDescriptorResolver.resolveFunctionDescriptor(module.builtIns.builtInsModule,context.scope,function,context.trace,context.dataFlowInfo,null)

        val functionDescriptors = getRangeOfFunctionDescriptors()

        val resolutionResults =
            callResolver.resolveRangeLiteralCallWithGivenDescriptor(context, rangeExpression, call, functionDescriptors)
        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }


        context.trace.record(RANGE_LITERAL_CALL, rangeExpression, resolutionResults.resultingCall)
        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)


    }

    private fun getRangeOfFunctionDescriptors(): Collection<SimpleFunctionDescriptor> {
        return listOf(RangeOfFunctionDescriptor(), RangeOfFunctionDescriptor(false))

    }

    private inner class RangeOfFunctionDescriptor(
        val isStep: Boolean = true
    ) : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.rangeOfName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {
        init {
//            fo <T> rangeOf(start:T,end:T,layer :Int64 )

            val t = RangeOfTypeParameterDescriptor(
                this,
                Annotations.EMPTY,

                Name.identifier("T"),
                0,

                module.builtIns.storageManager
            )

            val rangeType = module.builtIns.rangeType


            initialize(
                null, null, listOf(), listOf(
                    t
                ), listOfNotNull(
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this,
                        null,
                        0,
                        Annotations.EMPTY,
                        Name.identifier("start"),
                        false,
                        t.defaultType,
                        false,
                        SourceElement.NO_SOURCE,
                        { emptyList() }
                    ),
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this,
                        null,
                        1,
                        Annotations.EMPTY,
                        Name.identifier("end"),
                        false,
                        t.defaultType,
                        false,
                        SourceElement.NO_SOURCE,
                        { emptyList() }
                    ),

                    if (isStep) {
                        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                            this,
                            null,
                            2,
                            Annotations.EMPTY,
                            Name.identifier("layer"),
                            true,
                            module.builtIns.int64Type,
                            false,
                            SourceElement.NO_SOURCE,
                            { emptyList() }
                        )
                    } else {
                        null
                    }
                ),

                rangeType,
                Modality.FINAL,
                PUBLIC

            )
        }
    }

    inner class RangeOfTypeParameterDescriptor(
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

        fun CangJieType.replaceArgument(): CangJieType {
            val arguments = listOf(
                TypeProjectionImpl(this@RangeOfTypeParameterDescriptor.defaultType)

            )
            return simpleTypeWithNonTrivialMemberScope(
                attributes,
                constructor,
                arguments,
                isMarkedOption,
                memberScope
            )
        }

        override fun getTypeConstructor(): TypeConstructor {
            return object : TypeConstructor {
                override fun getSupertypes(): List<CangJieType> {
                    return _upperBounds
                }

                override fun equals(other: Any?): Boolean {
                    return this.hashCode() == other.hashCode()
                }

                override fun hashCode(): Int {
                    return 984279647
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
                    return this@RangeOfTypeParameterDescriptor
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
            return _upperBounds
        }

        override fun reportSupertypeLoopError(type: CangJieType) {

        }

        private val _upperBounds: List<CangJieType>
            get() = ArrayList<CangJieType>(3).apply {
//           三个边界
//            Countable<T> & Comparable<T> & Equatable<T>


                add(module.builtIns.countableType.replaceArgument())
                add(module.builtIns.ccomparableType.replaceArgument())
                add(module.builtIns.equatableType.replaceArgument())
            }

        override fun resolveUpperBounds(): List<CangJieType> {
            return _upperBounds
        }

    }
}
