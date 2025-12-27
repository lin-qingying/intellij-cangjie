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

package org.cangnova.cangjie.types.expressions

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Ref
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.tasks.OldResolutionCandidate
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.resolve.scopes.receivers.Receiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import java.util.*

class ControlStructureTypingUtils(
    val callResolver: CallResolver,
    val dataFlowAnalyzer: DataFlowAnalyzer,
    val moduleDescriptor: ModuleDescriptor,
    val storageManager: StorageManager
) {
    companion object {


        val LOG: Logger = Logger.getInstance(
            ControlStructureTypingUtils::class.java
        )

        class ControlStructureDataFlowInfo(
            initialDataFlowInfo: DataFlowInfo,
            val dataFlowInfoForArgumentsMap: MutableMap<ValueArgument, DataFlowInfo>
        ) : MutableDataFlowInfoForArguments(initialDataFlowInfo) {

            override fun updateInfo(valueArgument: ValueArgument, dataFlowInfo: DataFlowInfo) {
                dataFlowInfoForArgumentsMap[valueArgument] = dataFlowInfo
            }

            override fun updateResultInfo(dataFlowInfo: DataFlowInfo) {}

            override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
                return dataFlowInfoForArgumentsMap[valueArgument] ?: error("DataFlowInfo not found for ValueArgument")
            }
        }

        private fun createIndependentDataFlowInfoForArgumentsForCall(
            initialDataFlowInfo: DataFlowInfo,
            dataFlowInfoForArgumentsMap: MutableMap<ValueArgument, DataFlowInfo>
        ): MutableDataFlowInfoForArguments {
            return ControlStructureDataFlowInfo(
                initialDataFlowInfo,
                dataFlowInfoForArgumentsMap
            )
        }

        fun createDataFlowInfoForArgumentsForIfCall(
            callForIf: Call,
            conditionInfo: DataFlowInfo,
            thenInfo: DataFlowInfo,
            elseInfo: DataFlowInfo
        ): MutableDataFlowInfoForArguments {
            val dataFlowInfoForArgumentsMap = hashMapOf<ValueArgument, DataFlowInfo>()
            dataFlowInfoForArgumentsMap[callForIf.valueArguments[0]] = thenInfo
            dataFlowInfoForArgumentsMap[callForIf.valueArguments[1]] = elseInfo
            return createIndependentDataFlowInfoForArgumentsForCall(conditionInfo, dataFlowInfoForArgumentsMap)
        }

        
        fun createDataFlowInfoForArgumentsOfMatchCall(
            callForWhen: Call,
            subjectDataFlowInfo: DataFlowInfo,
            entryDataFlowInfos: List<DataFlowInfo>
        ): MutableDataFlowInfoForArguments {
            val dataFlowInfoForArgumentsMap = mutableMapOf<ValueArgument, DataFlowInfo>()
            for ((i, argument) in callForWhen.valueArguments.withIndex()) {
                val entryDataFlowInfo = entryDataFlowInfos[i]
                dataFlowInfoForArgumentsMap[argument] = entryDataFlowInfo
            }
            return createIndependentDataFlowInfoForArgumentsForCall(subjectDataFlowInfo, dataFlowInfoForArgumentsMap)
        }


        
        fun createDataFlowInfoForArgumentsOfTryCall(
            callForTry: Call,
            dataFlowInfoBeforeTry: DataFlowInfo,
            dataFlowInfoAfterTry: DataFlowInfo
        ): MutableDataFlowInfoForArguments {
            val dataFlowInfoForArgumentsMap: MutableMap<ValueArgument, DataFlowInfo> =
                HashMap<ValueArgument, DataFlowInfo>()
            val valueArguments: List<ValueArgument> = callForTry.valueArguments
            dataFlowInfoForArgumentsMap[valueArguments[0]] = dataFlowInfoBeforeTry
            for (i in 1 until valueArguments.size) {
                dataFlowInfoForArgumentsMap[valueArguments[i]] = dataFlowInfoAfterTry
            }
            return createIndependentDataFlowInfoForArgumentsForCall(
                dataFlowInfoBeforeTry,
                dataFlowInfoForArgumentsMap
            )
        }

        
        fun createCallForSpecialConstruction(
            expression: CjExpression,
            calleeExpression: CjExpression,
            arguments: List<CjExpression>
        ): Call {
            val valueArguments: MutableList<ValueArgument> =
                Lists.newArrayList()
            for (argument in arguments) {
                valueArguments.add(CallMaker.makeValueArgument(argument))
            }
            return object : Call {
                override var noValueArgument: Boolean = false
                override var noTypeParameter: Boolean = false

                override val callOperationNode: ASTNode?
                    get() = expression.node
                override val explicitReceiver: Receiver?
                    get() = null
                override val dispatchReceiver: ReceiverValue?
                    get() = null
                override val calleeExpression: CjExpression
                    get() = calleeExpression
                override val valueArgumentList: CjValueArgumentList?
                    get() = null
                override val valueArguments: List<ValueArgument>
                    get() = valueArguments
                override val functionLiteralArguments: List<LambdaArgument>
                    get() = emptyList()
                override val typeArguments: List<CjTypeProjection>
                    get() = emptyList()
                override val typeArgumentList: CjTypeArgumentList?
                    get() = null
                override val callElement: CjElement
                    get() = expression
                override val callType: Call.CallType
                    get() = Call.CallType.DEFAULT
            }
        }

        private fun createKnownTypeParameterSubstitutorForSpecialCall(
            construct: ResolveConstruct,
            function: SimpleFunctionDescriptorImpl,
            expectedType: CangJieType,
            languageVersionSettings: LanguageVersionSettings
        ): TypeSubstitutor? {
            if (construct == ResolveConstruct.ELVIS || TypeUtils.noExpectedType(
                    expectedType
                )
                || TypeUtils.isDontCarePlaceholder(expectedType)
                || CangJieBuiltIns.isUnit(expectedType)
                || CangJieBuiltIns.isAny(expectedType)
            ) {
                return null
            }

            val typeParameterConstructor: TypeConstructor =
                function.typeParameters[0].typeConstructor
            val typeProjection: TypeProjection =
                TypeProjectionImpl(expectedType)
            return TypeSubstitutor.create(
                ImmutableMap.of(
                    typeParameterConstructor,
                    typeProjection
                )
            )
        }

        fun createFunctionDescriptorForSpecialConstruction(
            construct: ResolveConstruct,
            argumentNames: List<String>,
            isArgumentNullable: List<Boolean>,
            moduleDescriptor: ModuleDescriptor,
            storageManager: StorageManager
        ): SimpleFunctionDescriptorImpl {
            require(argumentNames.size == isArgumentNullable.size)

            val function = SimpleFunctionDescriptorImpl.create(
                moduleDescriptor, Annotations.EMPTY, construct.specialFunctionName,
                CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
            )

            val typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
                function, Annotations.EMPTY, Variance.INVARIANT,
                construct.specialTypeParameterName, 0, storageManager
            )

            val type = typeParameter.defaultType
            val nullableType = TypeUtils.makeOption(type)

            val valueParameters = ArrayList<ValueParameterDescriptor>(argumentNames.size)
            for (i in argumentNames.indices) {
                val argumentType = if (isArgumentNullable[i]) nullableType else type
                val valueParameter = ValueParameterDescriptorImpl(
                    function, null, i, Annotations.EMPTY, Name.identifier(argumentNames[i]), false,
                    argumentType,
                    /* declaresDefaultValue = */ false,

                    SourceElement.NO_SOURCE
                )
                valueParameters.add(valueParameter)
            }
            val returnType =
                if (construct != ResolveConstruct.ELVIS) type else type.replaceAnnotations(Annotations.EMPTY /*  AnnotationsForResolveUtils.getExactInAnnotations()*/)
            function.initialize(
                null,
                listOf(typeParameter),
                valueParameters,
                returnType,
                Modality.FINAL,
                DescriptorVisibilities.PUBLIC
            )
            return function
        }
    }


    /**
     * 将特定构造作为函数调用进行解析
     *
     * 此函数用于解析一些特殊的构造语法，将其视为函数调用来处理这些构造通常具有特定的语法结构，
     * 但为了类型检查和代码分析的目的，需要将其映射到函数调用上这样做可以让这些构造享受到
     * 函数调用处理机制的好处，包括类型推断、参数名称和可空性检查等
     *
     * @param call 调用表达式，代表了需要解析的特殊构造
     * @param construct 解析构造对象，描述了特
     * 殊构造的类型和属性
     * @param argumentNames 参数名称列表，用于构造函数描述符
     * @param isArgumentNullable 布尔值列表，指示每个参数是否可为null
     * @param context 表达式类型检查上下文，提供了类型检查所需的环境信息
     * @param dataFlowInfoForArguments 参数的数据流信息，可选参数，提供了参数间的依赖和影响信息
     * @return 解析后的函数调用描述符，代表了特殊构造作为函数调用的结果
     */
    /*package*/
    fun resolveSpecialConstructionAsCall(
        call: Call,
        construct: ResolveConstruct,
        argumentNames: List<String>,
        isArgumentNullable: List<Boolean>,
        context: ExpressionTypingContext,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
    ): ResolvedCall<FunctionDescriptor> {
        // 创建特殊构造的函数描述符，这是解析过程的第一步
        val function: SimpleFunctionDescriptorImpl =
            createFunctionDescriptorForSpecialConstruction(
                construct, argumentNames, isArgumentNullable
            )
        // 调用解析函数，将特殊构造作为函数调用来解析
        return resolveSpecialConstructionAsCall(call, function, construct, context, dataFlowInfoForArguments)
    }

    private fun resolveSpecialConstructionAsCall(
        call: Call,
        function: SimpleFunctionDescriptorImpl,
        construct: ResolveConstruct,
        context: ExpressionTypingContext,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
    ): ResolvedCall<FunctionDescriptor> {
        val tracing = createTracingForSpecialConstruction(call, construct.name, context)
        val knownTypeParameterSubstitutor = createKnownTypeParameterSubstitutorForSpecialCall(
            construct,
            function,
            context.expectedType,
            context.languageVersionSettings
        )
        val resolutionCandidate: OldResolutionCandidate<FunctionDescriptor> =
            OldResolutionCandidate.create(call, function, knownTypeParameterSubstitutor)
        val results = callResolver.resolveCallWithKnownCandidate(
            call,
            tracing,
            context,
            resolutionCandidate,
            dataFlowInfoForArguments
        )
        require(results.isSingleResult) { "Not single result after resolving one known candidate" }
        return results.resultingCall
    }

    internal fun resolveTryAsCall(
        call: Call,
        tryResourceExceptions: List<Pair<CjExpression, VariableDescriptor>>,

        catchedExceptions: List<Pair<CjExpression, VariableDescriptor>>,
        context: ExpressionTypingContext,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
    ): ResolvedCall<FunctionDescriptor> {
        val argumentNames = mutableListOf("tryBlock")
        val argumentsNullability = mutableListOf(false)

        val tryResourceExceptionsByGroup = tryResourceExceptions.groupBy { it.first }
        for (tryBlock in tryResourceExceptionsByGroup.keys) {
            context.trace.record(
                BindingContext.NEW_INFERENCE_TRY_EXCEPTION_PARAMETER,
                tryBlock,
                Ref.create(tryResourceExceptionsByGroup[tryBlock]?.map { it.second })
            )


        }


        var counter = 0
        for ((catchBlock, catchedExceptionDescriptor) in catchedExceptions) {
            argumentNames.add("catchBlock$counter")
            argumentsNullability.add(false)

            context.trace.record(
                BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER,
                catchBlock,
                Ref.create(catchedExceptionDescriptor)
            )

            counter++
        }

        val function =
            createFunctionDescriptorForSpecialConstruction(ResolveConstruct.TRY, argumentNames, argumentsNullability)

        return resolveSpecialConstructionAsCall(call, function, ResolveConstruct.TRY, context, dataFlowInfoForArguments)
    }

    private fun createFunctionDescriptorForSpecialConstruction(
        construct: ResolveConstruct,
        argumentNames: List<String>,
        isArgumentNullable: List<Boolean>
    ): SimpleFunctionDescriptorImpl {
        return createFunctionDescriptorForSpecialConstruction(
            construct, argumentNames, isArgumentNullable, moduleDescriptor,
            storageManager
        )
    }

    private fun createTracingForSpecialConstruction(
        call: Call,
        constructionName: String,
        context: ExpressionTypingContext
    ): TracingStrategy {
        class CheckTypeContext(
            val trace: BindingTrace,
            val expectedType: CangJieType
        ) {

            fun makeTypeNullable(): CheckTypeContext {
                if (TypeUtils.noExpectedType(expectedType)) return this
                return CheckTypeContext(trace, TypeUtils.makeOption(expectedType))
            }
        }

        val checkTypeVisitor: CjVisitor<Boolean, CheckTypeContext> =
            object : CjVisitor<Boolean, CheckTypeContext>() {
                fun checkExpressionType(
                    expression: CjExpression,
                    c: CheckTypeContext
                ): Boolean {
                    val typeInfo: CangJieTypeInfo =
                        BindingContextUtils.getRecordedTypeInfo(
                            expression,
                            c.trace.bindingContext
                        )
                            ?: return false

                    val hasError = Ref.create<Boolean>()
                    dataFlowAnalyzer.checkType(
                        typeInfo.type,
                        expression,
                        context
                            .replaceExpectedType(c.expectedType)
                            .replaceDataFlowInfo(typeInfo.dataFlowInfo)
                            .replaceBindingTrace(c.trace),
                        hasError,
                        true
                    )
                    return hasError.get()
                }

                fun checkExpressionTypeRecursively(
                    expression: CjExpression?,
                    c: CheckTypeContext
                ): Boolean {
                    if (expression == null) return false
                    return expression.accept(this, c) == true
                }

                fun checkSubExpressions(
                    firstSub: CjExpression?,
                    secondSub: CjExpression?,
                    expression: CjExpression,
                    firstContext: CheckTypeContext,
                    secondContext: CheckTypeContext,
                    context: CheckTypeContext
                ): Boolean {
                    var errorWasReported = checkExpressionTypeRecursively(firstSub, firstContext)
                    errorWasReported = errorWasReported or checkExpressionTypeRecursively(secondSub, secondContext)
                    return errorWasReported || checkExpressionType(expression, context)
                }

//                override fun visitMatchExpression(
//                    whenExpression: CjMatchExpression,
//                    c: CheckTypeContext
//                ): Boolean {
//                    var errorWasReported = false
//                    for (whenEntry in whenExpression.getEntries()) {
//                        val entryExpression: CjExpression = whenEntry.getExpression()
//                        if (entryExpression != null) {
//                            errorWasReported = errorWasReported or checkExpressionTypeRecursively(entryExpression, c)
//                        }
//                    }
//                    errorWasReported = errorWasReported or checkExpressionType(whenExpression, c)
//                    return errorWasReported
//                }

                override fun visitIfExpression(
                    ifExpression: CjIfExpression,
                    c: CheckTypeContext
                ): Boolean {
                    val thenBranch = ifExpression.then
                    val elseBranch = ifExpression.`else`
                    if (thenBranch == null || elseBranch == null) {
                        return checkExpressionType(ifExpression, c)
                    }
                    return checkSubExpressions(thenBranch, elseBranch, ifExpression, c, c, c)
                }

                override fun visitBlockExpression(
                    expression: CjBlockExpression,
                    c: CheckTypeContext
                ): Boolean {
                    if (expression.statements.isEmpty()) {
                        return checkExpressionType(expression, c)
                    }
                    val lastStatement =
                        CjPsiUtil.getLastStatementInABlock(expression)
                    if (lastStatement != null) {
                        return checkExpressionTypeRecursively(lastStatement, c)
                    }
                    return false
                }

                override fun visitPostfixExpression(
                    expression: CjPostfixExpression,
                    c: CheckTypeContext
                ): Boolean {

                    return super.visitPostfixExpression(expression, c) == true
                }

                override fun visitBinaryExpression(
                    expression: CjBinaryExpression,
                    c: CheckTypeContext
                ): Boolean {
                    if (expression.operationReference
                            .referencedNameElementType === CjTokens.COALESCING
                    ) {
                        return checkSubExpressions(
                            expression.left,
                            expression.right,
                            expression,
                            c.makeTypeNullable(),
                            c,
                            c
                        )
                    }
                    return super.visitBinaryExpression(expression, c) == true
                }

                override fun visitExpression(
                    expression: CjExpression,
                    c: CheckTypeContext
                ): Boolean {
                    return checkExpressionType(expression, c)
                }
            }



        return object :
            ThrowingOnErrorTracingStrategy("resolveName $constructionName as a call") {
            override fun <D : CallableDescriptor> bindReference(
                trace: BindingTrace,
                resolvedCall: ResolvedCall<D>
            ) {
                //do nothing
            }

            override fun bindCall(
                trace: BindingTrace,
                call: Call
            ) {
                trace.record(
                    BindingContext.CALL,
                    call.calleeExpression ?: return,
                    call
                )
            }

            override fun <D : CallableDescriptor> bindResolvedCall(
                trace: BindingTrace,
                resolvedCall: ResolvedCall<D>
            ) {
                trace.record (
                    BindingContext.RESOLVED_CALL,
                    call,
                    resolvedCall
                )
            }

//            override fun typeInferenceFailed(
//                context: ResolutionContext<*>,
//                data: InferenceErrorData
//            ) {
//                val constraintSystem: ConstraintSystem =
//                    data.constraintSystem
//                val status: ConstraintSystemStatus =
//                    constraintSystem.status
//                assert(!status.isSuccessful()) { "Report error only for not successful constraint system" }
//
//                if (status.hasErrorInConstrainingTypes() || status.hasUnknownParameters()) {
//                    return
//                }
//                val expression: CjExpression =
//                    call.getCallElement() as CjExpression
//                if (status.hasOnlyErrorsDerivedFrom(ConstraintPositionKind.EXPECTED_TYPE_POSITION) || status.hasConflictingConstraints()
//                    || status.hasTypeInferenceIncorporationError()
//                ) {
//                    if (noTypeCheckingErrorsInExpression(expression, context.trace, data.expectedType)) {
//                        val calleeExpression = call.getCalleeExpression()
//                        if (calleeExpression is CjMatchExpression || calleeExpression is CjIfExpression) {
//                            if (status.hasConflictingConstraints() || status.hasTypeInferenceIncorporationError()) {
//                                // TODO provide comprehensible error report for hasConflictingConstraints() case (if possible)
//                                context.trace.report(
//                                    Errors.TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT.on(
//                                        expression
//                                    )
//                                )
//                            }
//                        }
//                    }
//                    return
//                }
//                val parentDeclaration: CjDeclaration = PsiTreeUtil.getParentOfType(
//                    expression,
//                    CjNamedDeclaration::class.java
//                )
//                logError(
//                    """
//                    Expression: ${if (parentDeclaration != null) parentDeclaration.text else expression.text}
//                    Constraint system status:
//                    ${ConstraintsUtil.getDebugMessageForStatus(status)}
//                    """.trimIndent()
//                )
//            }

            fun noTypeCheckingErrorsInExpression(
                expression: CjExpression,
                trace: BindingTrace,
                expectedType: CangJieType
            ): Boolean {
                return java.lang.Boolean.TRUE !== expression.accept(
                    checkTypeVisitor,
                    CheckTypeContext(trace, expectedType)
                )
            }
        }
    }

    enum class ResolveConstruct(name: String) {
        IF("if"), ELVIS("elvis"), COALESCING("coalescing"), EXCL_EXCL("ExclExcl"), MATCH("match"), TRY("try");

        val specialFunctionName: Name = Name.identifier(
            "<SPECIAL-FUNCTION-FOR-" + name.uppercase(
                Locale.getDefault()
            ) + "-RESOLVE>"
        )
        val specialTypeParameterName: Name =
            Name.identifier(
                "<TYPE-PARAMETER-FOR-" + name.uppercase(
                    Locale.getDefault()
                ) + "-RESOLVE>"
            )


    }

    private abstract
    class ThrowingOnErrorTracingStrategy protected constructor(private val debugName: String) :
        TracingStrategy {
        fun logError() {
            logError(null)
        }

        protected fun logError(additionalInformation: String?) {
            var errorMessage = "Resolution error of this type shouldn't occur for $debugName"
            if (additionalInformation != null) {
                errorMessage += ".\n$additionalInformation"
            }
            LOG.error(errorMessage)
        }

        override fun unresolvedReference(trace: BindingTrace) {
            logError()
        }

//        override fun recursiveType(
//            trace:  BindingTrace,
//            languageVersionSettings: LanguageVersionSettings,
//            insideAugmentedAssignment: Boolean
//        ) {
//            logError()
//        }

        override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
            trace: BindingTrace,
            candidates: Collection<ResolvedCall<D>>
        ) {
            logError()
        }

        override fun <D : CallableDescriptor> recordAmbiguity(
            trace: BindingTrace,
            candidates: Collection<ResolvedCall<D>>
        ) {
            logError()
        }

