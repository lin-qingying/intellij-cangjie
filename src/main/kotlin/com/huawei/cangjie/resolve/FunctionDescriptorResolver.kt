package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.*
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.Errors.*
import com.huawei.cangjie.descriptors.annotations.AnnotationSplitter
import com.huawei.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.psiUtil.isEmptyBody
import com.huawei.cangjie.resolve.DescriptorResolver.getDefaultModality
import com.huawei.cangjie.resolve.DescriptorResolver.getDefaultVisibility
import com.huawei.cangjie.resolve.DescriptorUtils.getDispatchReceiverParameterIfNeeded
import com.huawei.cangjie.resolve.ModifiersChecker.Companion.resolveMemberModalityFromModifiers
import com.huawei.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.huawei.cangjie.resolve.calls.DslMarkerUtils
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.util.createValueParametersForInvokeInFunctionType
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.resolve.scopes.TraceBasedLocalRedeclarationChecker
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.ExpressionTypingUtils.isFunctionExpression
import com.huawei.cangjie.types.expressions.ExpressionTypingUtils.isFunctionLiteral
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.replaceAnnotations
import java.util.*


class FunctionDescriptorResolver(
    private val typeResolver: TypeResolver,
    private val descriptorResolver: DescriptorResolver,
    private val annotationResolver: AnnotationResolver,
    private val builtIns: CangJieBuiltIns,
    private val modifiersChecker: ModifiersChecker,
    private val overloadChecker: OverloadChecker,
//    private val contractParsingServices: ContractParsingServices,
//    private val expressionTypingServices: ExpressionTypingServices,

    private val storageManager: StorageManager
) {
    private fun resolveValueParameters(
        functionDescriptor: FunctionDescriptor,
        parameterScope: LexicalWritableScope,
        valueParameters: List<CjParameter>,
        trace: BindingTrace,
        expectedParameterTypes: List<CangJieType>?,
        inferenceSession: InferenceSession?
    ): List<ValueParameterDescriptor> {
        val result = ArrayList<ValueParameterDescriptor>()
//
        for (i in valueParameters.indices) {
            val valueParameter = valueParameters[i]
            val typeReference = valueParameter.typeReference
            val expectedType = expectedParameterTypes?.let { if (i < it.size) it[i] else null }
                ?.takeUnless { TypeUtils.noExpectedType(it) }

            val type: CangJieType
            if (typeReference != null) {
                type = typeResolver.resolveType(parameterScope, typeReference, trace, true)
                if (expectedType != null) {
                    if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(expectedType, type)) {
                        trace.report(EXPECTED_PARAMETER_TYPE_MISMATCH.on(valueParameter, expectedType))
                    }
                }
            } else {
                type =
                    if (isFunctionLiteral(functionDescriptor) || isFunctionExpression(functionDescriptor)) {
                        val containsErrorType = TypeUtils.contains(expectedType) { it.isError }
                        if (expectedType == null || containsErrorType) {
                            trace.report(CANNOT_INFER_PARAMETER_TYPE.on(valueParameter))
                        }

                        expectedType ?: TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE
                    } else {
                        trace.report(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.on(valueParameter))
                        ErrorUtils.createErrorType(
                            ErrorTypeKind.MISSED_TYPE_FOR_PARAMETER,
                            valueParameter.nameAsSafeName.toString()
                        )
                    }
            }

//            if (functionDescriptor !is ConstructorDescriptor || !functionDescriptor.isPrimary) {
//                val isConstructor = functionDescriptor is ConstructorDescriptor
//                with(modifiersChecker.withTrace(trace)) {
//                    checkParameterHasNoValOrVar(
//                        valueParameter,
//                        if (isConstructor) VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER else VAL_OR_VAR_ON_FUN_PARAMETER
//                    )
//                }
//            }

            val valueParameterDescriptor = descriptorResolver.resolveValueParameterDescriptor(
                parameterScope, functionDescriptor, valueParameter, i, type, trace, Annotations.EMPTY, inferenceSession
            )

            // Do not report NAME_SHADOWING for lambda destructured parameters as they may be not fully resolved at this time
//            ExpressionTypingUtils.checkVariableShadowing(parameterScope, trace, valueParameterDescriptor)

            parameterScope.addVariableDescriptor(valueParameterDescriptor)
            result.add(valueParameterDescriptor)
        }
        return result

    }


    private fun CangJieType.removeParameterNameAnnotation(): CangJieType {
        if (this is TypeUtils.SpecialType) return this
        val parameterNameAnnotation = annotations.findAnnotation(StandardNames.FqNames.parameterName) ?: return this
        return replaceAnnotations(Annotations.create(annotations.filter { it != parameterNameAnnotation }))
    }

    private fun CangJieType.getValueParameters(owner: FunctionDescriptor): List<ValueParameterDescriptor>? =
        if (functionTypeExpected()) {
            createValueParametersForInvokeInFunctionType(owner, this.getValueParameterTypesFromFunctionType())
        } else null

    private fun createValueParameterDescriptors(
        function: CjFunction,
        functionDescriptor: SimpleFunctionDescriptorImpl,
        innerScope: LexicalWritableScope,
        trace: BindingTrace,
        expectedFunctionType: CangJieType,
        inferenceSession: InferenceSession?
    ): List<ValueParameterDescriptor> {

        val expectedValueParameters = expectedFunctionType.getValueParameters(functionDescriptor)
        val expectedParameterTypes = expectedValueParameters?.map { it.type.removeParameterNameAnnotation() }
//        if (expectedValueParameters != null) {
//            if (expectedValueParameters.size == 1 && function is CjFunctionLiteral && function.getValueParameterList() == null) {
//                // it parameter for lambda
//                val valueParameterDescriptor = expectedValueParameters.single()
//                val it = ValueParameterDescriptorImpl(
//                    functionDescriptor, null, 0, Annotations.EMPTY, StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME,
//                    expectedParameterTypes!!.single(), valueParameterDescriptor.declaresDefaultValue(),
//                    valueParameterDescriptor.isCrossinline, valueParameterDescriptor.isNoinline,
//                    valueParameterDescriptor.varargElementType, SourceElement.NO_SOURCE
//                )
//                trace.record(BindingContext.AUTO_CREATED_IT, it)
//                return listOf(it)
//            }
//            if (function.valueParameters.size != expectedValueParameters.size) {
//                trace.report(EXPECTED_PARAMETERS_NUMBER_MISMATCH.on(function, expectedParameterTypes!!.size, expectedParameterTypes))
//            }
//        }
//
        trace.recordScope(innerScope, function.valueParameterList)
//
        return resolveValueParameters(
            functionDescriptor,
            innerScope,
            function.valueParameters,
            trace,
            expectedParameterTypes,
            inferenceSession
        )
//TODO()
    }

    private fun CangJieType.getReceiverType(): CangJieType? =
        if (functionTypeExpected()) this.getReceiverTypeFromFunctionType() else null

    private fun CangJieType.getContextReceiversTypes(): List<ContextReceiverTypeWithLabel> =
        if (functionTypeExpected()) {
            this.getContextReceiverTypesFromFunctionType().map { ContextReceiverTypeWithLabel(it, label = null) }
        } else {
            emptyList()
        }


    /**
     * 方法返回值类型推断
     *    如果没有显示指定类型，从方法块的最后一条语句推断类型，如果没有语句 指定类型为Unit
     */
    fun resolveFunctionReturnType(
        function: CjFunction,
        trace: BindingTrace,
        headerScope: LexicalScope,
    ): CangJieType {
//        显示指定的类型
        return if (function.typeReference != null) {
            typeResolver.resolveType(headerScope, function.typeReference!!, trace, true)

        } else if (function.hasBody()) {
            val block = function.getBodyBlockExpression()
            if (block!!.isEmptyBody()) {
                return builtIns.unitType
            }

//        TODO 返回值类型推断 暂时返回Unit
//            return block.returnValueInferred()
            return builtIns.unitType
        } else {
            builtIns.unitType

        }

    }

    fun initializeFunctionDescriptorAndExplicitReturnType(
        container: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjFunction,
        functionDescriptor: SimpleFunctionDescriptorImpl,
        trace: BindingTrace,
        expectedFunctionType: CangJieType,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ) {
        val headerScope = LexicalWritableScope(
            scope, functionDescriptor, true,
            TraceBasedLocalRedeclarationChecker(trace, overloadChecker), LexicalScopeKind.FUNCTION_HEADER
        )
//
        val typeParameterDescriptors =
            descriptorResolver.resolveTypeParametersForDescriptor(
                functionDescriptor,
                headerScope,
                scope,
                function.typeParameters,
                trace
            )
//        descriptorResolver.resolveGenericBounds(
//            function,
//            functionDescriptor,
//            headerScope,
//            typeParameterDescriptors,
//            trace
//        )

        val receiverTypeRef = function.receiverTypeReference
        val receiverType =
            if (receiverTypeRef != null) {
                typeResolver.resolveType(headerScope, receiverTypeRef, trace, true)
            } else {
                if (function is CjFunctionLiteral) expectedFunctionType.getReceiverType() else null
            }
//
        val contextReceivers = function.contextReceivers
        val contextReceiverTypes =
            if (function is CjFunctionLiteral) expectedFunctionType.getContextReceiversTypes()
            else contextReceivers
                .mapNotNull {
                    val typeReference = it.typeReference() ?: return@mapNotNull null
                    val type = typeResolver.resolveType(headerScope, typeReference, trace, true)
                    ContextReceiverTypeWithLabel(type, it.labelNameAsName())
                }


        val valueParameterDescriptors =
            createValueParameterDescriptors(
                function,
                functionDescriptor,
                headerScope,
                trace,
                expectedFunctionType,
                inferenceSession
            )

        headerScope.freeze()


        val returnType = resolveFunctionReturnType(function, trace, headerScope)


        val visibility = resolveVisibilityFromModifiers(function, getDefaultVisibility(function, container))
        val modality = resolveMemberModalityFromModifiers(
            function, getDefaultModality(container, visibility, function.hasBody()),
            trace.bindingContext, container
        )

//        val contractProvider =
//            getContractProvider(functionDescriptor, trace, scope, dataFlowInfo, function, inferenceSession)
        val userData = mutableMapOf<CallableDescriptor.UserDataKey<*>, Any>().apply {
//            if (contractProvider != null) {
//                put(ContractProviderKey, contractProvider)
//            }

            if (receiverType != null && expectedFunctionType.functionTypeExpected() && !expectedFunctionType.annotations.isEmpty()) {
                put(DslMarkerUtils.FunctionTypeAnnotationsKey, expectedFunctionType.annotations)
            }
        }
//
        val extensionReceiver = receiverType?.let {
            val splitter =
                AnnotationSplitter(storageManager, it.annotations, EnumSet.of(AnnotationUseSiteTarget.RECEIVER))
            DescriptorFactory.createExtensionReceiverParameterForCallable(
                functionDescriptor, it, splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.RECEIVER)
            )
        }
        val contextReceiverDescriptors = contextReceiverTypes.mapIndexedNotNull { index, contextReceiver ->
            val splitter = AnnotationSplitter(
                storageManager,
                contextReceiver.type.annotations,
                EnumSet.of(AnnotationUseSiteTarget.RECEIVER)
            )
            DescriptorFactory.createContextReceiverParameterForCallable(
                functionDescriptor,
                contextReceiver.type,
                contextReceiver.label,
                splitter.getAnnotationsForTarget(AnnotationUseSiteTarget.RECEIVER),
                index
            )
        }


