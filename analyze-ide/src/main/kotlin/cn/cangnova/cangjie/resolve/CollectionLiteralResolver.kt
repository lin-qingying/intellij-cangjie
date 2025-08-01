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

import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import cn.cangnova.cangjie.diagnostics.DiagnosticFactory1
import cn.cangnova.cangjie.diagnostics.Errors.VARRAY_SIZE_MISMATCH
import cn.cangnova.cangjie.incremental.CangJieLookupLocation
import cn.cangnova.cangjie.psi.CjCollectionLiteralExpression
import cn.cangnova.cangjie.resolve.BindingContext.COLLECTION_LITERAL_CALL
import cn.cangnova.cangjie.resolve.calls.CallResolver
import cn.cangnova.cangjie.resolve.calls.util.CallMaker
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext
import cn.cangnova.cangjie.types.expressions.ExpressionTypingServices
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import cn.cangnova.cangjie.types.util.isVArray
import cn.cangnova.cangjie.types.util.replaceArgument
import jakarta.inject.Inject

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
        private val upperBounds: List<CangJieType> = ArrayList<CangJieType>(1).apply {
            add(containingDeclaration.builtIns.defaultBound)
        }
        private val constructor = object : TypeConstructor {
            override fun getSupertypes(): List<CangJieType> {
                return emptyList()
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
                return "arrayOf"
            }

            override fun getDeclarationDescriptor(): ClassifierDescriptor? {
                return null
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

        override fun reportSupertypeLoopError(type: CangJieType) {

        }

//        override fun getTypeConstructor(): TypeConstructor {
//            return constructor
//        }

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
        val returnType: VArrayType
    ) : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.arrayOfName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {
        init {

            val arrayType = module.builtIns.arrayType.replaceArgument(
                returnType.arguments[0].type
            )

//            arrayType.arguments
            initialize(
                null, null, listOf(), listOf(

                ), listOf(ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
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
                )), returnType,
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
            val arrayType = module.builtIns.arrayType.replaceArgument(
                t.defaultType
            )

//            arrayType.arguments
            initialize(
                null, null, listOf(), listOf(
                    t
                ), listOf(ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
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
                )), arrayType,
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

        val memberScopeOfCangJiePackage = module.getPackage(StandardNames.BUILT_INS_PACKAGE_FQ_NAME).memberScope
        return memberScopeOfCangJiePackage.getContributedFunctions(callName, CangJieLookupLocation(expression))
    }

}