//        override fun missingReceiver(
//            trace:  BindingTrace,
//            expectedReceiver:  ReceiverParameterDescriptor
//        ) {
//            logError()
//        }

//        override fun wrongReceiverType(
//            trace:  BindingTrace,
//            receiverParameter:  ReceiverParameterDescriptor,
//            receiverArgument: ReceiverValue,
//            c: ResolutionContext<*>
//        ) {
//            logError()
//        }

//        override fun noReceiverAllowed(trace:  BindingTrace) {
//            logError()
//        }

        override fun noValueForParameter(
            trace: BindingTrace,
            valueParameter: ValueParameterDescriptor
        ) {
            logError()
        }

//        override fun wrongNumberOfTypeArguments(
//            trace:  BindingTrace,
//            expectedTypeArgumentCount: Int,
//            descriptor:  CallableDescriptor
//        ) {
//            logError()
//        }

        override fun <D : CallableDescriptor> ambiguity(
            trace: BindingTrace,
            resolvedCalls: Collection<ResolvedCall<D>>
        ) {
            logError()
        }

        override fun <D : CallableDescriptor> noneApplicable(
            trace: BindingTrace,
            descriptors: Collection<ResolvedCall<D>>
        ) {
            logError()
        }

        override fun <D : CallableDescriptor> cannotCompleteResolve(
            trace: BindingTrace,
            descriptors: Collection<ResolvedCall<D>>
        ) {
            logError()
        }
//
//        override fun instantiationOfAbstractClass(trace:  BindingTrace) {
//            logError()
//        }
//
//        override fun abstractSuperCall(trace:  BindingTrace) {
//            logError()
//        }
//
//        override fun nestedClassAccessViaInstanceReference(
//            trace:  BindingTrace,
//            classDescriptor:  ClassDescriptor,
//            explicitReceiverKind: ExplicitReceiverKind
//        ) {
//            logError()
//        }

        override fun unsafeCall(
            trace: BindingTrace,
            type: CangJieType,
            isCallForImplicitInvoke: Boolean
        ) {
            logError()
        }

        override fun invisibleMember(
            trace: BindingTrace,
            descriptor: DeclarationDescriptorWithVisibility
        ) {
            logError()
        }
//
//        override fun typeInferenceFailed(
//            context:   ResolutionContext<*>,
//            inferenceErrorData:  InferenceErrorData
//        ) {
//            logError()
//        }
    }
}
