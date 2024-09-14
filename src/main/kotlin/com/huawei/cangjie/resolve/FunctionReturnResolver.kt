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
import com.huawei.cangjie.diagnostics.Errors.TYPE_MISMATCH_MULTIPLE_SUPERTYPES
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjBlockExpression
import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.resolve.calls.CallResolver
import com.huawei.cangjie.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import com.huawei.cangjie.resolve.calls.util.CallMaker
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeRefinement
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo


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
        returns.forEach {

            val typeInfo = expressionTypingServices.getTypeInfo(it, context)

//        清除本次分析数据
//            BindingContextUtils.clear(context.trace)
//            不留存类型数据
//BindingContextUtils.updateRecordedType(noTypeInfo(context.dataFlowInfo),it,context.trace,false)
//            BindingContextUtils.removeBySlice(BindingContext.EXPRESSION_TYPE_INFO, it,context.trace)

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


        val resultType = SimpleClassicTypeSystemContext.commonSuperType(
            typeInfos.mapNotNull {
                it.type
            }
        )
        if (resultType is MultipleSupertypeTypeInferenceFailure) {
            context.trace.report(TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(blockExpression, resultType.intersectedTypes))
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
