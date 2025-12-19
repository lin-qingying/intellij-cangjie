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
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjRangeExpression
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.RANGE_LITERAL_CALL
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo


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

            val rangeType = module.builtIns.stdlibTypes.rangeType


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
                isOption,
                memberScope
            )
        }

        override val typeConstructor: TypeConstructor
            get() = object : TypeConstructor {

                override val supertypes: Collection<CangJieType>
                    get() = _upperBounds

                override fun equals(other: Any?): Boolean {
                    return this.hashCode() == other.hashCode()
                }

//                override fun hashCode(): Int {
//                    return 984279647
//                }


                override val builtIns: CangJieBuiltIns
                    get() = containingDeclaration.builtIns
                override val isDenotable: Boolean
                    get() = false


                override fun toString(): String {
                    return "T"
                }

                override val declarationDescriptor: ClassifierDescriptor
                    get() = this@RangeOfTypeParameterDescriptor


//                override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
//                    return false
//                }


                override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
                    return this
                }


                override val isFinal: Boolean
                    get() = true
                override val parameters: List<TypeParameterDescriptor>
                    get() = emptyList()


            }


        override val upperBounds: List<CangJieType>
            get() = _upperBounds

        override fun reportSupertypeLoopError(type: CangJieType) {

        }

        private val _upperBounds: List<CangJieType>
            get() = ArrayList<CangJieType>(3).apply {
//           三个边界
//            Countable<T> & Comparable<T> & Equatable<T>


                add(module.builtIns.stdlibTypes.countableType.replaceArgument())
                add(module.builtIns.stdlibTypes.comparableType.replaceArgument())
                add(module.builtIns.stdlibTypes.equatableType.replaceArgument())
            }

        override fun resolveUpperBounds(): List<CangJieType> {
            return _upperBounds
        }

    }
}
