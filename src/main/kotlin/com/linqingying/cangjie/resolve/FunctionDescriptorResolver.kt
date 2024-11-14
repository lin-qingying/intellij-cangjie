package com.linqingying.cangjie.resolve

import com.intellij.psi.PsiElement
import com.linqingying.cangjie.builtins.*
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.AnnotationSplitter
import com.linqingying.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.FunctionExpressionDescriptor
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorForExtendImpl
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.descriptors.macro.MacroDescriptorImpl
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.DescriptorResolver.getDefaultModality
import com.linqingying.cangjie.resolve.DescriptorResolver.getDefaultVisibility
import com.linqingying.cangjie.resolve.DescriptorUtils.getDispatchReceiverParameterIfNeeded
import com.linqingying.cangjie.resolve.ModifiersChecker.Companion.resolveMemberModalityFromModifiers
import com.linqingying.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import com.linqingying.cangjie.resolve.calls.DslMarkerUtils
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.util.createValueParametersForInvokeInFunctionType
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.resolve.source.toSourceElement
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.types.expressions.ExpressionTypingUtils.isFunctionExpression
import com.linqingying.cangjie.types.expressions.ExpressionTypingUtils.isFunctionLiteral
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.types.util.replaceAnnotations
import java.util.*


class FunctionDescriptorResolver(
    private val typeResolver: TypeResolver,
    private val descriptorResolver: DescriptorResolver,
    private val annotationResolver: AnnotationResolver,
    private val builtIns: CangJieBuiltIns,
    private val modifiersChecker: ModifiersChecker,
    private val overloadChecker: OverloadChecker,
    private val functionReturnResolver: FunctionReturnResolver,
//    private val contractParsingServices: ContractParsingServices,
    private val expressionTypingServices: ExpressionTypingServices,

    private val storageManager: StorageManager
) {

    fun resolveFunctionExpressionDescriptor(
        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjNamedFunction,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        expectedFunctionType: CangJieType,
        inferenceSession: InferenceSession?
    ): SimpleFunctionDescriptor = resolveFunctionDescriptor(
        ::FunctionExpressionDescriptor,
        containingDescriptor,
        scope,
        function,
        trace,
        dataFlowInfo,
        expectedFunctionType,
        inferenceSession
    )

    fun resolvePrimaryConstructorDescriptor(
        scope: LexicalScope,
        classDescriptor: ClassDescriptor,
        classElement: CjPureTypeStatement,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        inferenceSession: InferenceSession?
    ): ClassConstructorDescriptorImpl? {

        if (/*classDescriptor.kind == ClassKind.ENUM_ENTRY ||*/ !classElement.hasPrimaryConstructor()) return null
        return createConstructorDescriptor(
            scope,
            classDescriptor,
            true,
            classElement.primaryConstructorModifierList,
            classElement.primaryConstructor ?: classElement,
            classElement.primaryConstructorParameters,
            trace,
            languageVersionSettings,
            inferenceSession
        )
    }

    private fun resolveValueParameters(
        functionDescriptor: FunctionDescriptor,
        parameterScope: LexicalWritableScope,
        valueParameters: List<CjParameter>,
        trace: BindingTrace,
        expectedParameterTypes: List<CangJieType>?,
        inferenceSession: InferenceSession?
    ): List<ValueParameterDescriptor> {
        val result = ArrayList<ValueParameterDescriptor>()

        var isNamed = false

        for (i in valueParameters.indices) {
            val valueParameter = valueParameters[i]


            if (valueParameter.isNamed) {
                isNamed = true
            } else if (isNamed) {
                trace.report(NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER.on(valueParameter))
            }

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

    fun resolveConstructorDescriptor(
        scope: LexicalScope,
        classDescriptor: ClassDescriptor,
        constructor: CjConstructor<*>,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        inferenceSession: InferenceSession?,
        isEnd: Boolean = false
    ): ClassConstructorDescriptorImpl {
        return createConstructorDescriptor(
            scope,
            classDescriptor,
            constructor is CjPrimaryConstructor,
            constructor.modifierList,
            constructor,
            constructor.valueParameters,
            trace,
            languageVersionSettings,
            inferenceSession,
            isEnd
        )
    }


    private fun createConstructorDescriptor(
        scope: LexicalScope,
        classDescriptor: ClassDescriptor,
        isPrimary: Boolean,
        modifierList: CjModifierList?,
        declarationToTrace: CjPureElement,
        valueParameters: List<CjParameter>,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        inferenceSession: InferenceSession?,
        isEnd: Boolean = false
    ): ClassConstructorDescriptorImpl {
        val constructorDescriptor = ClassConstructorDescriptorImpl.create(
            classDescriptor,
            annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace),
            isPrimary,
            declarationToTrace.toSourceElement(),
            isEnd
        )
        constructorDescriptor.isExpect = classDescriptor.isExpect
//        constructorDescriptor.isActual = modifierList?.hasActualModifier() == true ||
//                // We don't require 'actual' for constructors of actual annotations
//                classDescriptor.kind == ClassKind.ANNOTATION_CLASS && classDescriptor.isActual
        val parameterScope = LexicalWritableScope(
            scope,
            constructorDescriptor,
            false,
            TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
            LexicalScopeKind.CONSTRUCTOR_HEADER
        )
        val constructor = constructorDescriptor.initialize(
            resolveValueParameters(
                constructorDescriptor, parameterScope, valueParameters, trace, null, inferenceSession
            ),
            resolveVisibilityFromModifiers(
                modifierList,
                DescriptorUtils.getDefaultConstructorVisibility(
                    classDescriptor, languageVersionSettings.supportsFeature(
                        LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage
                    )
                )
            )
        )
        constructor.returnType = classDescriptor.defaultType
//        if (DescriptorUtils.isAnnotationClass(classDescriptor)) {
//            CompileTimeConstantUtils.checkConstructorParametersType(valueParameters, trace)
//        }
        if (declarationToTrace is PsiElement)
            trace.record(
                if (isEnd) {
                    BindingContext.END_CONSTRUCTOR
                } else {
                    BindingContext.CONSTRUCTOR
                }, declarationToTrace, constructorDescriptor
            )
        return constructor
    }

    private fun createValueParameterDescriptors(
        function: CjFunction,
        functionDescriptor: FunctionDescriptor,
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

    }

    private fun CangJieType.getReceiverType(): CangJieType? =
        if (functionTypeExpected()) this.getReceiverTypeFromFunctionType() else null

    private fun CangJieType.getContextReceiversTypes(): List<ContextReceiverTypeWithLabel> =
        if (functionTypeExpected()) {
            this.getContextReceiverTypesFromFunctionType().map { ContextReceiverTypeWithLabel(it, label = null) }
        } else {
            emptyList()
        }

    fun resolveMacroReturnType(
        function: CjMacroDeclaration,
        context: ExpressionTypingContext,
    ): CangJieType {
//        显示指定的类型
        return if (function.typeReference != null) {
            typeResolver.resolveType(context.scope, function.typeReference!!, context.trace, true)

        } else {
            builtIns.tokensType

        }

    }

    /**
     * 方法返回值类型推断
     *    如果没有显示指定类型，从方法块的最后一条语句推断类型，如果没有语句 指定类型为Unit
     */
    fun resolveFunctionReturnType(
        function: CjFunction,
        context: ExpressionTypingContext,
    ): CangJieType {
//        显示指定的类型
        return if (function.typeReference != null) {
            typeResolver.resolveType(context.scope, function.typeReference!!, context.trace, true)

        } else if (function.hasBody()) {
//            val block = function.bodyBlockExpression
//            if (block!!.isEmptyBody()) {
//                return builtIns.unitType
//            }

//        TODO 返回值类型推断 暂时返回Unit
//            return block.returnValueInferred()
//            return builtIns.unitType

            function.keyword?.let {
                context.trace.report(
                    RETURN_TYPE_NOT_SPECIFIED_ERROR.on(it)
                )
            }
            return builtIns.unitType

//            return functionReturnResolver.resolveFunctionReturn(function, context)
//            return NO_EXPECTED_TYPE
        } else {
            builtIns.unitType

        }

    }

    fun initializeMacroDescriptorAndExplicitReturnType(
        container: DeclarationDescriptor,
        scope: LexicalScope,
        macro: CjMacroDeclaration,
        functionDescriptor: MacroDescriptor,
        trace: BindingTrace,
        expectedFunctionType: CangJieType,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ) {
        val headerScope = LexicalWritableScope(
            scope, functionDescriptor, true,
            TraceBasedLocalRedeclarationChecker(trace, overloadChecker), LexicalScopeKind.FUNCTION_HEADER
        )
//         宏声明没有类型参数
//        val typeParameterDescriptors =
//            descriptorResolver.resolveTypeParametersForDescriptor(
//                functionDescriptor,
//                headerScope,
//                scope,
//                function.typeParameters,
//                trace
//            )
//        descriptorResolver.resolveGenericBounds(
//            function,
//            functionDescriptor,
//            headerScope,
//            typeParameterDescriptors,
//            trace
//        )
        val contextReceivers = macro.contextReceivers
        val contextReceiverTypes = contextReceivers
            .mapNotNull {
                val typeReference = it.typeReference() ?: return@mapNotNull null
                val type = typeResolver.resolveType(headerScope, typeReference, trace, true)
                ContextReceiverTypeWithLabel(type, it.labelNameAsName())
            }


        val valueParameterDescriptors =
            createValueParameterDescriptors(
                macro,
                functionDescriptor,
                headerScope,
                trace,
                expectedFunctionType,
                inferenceSession
            )

        headerScope.freeze()

        val innerScope: LexicalScope =
            FunctionDescriptorUtil.getFunctionInnerScope(
                headerScope,
                functionDescriptor,
                trace,
                overloadChecker
            )
        val context = expressionTypingServices.createContext(
            headerScope,
            dataFlowInfo, NO_EXPECTED_TYPE,
            trace
        )
        val returnType = resolveMacroReturnType(macro, context)

        val visibility = resolveVisibilityFromModifiers(macro, getDefaultVisibility(macro, container))
        val modality = resolveMemberModalityFromModifiers(
            macro, getDefaultModality(container, visibility, macro.hasBody()),
            trace.bindingContext, container
        )

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


        functionDescriptor.initialize(

            getDispatchReceiverParameterIfNeeded(container),
            contextReceiverDescriptors,

            valueParameterDescriptors,
            returnType,
            modality,
            visibility,

            )


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
        val isExtend = function is CjNamedFunctionForExtend
//        val extendScope = LexicalWritableScope(
//            scope, functionDescriptor, true,
//            TraceBasedLocalRedeclarationChecker(trace, overloadChecker), LexicalScopeKind.EXTEND_HEADER
//        )
        val headerScope = LexicalWritableScope(
            /*if (isExtend) extendScope else*/ scope, functionDescriptor, true,
            TraceBasedLocalRedeclarationChecker(trace, overloadChecker), LexicalScopeKind.FUNCTION_HEADER
        )
//这是扩展方法
        val receiverTypeRef = function.receiverTypeReference
        val receiverType =
            if (receiverTypeRef != null) {
                if (container is LazyExtendClassDescriptor) {
                    container.type
                } else {
                    typeResolver.resolveType(scope, receiverTypeRef, trace, true)

                }
            } else {
                if (function is CjFunctionLiteral) expectedFunctionType.getReceiverType() else null
            }
//        来自扩展
        if (function is CjNamedFunctionForExtend) {
            functionDescriptor as SimpleFunctionDescriptorForExtendImpl
//            val typeParametersForExtend =
//                descriptorResolver.resolveTypeParametersForDescriptor(
//                    functionDescriptor,
//                    extendScope,
//                    scope,
//                    function.extendTypeParameters,
//                    trace
//                )
//
//            descriptorResolver.resolveGenericBoundsBorExtend(
//                function,
//                functionDescriptor,
//                scope,
//                typeParametersForExtend,
//                trace
//            )

//            functionDescriptor.typeParametersForExtend = typeParametersForExtend
            if (container is LazyExtendClassDescriptor) {
                functionDescriptor.typeParametersForExtend = container.declaredTypeParameters

            }
        }

        val typeParameterDescriptors =
            descriptorResolver.resolveTypeParametersForDescriptor(
                functionDescriptor,
                headerScope,
                scope,
                function.typeParameters,
                trace
            )



        descriptorResolver.resolveGenericBounds(
            function,
            functionDescriptor,
            headerScope,
            typeParameterDescriptors,
            trace
        )


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


        val innerScope: LexicalScope =
            FunctionDescriptorUtil.getFunctionInnerScope(
                headerScope,
                functionDescriptor,
                trace,
                overloadChecker
            )
        val codeBlockscope =
            LexicalWritableScope(
                innerScope, functionDescriptor, false, LocalRedeclarationChecker.DO_NOTHING,
                LexicalScopeKind.CODE_BLOCK
            )

        val context = expressionTypingServices.createContext(
            headerScope,
            dataFlowInfo, NO_EXPECTED_TYPE,
            trace
        )
//val returnType = expressionTypingServices.resolveFunctionReturnType(headerScope,function,functionDescriptor,dataFlowInfo,null,trace,null).type
        val returnType = resolveFunctionReturnType(function, context)


        val visibility = resolveVisibilityFromModifiers(function, getDefaultVisibility(function, container))
        val modality = resolveMemberModalityFromModifiers(
            function, getDefaultModality(container, visibility, function.hasBody()),
            trace.bindingContext, container
        )

        val userData = mutableMapOf<CallableDescriptor.UserDataKey<*>, Any>().apply {


            if (receiverType != null && expectedFunctionType.functionTypeExpected() && !expectedFunctionType.annotations.isEmpty()) {
                put(DslMarkerUtils.FunctionTypeAnnotationsKey, expectedFunctionType.annotations)
            }
        }

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

        functionDescriptor.setIsOperator(function.hasModifier(CjTokens.OPERATOR_KEYWORD))


    }

    private fun CangJieType.functionTypeExpected() = !TypeUtils.noExpectedType(this) && isBuiltinFunctionalType

    private fun initializeFunctionReturnTypeBasedOnFunctionBody(
        scope: LexicalScope,
        function: CjFunction,
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

    private fun resolveMacroDescriptor(

        macroConstructor: (DeclarationDescriptor, Name, SourceElement) -> MacroDescriptor,
        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        macro: CjMacroDeclaration,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        expectedFunctionType: CangJieType,
        inferenceSession: InferenceSession?
    ): MacroDescriptor {
        val macroDescriptor = macroConstructor(
            containingDescriptor,

            macro.nameAsSafeName,

            macro.toSourceElement()
        )

        initializeMacroDescriptorAndExplicitReturnType(
            containingDescriptor,
            scope,
            macro,
            macroDescriptor,
            trace,
            expectedFunctionType,
            dataFlowInfo,
            inferenceSession
        )

        BindingContextUtils.recordMacroDeclarationToDescriptor(trace, macro, macroDescriptor)


        return macroDescriptor
    }

    private fun resolveFunctionDescriptor(

        functionConstructor: (DeclarationDescriptor, Annotations, Name, CallableMemberDescriptor.Kind, SourceElement) -> SimpleFunctionDescriptorImpl,
        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjFunction,
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

    fun resolveMacroDescriptor(

        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjMacroDeclaration,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ): MacroDescriptor {
        if (function.name == null) trace.report(FUNCTION_DECLARATION_WITH_NO_NAME.on(function))

        return resolveMacroDescriptor(
            MacroDescriptorImpl::create, containingDescriptor, scope,
            function, trace, dataFlowInfo, NO_EXPECTED_TYPE, inferenceSession
        )
    }

    fun resolveFunctionDescriptor(

        containingDescriptor: DeclarationDescriptor,
        scope: LexicalScope,
        function: CjFunction,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ): SimpleFunctionDescriptor {
        if (function.name == null) trace.report(FUNCTION_DECLARATION_WITH_NO_NAME.on(function))

        return resolveFunctionDescriptor(
            if (function is CjNamedFunctionForExtend) SimpleFunctionDescriptorForExtendImpl::create else SimpleFunctionDescriptorImpl::create,
            containingDescriptor,
            scope,
            function,
            trace,
            dataFlowInfo,
            NO_EXPECTED_TYPE,
            inferenceSession
        )
    }
}

