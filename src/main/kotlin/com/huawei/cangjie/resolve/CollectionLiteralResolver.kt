package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.huawei.cangjie.diagnostics.DiagnosticFactory1
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjCollectionLiteralExpression
import com.huawei.cangjie.resolve.BindingContext.COLLECTION_LITERAL_CALL
import com.huawei.cangjie.resolve.calls.CallResolver
import com.huawei.cangjie.resolve.calls.util.CallMaker
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeRefinement
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo
import com.intellij.psi.PsiElement

class CollectionLiteralResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val languageVersionSettings: LanguageVersionSettings
) {
    fun resolveCollectionLiteral(
        collectionLiteralExpression: CjCollectionLiteralExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
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

//        val callName = getArrayFunctionCallName(context.expectedType)
//        val functionDescriptors = getFunctionDescriptorForCollectionLiteral(expression, callName)
//        if (functionDescriptors.isEmpty()) {
//            context.trace.report(
//                MISSING_STDLIB.on(
//                    expression, "Collection literal call '$callName()' is unresolved"
//                )
//            )
//            return noTypeInfo(context)
//        }


//        val resolutionResults =
//            callResolver.resolveCollectionLiteralCallWithGivenDescriptor(context, expression, call, functionDescriptors)
//

//

//        return noTypeInfo(context)

    }

    private class ArrayOfTypeParameterDescriptor(
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
                val typeParameterDescriptor = ArrayOfTypeParameterDescriptor(
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

    //        使用调用函数的方式解析数组字面量
//        func arrayOf<T>(elements:Array<T>):Array<T>
    private inner class ArrayOfFunctionDescriptor : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.arrayOfName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {




        init {
            val arrayType = module.builtIns.arrayType

            arrayType.arguments
            initialize(
                null, null, listOf(), listOf(
                    ArrayOfTypeParameterDescriptor.createWithDefaultBound(
                        this,
                        Annotations.EMPTY,
                        Variance.INVARIANT,
                        Name.identifier("T"),
                        0,

                        module.builtIns.storageManager
                    )
                ), listOf(ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                    this,
                    null,
                    0,
                    Annotations.EMPTY,
                    Name.identifier("elements"),
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

//    private enum class ContainerKind {
//        AnnotationOrAnnotationClass,
//        CompanionOfAnnotation,
//        Other
//    }

//    private fun computeKindOfContainer(expression: CjCollectionLiteralExpression): ContainerKind {
//
//        return             ContainerKind.   Other
//        val parent = PsiTreeUtil.getParentOfType(expression, CjAnnotationEntry::class.java, CjClass::class.java, CjEnum::class.java, CjStruct::class.java,CjInterface::class.java)

//        return if (parent is CjAnnotationEntry || (parent is CjClass && parent.isAnnotation())) {
//            ContainerKind. AnnotationOrAnnotationClass
//        } else {
//            ContainerKind.   Other
//        }
//    }

//    private fun getArrayFunctionCallName(expectedType: CangJieType): Name {
//        if (TypeUtils.noExpectedType(expectedType) ||
//            !(CangJieBuiltIns.isPrimitiveArray(expectedType) /*|| CangJieBuiltIns.isUnsignedArrayType(expectedType)*/)
//        ) {
//            return ArrayFqNames.ARRAY_OF_FUNCTION
//        }
//
//        val descriptor = expectedType.constructor.declarationDescriptor ?: return ArrayFqNames.ARRAY_OF_FUNCTION
//
//        return ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY[CangJieBuiltIns.getPrimitiveArrayType(descriptor)]
//            ?: UnsignedTypes.unsignedArrayTypeToArrayCall[UnsignedTypes.toUnsignedArrayType(descriptor)]
//            ?: ArrayFqNames.ARRAY_OF_FUNCTION
//    }
}