//
        functionDescriptor.initialize(
            extensionReceiver,
            getDispatchReceiverParameterIfNeeded(container),
            contextReceiverDescriptors,
            typeParameterDescriptors,
            valueParameterDescriptors,
            returnType,
            modality,
            visibility,
            userData.takeIf { it.isNotEmpty() }
        )
//
        functionDescriptor.setIsOperator(function.hasModifier(CjTokens.OPERATOR_KEYWORD))


//        functionDescriptor.isExpect = container is PackageFragmentDescriptor && function.hasExpectModifier() ||
//                container is ClassDescriptor && container.isExpect
//        functionDescriptor.isActual = function.hasActualModifier()
//
//        receiverType?.let { ForceResolveUtil.forceResolveAllContents(it.annotations) }
//        for (valueParameterDescriptor in valueParameterDescriptors) {
//            ForceResolveUtil.forceResolveAllContents(valueParameterDescriptor.type.annotations)
//        }

    }

    private fun CangJieType.functionTypeExpected() = !TypeUtils.noExpectedType(this) && isBuiltinFunctionalType

    private fun initializeFunctionReturnTypeBasedOnFunctionBody(
        scope: LexicalScope,
        function: CjNamedFunction,
        functionDescriptor: SimpleFunctionDescriptorImpl,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ) {
        if (functionDescriptor.returnType != null) return
        assert(function.typeReference == null) {
            "Return type must be initialized early for function: " + function.text + ", at: " + PsiDiagnosticUtils.atLocation(
                function
            )
        }
        val inferredReturnType = when {
            function.hasBlockBody() ->
                builtIns.unitType

            function.hasBody() ->
                descriptorResolver.inferReturnTypeFromExpressionBody(
                    trace, scope, dataFlowInfo, function, functionDescriptor, inferenceSession
                )

            else ->
                ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE, functionDescriptor.name.asString())
        }
        functionDescriptor.setReturnType(inferredReturnType)
    }

    private fun resolveFunctionDescriptor(

        functionConstructor: (DeclarationDescriptor, Annotations, Name, CallableMemberDescriptor.Kind, SourceElement) -> SimpleFunctionDescriptorImpl,
        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjNamedFunction,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        expectedFunctionType: CangJieType,
        inferenceSession: InferenceSession?
    ): SimpleFunctionDescriptor {
        val functionDescriptor = functionConstructor(
            containingDescriptor,
            annotationResolver.resolveAnnotationsWithoutArguments(scope, function.modifierList, trace),
            function.nameAsSafeName,
            CallableMemberDescriptor.Kind.DECLARATION,
            function.toSourceElement()
        )
        initializeFunctionDescriptorAndExplicitReturnType(
            containingDescriptor,
            scope,
            function,
            functionDescriptor,
            trace,
            expectedFunctionType,
            dataFlowInfo,
            inferenceSession
        )
        initializeFunctionReturnTypeBasedOnFunctionBody(
            scope,
            function,
            functionDescriptor,
            trace,
            dataFlowInfo,
            inferenceSession
        )
        BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, function, functionDescriptor)
        return functionDescriptor
    }

    private data class ContextReceiverTypeWithLabel(val type: CangJieType, val label: Name?)

    fun resolveFunctionDescriptor(

        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjNamedFunction,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ): SimpleFunctionDescriptor {
        if (function.name == null) trace.report(FUNCTION_DECLARATION_WITH_NO_NAME.on(function))

        return resolveFunctionDescriptor(
            SimpleFunctionDescriptorImpl::create, containingDescriptor, scope,
            function, trace, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, inferenceSession
        )
    }
}

