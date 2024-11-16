package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.createFunctionType
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.resolve.calls.CallResolver
import com.linqingying.cangjie.resolve.calls.util.CallMaker
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

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
