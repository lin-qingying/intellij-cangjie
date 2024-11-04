package com.linqingying.cangjie.resolve.calls

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjLambdaExpression
import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.psi.psiUtil.checkReservedYield
import com.linqingying.cangjie.resolve.BindingContext.REFERENCE_TARGET
import com.linqingying.cangjie.resolve.OverrideResolver
import com.linqingying.cangjie.resolve.calls.components.hasDefaultValue
import com.linqingying.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import com.linqingying.cangjie.resolve.calls.model.DefaultValueArgument
import com.linqingying.cangjie.resolve.calls.model.ExpressionValueArgument
import com.linqingying.cangjie.resolve.calls.model.MutableResolvedCall
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy
import com.linqingying.cangjie.resolve.calls.util.getValueArgumentsInParentheses
import com.linqingying.cangjie.resolve.calls.util.isTrailingLambdaOnNewLIne
import com.linqingying.cangjie.resolve.calls.util.reportTrailingLambdaErrorOr


object ValueArgumentsToParametersMapper {
    fun <D : CallableDescriptor?> mapValueArgumentsToParameters(
        call: Call,
        tracing: TracingStrategy,
        candidateCall: MutableResolvedCall<D>,
        languageVersionSettings: LanguageVersionSettings
    ): Status {
        //return new ValueArgumentsToParametersMapper().process(call, tracing, candidateCall, unmappedArguments);
        val processor = Processor(call, candidateCall, tracing, languageVersionSettings)
        processor.process()
        return processor.status
    }

    enum class Status(val isSuccess: Boolean) {
        ERROR(false),
        WEAK_ERROR(false),
        OK(true);

        fun compose(other: Status): Status {
            if (this == ERROR || other == ERROR) return ERROR
            if (this == WEAK_ERROR || other == WEAK_ERROR) return WEAK_ERROR
            return this
        }
    }

