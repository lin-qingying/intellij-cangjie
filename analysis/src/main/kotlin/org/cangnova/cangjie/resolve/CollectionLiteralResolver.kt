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

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.diagnostics.DiagnosticFactory1
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import jakarta.inject.Inject
import org.cangnova.cangjie.diagnostics.infos.errors.VARRAY_SIZE_MISMATCH
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.COLLECTION_LITERAL_CALL
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo

class CollectionLiteralResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val languageVersionSettings: LanguageVersionSettings,

    ) {
    private lateinit var expressionTypingServices: ExpressionTypingServices

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    fun resolveCollectionLiteral(
        collectionLiteralExpression: CjCollectionLiteralExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

        if (context.expectedType.isVArray) return resolveCollectionLiteralByVArray(collectionLiteralExpression, context)
//        when (computeKindOfContainer(collectionLiteralExpression)) {
//            AnnotationOrAnnotationClass -> {}
//            CompanionOfAnnotation -> {
//                val factory = when (context.languageVersionSettings.supportsFeature(ProhibitArrayLiteralsInCompanionOfAnnotation)) {
//                    true -> UNSUPPORTED
//                    false -> UNSUPPORTED_WARNING
//                }
//                reportUnsupportedLiteral(context, factory, collectionLiteralExpression)
//            }
//            ContainerKind.Other -> reportUnsupportedLiteral(context, UNSUPPORTED, collectionLiteralExpression)
//        }

        return resolveCollectionLiteralSpecialMethod(collectionLiteralExpression, context)
    }

    fun resolveCollectionLiteralByVArray(
        collectionLiteralExpression: CjCollectionLiteralExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        context.expectedType as VArrayType
        val expressionSize = collectionLiteralExpression.innerExpressions.size

        if (expressionSize != context.expectedType.size) {

            context.trace.report(
                VARRAY_SIZE_MISMATCH.on(
                    collectionLiteralExpression
                )
            )
//            报错长度不符
        }

//        校验类型
        val eContext = context.replaceExpectedType(context.expectedType.arguments[0].type)
        collectionLiteralExpression.innerExpressions.forEach {
            expressionTypingServices.expressionTypingFacade.getTypeInfo(
                it, eContext
            )
        }


        return createTypeInfo(context.expectedType)

    }

    private fun reportUnsupportedLiteral(
        context: ExpressionTypingContext,
        diagnosticFactory: DiagnosticFactory1<PsiElement, String>,
        collectionLiteralExpression: CjCollectionLiteralExpression
    ) {
        context.trace.report(
            diagnosticFactory.on(
                collectionLiteralExpression,
                "Collection literals outside of annotations"
            )
        )
    }

    private fun resolveCollectionLiteralSpecialMethod(
        expression: CjCollectionLiteralExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val call = CallMaker.makeCallForCollectionLiteral(expression)


        val functionDescriptors = getArrayOfFunctionDescriptors()

        val resolutionResults =
            callResolver.resolveCollectionLiteralCallWithGivenDescriptor(context, expression, call, functionDescriptors)
        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }


        context.trace.record(COLLECTION_LITERAL_CALL, expression, resolutionResults.resultingCall)
        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)


    }

    private class ArrayOfTypeParameterDescriptor(
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,

        name: Name,
        index: Int,
        storageManager: StorageManager,
        upperBound: List<CangJieType> = listOf(containingDeclaration.builtIns.defaultBound)
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
        private val constructor = object : TypeConstructor {


            override val supertypes: Collection<CangJieType>
                get() = emptyList()
            override fun equals(other: Any?): Boolean {
                return this.hashCode() == other.hashCode()
            }

            override fun hashCode(): Int {
                return -728150917
            }



            override val builtIns: CangJieBuiltIns
                get() = containingDeclaration.builtIns


            override val isDenotable: Boolean
                get() = false
            override fun toString(): String {
                return "arrayOf"
            }

            override val declarationDescriptor: ClassifierDescriptor?
                get() = null


            override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
                return this
            }

            override val isFinal: Boolean
                get() = true
            override val parameters: List<TypeParameterDescriptor>
                get() = emptyList()


        }

        override fun reportSupertypeLoopError(type: CangJieType) {

        }

//        override fun getTypeConstructor(): TypeConstructor {
//            return constructor
//        }



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
                storageManager: StorageManager,
                upperBound: List<CangJieType> = listOf(containingDeclaration.builtIns.defaultBound)
            ): TypeParameterDescriptor {
                val typeParameterDescriptor = ArrayOfTypeParameterDescriptor(
                    containingDeclaration,
                    annotations,

                    name,
                    index,

                    storageManager,
                    upperBound
                )
//                typeParameterDescriptor.addUpperBound(containingDeclaration.builtIns.defaultBound)
//                typeParameterDescriptor.setInitialized()
                return typeParameterDescriptor
            }
        }

    }


    //        使用调用函数的方式解析数组字面量
//        func arrayOf<T>(elements:Array<T>):VArray<T>
    private inner class VArrayOfFunctionDescriptor(
        override val returnType: VArrayType
    ) : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.arrayOfName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {
        init {

            val arrayType = module.builtIns.stdlibTypes.arrayType.replaceArgument(
                returnType.arguments[0].type
            )

//            arrayType.arguments
            initialize(
                null,     listOf(

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
                        SourceElement.NO_SOURCE
                    ) ), returnType,
                Modality.FINAL,
                PUBLIC

            )
        }

    }

    private inner class ArrayOfFunctionDescriptor : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.arrayOfName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {
        init {
            val t = ArrayOfTypeParameterDescriptor.createWithDefaultBound(
                this,
                Annotations.EMPTY,
                Variance.INVARIANT,
                Name.identifier("T"),
                0,

                module.builtIns.storageManager
            )
            val arrayType = module.builtIns.stdlibTypes.arrayType.replaceArgument(
                t.defaultType
            )

//            arrayType.arguments
            initialize(
                 null,  listOf(
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
                        SourceElement.NO_SOURCE
                    ) ), arrayType,
                Modality.FINAL,
                PUBLIC

            )
        }

    }


    private fun getArrayOfFunctionDescriptors(): Collection<SimpleFunctionDescriptor> {
        return listOf(ArrayOfFunctionDescriptor())

    }

    private fun getFunctionDescriptorForCollectionLiteral(
        expression: CjCollectionLiteralExpression,
        callName: Name
    ): Collection<SimpleFunctionDescriptor> {

        val memberScopeOfCangJiePackage = module.getPackage(StandardNames.BASIC_PACKAGE_FQ_NAME).memberScope
        return memberScopeOfCangJiePackage.getContributedFunctions(callName, CangJieLookupLocation(expression))
    }

}