    private class Processor<D : CallableDescriptor?>(
        private val call: Call,
        private val candidateCall: MutableResolvedCall<D>,
        private val tracing: TracingStrategy,
        private val languageVersionSettings: LanguageVersionSettings
    ) {
        private val parameters: List<ValueParameterDescriptor> =
            candidateCall.candidateDescriptor.valueParameters

        private val parameterByName: MutableMap<Name, ValueParameterDescriptor> =
            HashMap()
        private var parameterByNameInOverriddenMethods: MutableMap<Name, ValueParameterDescriptor>? = null

        //        private final Map<ValueParameterDescriptor, VarargValueArgument> varargs = new HashMap<>();
        private val usedParameters: MutableSet<ValueParameterDescriptor> = HashSet()
        var status: Status = Status.OK

        private fun getParameterByNameInOverriddenMethods(name: Name): ValueParameterDescriptor? {
            if (parameterByNameInOverriddenMethods == null) {
                parameterByNameInOverriddenMethods = mutableMapOf()
                for (valueParameter in parameters) {
                    for (parameterDescriptor in valueParameter.overriddenDescriptors) {
                        parameterByNameInOverriddenMethods!![parameterDescriptor.name] = valueParameter
                    }
                }
            }

            return parameterByNameInOverriddenMethods!![name]
        }

        // We saw only positioned arguments so far
        private val positionedOnly: ProcessorState = object : ProcessorState {
            private val currentParameter = 0

            private fun numberOfParametersForPositionedArguments(): Int {
                return if (call.callType == Call.CallType.ARRAY_SET_METHOD) parameters.size - 1 else parameters.size
            }

            fun nextValueParameter(): ValueParameterDescriptor? {
                if (currentParameter >= numberOfParametersForPositionedArguments()) return null

                val head = parameters[currentParameter]

                // If we found a vararg parameter, we are stuck with it forever
//                if (head.getVarargElementType() == null) {
//                    currentParameter++;
//                }
                return head
            }

            override fun processNamedArgument(argument: ValueArgument): ProcessorState {
                return positionedThenNamed.processNamedArgument(argument)
            }

            override fun processPositionedArgument(argument: ValueArgument): ProcessorState {
                processArgument(argument, nextValueParameter())
                return this
            }

            override fun processArraySetRHS(argument: ValueArgument): ProcessorState {
                processArgument(argument, parameters.lastOrNull())
                return this
            }

            private fun processArgument(argument: ValueArgument, parameter: ValueParameterDescriptor?) {
                if (parameter != null) {
                    usedParameters.add(parameter)
                    putVararg(parameter, argument)
                } else {
                    report(TOO_MANY_ARGUMENTS.on(argument.asElement(), candidateCall.candidateDescriptor))

                    status = Status.WEAK_ERROR
                }
            }
        }

        // We saw zero or more positioned arguments and then a named one
        private val positionedThenNamed: ProcessorState = object : ProcessorState {
            override fun processNamedArgument(argument: ValueArgument): ProcessorState {
                assert(argument.isNamed())

                val candidate: D = candidateCall.candidateDescriptor

                val argumentName = checkNotNull(argument.getArgumentName())
                var valueParameterDescriptor = parameterByName[argumentName.asName]
                val nameReference = argumentName.referenceExpression

                if (!languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
                    checkReservedYield(nameReference, candidateCall.trace)
                }

                if (candidate!!.hasStableParameterNames() && nameReference != null &&
                    candidate is CallableMemberDescriptor && (candidate as CallableMemberDescriptor).getKind() === CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                ) {
                    if (valueParameterDescriptor == null) {
                        valueParameterDescriptor = getParameterByNameInOverriddenMethods(argumentName.asName)
                    }

                    if (valueParameterDescriptor != null) {
                        for (parameterFromSuperclass in valueParameterDescriptor.overriddenDescriptors) {
                            if (OverrideResolver.shouldReportParameterNameOverrideWarning(
                                    valueParameterDescriptor,
                                    parameterFromSuperclass
                                )
                            ) {
                                report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))
                            }
                        }
                    }
                }

                if (valueParameterDescriptor == null) {
                    if (nameReference != null) {
                        report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))
                    }
                    status = Status.WEAK_ERROR
                } else {
                    if (nameReference != null) {
                        candidateCall.trace.record(
                            REFERENCE_TARGET,
                            nameReference,
                            valueParameterDescriptor
                        )
                    }
                    if (!usedParameters.add(valueParameterDescriptor)) {
                        if (nameReference != null) {
                            report(ARGUMENT_PASSED_TWICE.on(nameReference))
                        }
                        status = Status.WEAK_ERROR
                    } else {
                        putVararg(valueParameterDescriptor, argument)
                    }
                }

                return this
            }

            override fun processPositionedArgument(argument: ValueArgument): ProcessorState {
                report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(argument.asElement()))
                status = Status.WEAK_ERROR

                return this
            }

            override fun processArraySetRHS(argument: ValueArgument): ProcessorState {
                throw IllegalStateException("Array set RHS cannot appear after a named argument syntactically: $argument")
            }
        }

        init {
            for (valueParameter in parameters) {
                parameterByName[valueParameter.name] = valueParameter
            }
        }

        fun process() {
            var state = positionedOnly
            val isArraySetMethod = call.callType == Call.CallType.ARRAY_SET_METHOD
            val argumentsInParentheses: List<ValueArgument> = call.getValueArgumentsInParentheses(

            )
            val iterator = argumentsInParentheses.iterator()
            while (iterator.hasNext()) {
                val valueArgument = iterator.next()
                state = if (valueArgument.isNamed()) {
                    state.processNamedArgument(valueArgument)
                } else if (isArraySetMethod && !iterator.hasNext()) {
                    state.processArraySetRHS(valueArgument)
                } else {
                    state.processPositionedArgument(valueArgument)
                }
            }

//            for ((key, value) in varargs.entrySet()) {
//                candidateCall.recordValueArgument(key!!, value)
//            }

            processFunctionLiteralArguments()
            reportUnmappedParameters()
        }

        private fun processFunctionLiteralArguments() {
            val functionLiteralArguments = call.functionLiteralArguments
            if (functionLiteralArguments.isEmpty()) return

            val lambdaArgument = functionLiteralArguments[0]
            val possiblyLabeledFunctionLiteral = lambdaArgument.getArgumentExpression()

            if (parameters.isEmpty()) {
                candidateCall.trace.reportTrailingLambdaErrorOr(
                    possiblyLabeledFunctionLiteral
                ) { expression ->
                    TOO_MANY_ARGUMENTS.on(
                        expression,
                        candidateCall.candidateDescriptor
                    )
                }
                status = Status.ERROR

            } else {
                val lastParameter: ValueParameterDescriptor = parameters.last()
                if (!usedParameters.add(lastParameter)) {
                    candidateCall.trace.reportTrailingLambdaErrorOr(
                        possiblyLabeledFunctionLiteral
                    ) { expr ->
                        TOO_MANY_ARGUMENTS.on(
                            expr,
                            candidateCall.candidateDescriptor
                        )
                    }
                    status = Status.WEAK_ERROR
                } else {
                    putVararg(lastParameter, lambdaArgument)
                }
            }

            for (i in 1 until functionLiteralArguments.size) {
                val argument: CjExpression? =
                    functionLiteralArguments[i].getArgumentExpression()
                if (argument is CjLambdaExpression) {
                    report(MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(argument))
                    if (argument.isTrailingLambdaOnNewLIne) {
                        report(
                            UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(
                                argument
                            )
                        )
                    }
                }
                status = Status.WEAK_ERROR
            }
        }

        private fun reportUnmappedParameters() {
            for (valueParameter in parameters) {
                if (!usedParameters.contains(valueParameter)) {
                    if (valueParameter.hasDefaultValue()) {
                        candidateCall.recordValueArgument(valueParameter, DefaultValueArgument.DEFAULT)
                    }/* else if (valueParameter.getVarargElementType() != null) {
                        candidateCall.recordValueArgument(valueParameter, VarargValueArgument())
                    }*/ else {
                        tracing.noValueForParameter(candidateCall.trace, valueParameter)
                        status = Status.ERROR
                    }
                }
            }
        }

        private fun putVararg(valueParameterDescriptor: ValueParameterDescriptor, valueArgument: ValueArgument) {
//            if (valueParameterDescriptor.getVarargElementType() != null) {
//                val vararg: VarargValueArgument =
//                    varargs.computeIfAbsent(valueParameterDescriptor) { k -> VarargValueArgument() }
//                vararg.addArgument(valueArgument)
//            } else {
//            val spread = valueArgument.getSpreadElement()
//            if (spread != null) {
//                candidateCall.trace.report(NON_VARARG_SPREAD.onError(spread))
//                status  = Status.WEAK_ERROR
//            }
            val argument: ResolvedValueArgument = ExpressionValueArgument(valueArgument)
            candidateCall.recordValueArgument(valueParameterDescriptor, argument)
//            }
        }


        private fun report(diagnostic: Diagnostic) {
            candidateCall.trace.report(diagnostic)
        }

        private interface ProcessorState {
            fun processNamedArgument(argument: ValueArgument): ProcessorState

            fun processPositionedArgument(argument: ValueArgument): ProcessorState

            fun processArraySetRHS(argument: ValueArgument): ProcessorState
        }
    }
}
