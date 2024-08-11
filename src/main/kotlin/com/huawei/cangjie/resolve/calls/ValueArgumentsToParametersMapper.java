//package com.huawei.cangjie.resolve.calls;
//
//import com.huawei.cangjie.config.LanguageVersionSettings;
//import com.huawei.cangjie.descriptors.CallableDescriptor;
//import com.huawei.cangjie.descriptors.ValueParameterDescriptor;
//import com.huawei.cangjie.name.Name;
//import com.huawei.cangjie.psi.*;
//import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall;
//import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy;
//import com.huawei.cangjie.resolve.calls.util.CallUtilKt;
//import com.intellij.psi.impl.source.tree.LeafPsiElement;
//import kotlin.collections.CollectionsCj;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.*;
//
//import static com.huawei.cangjie.descriptors.Errors.TOO_MANY_ARGUMENTS;
//
//public class ValueArgumentsToParametersMapper {
//
//
//    public enum Status {
//        ERROR(false),
//        WEAK_ERROR(false),
//        OK(true);
//
//        private final boolean success;
//
//        Status(boolean success) {
//            this.success = success;
//        }
//
//        public boolean isSuccess() {
//            return success;
//        }
//
//        public Status compose(Status other) {
//            if (this == ERROR || other == ERROR) return ERROR;
//            if (this == WEAK_ERROR || other == WEAK_ERROR) return WEAK_ERROR;
//            return this;
//        }
//    }
//    public static <D extends CallableDescriptor> Status mapValueArgumentsToParameters(
//            @NotNull Call call,
//            @NotNull TracingStrategy tracing,
//            @NotNull MutableResolvedCall<D> candidateCall,
//            @NotNull LanguageVersionSettings languageVersionSettings
//    ) {
//        //return new ValueArgumentsToParametersMapper().process(call, tracing, candidateCall, unmappedArguments);
//        Processor<D> processor = new Processor<>(call, candidateCall, tracing, languageVersionSettings);
//        processor.process();
//        return processor.status;
//    }
//
//    private static class Processor<D extends CallableDescriptor> {
//        private final Call call;
//        private final TracingStrategy tracing;
//        private final MutableResolvedCall<D> candidateCall;
//        private final LanguageVersionSettings languageVersionSettings;
//        private final List<ValueParameterDescriptor> parameters;
//
//        private final Map<Name,ValueParameterDescriptor> parameterByName;
//        private Map<Name,ValueParameterDescriptor> parameterByNameInOverriddenMethods;
//
////        private final Map<ValueParameterDescriptor, VarargValueArgument> varargs = new HashMap<>();
//        private final Set<ValueParameterDescriptor> usedParameters = new HashSet<>();
//        private Status status = Status.OK;
//
//        private Processor(
//                @NotNull Call call,
//                @NotNull MutableResolvedCall<D> candidateCall,
//                @NotNull TracingStrategy tracing,
//                @NotNull LanguageVersionSettings languageVersionSettings
//        ) {
//            this.call = call;
//            this.tracing = tracing;
//            this.candidateCall = candidateCall;
//            this.parameters = candidateCall.getCandidateDescriptor().getValueParameters();
//            this.languageVersionSettings = languageVersionSettings;
//
//            this.parameterByName = new HashMap<>();
//            for (ValueParameterDescriptor valueParameter : parameters) {
//                parameterByName.put(valueParameter.getName(), valueParameter);
//            }
//        }
//
//        @Nullable
//        private ValueParameterDescriptor getParameterByNameInOverriddenMethods(Name name) {
//            if (parameterByNameInOverriddenMethods == null) {
//                parameterByNameInOverriddenMethods = new HashMap<>();
//                for (ValueParameterDescriptor valueParameter : parameters) {
//                    for (ValueParameterDescriptor parameterDescriptor : valueParameter.getOverriddenDescriptors()) {
//                        parameterByNameInOverriddenMethods.put(parameterDescriptor.getName(), valueParameter);
//                    }
//                }
//            }
//
//            return parameterByNameInOverriddenMethods.get(name);
//        }
//
//        // We saw only positioned arguments so far
//        private final ProcessorState positionedOnly = new ProcessorState() {
//            private int currentParameter = 0;
//
//            private int numberOfParametersForPositionedArguments() {
//                return call.getCallType() == Call.CallType.ARRAY_SET_METHOD ? parameters.size() - 1 : parameters.size();
//            }
//
//            @Nullable
//            public ValueParameterDescriptor nextValueParameter() {
//                if (currentParameter >= numberOfParametersForPositionedArguments()) return null;
//
//                ValueParameterDescriptor head = parameters.get(currentParameter);
//
//                // If we found a vararg parameter, we are stuck with it forever
////                if (head.getVarargElementType() == null) {
////                    currentParameter++;
////                }
//
//                return head;
//            }
//
//            @Override
//            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
//                return positionedThenNamed.processNamedArgument(argument);
//            }
//
//            @Override
//            public ProcessorState processPositionedArgument(@NotNull ValueArgument argument) {
//                processArgument(argument, nextValueParameter());
//                return positionedOnly;
//            }
//
//            @Override
//            public ProcessorState processArraySetRHS(@NotNull ValueArgument argument) {
//                processArgument(argument, CollectionsCj.lastOrNull(parameters));
//                return positionedOnly;
//            }
//
//            private void processArgument(@NotNull ValueArgument argument, @Nullable ValueParameterDescriptor parameter) {
//                if (parameter != null) {
//                    usedParameters.add(parameter);
//                    putVararg(parameter, argument);
//                }
//                else {
//                    report(TOO_MANY_ARGUMENTS.on(argument.asElement(), candidateCall.getCandidateDescriptor()));
//                    setStatus(WEAK_ERROR);
//                }
//            }
//        };
//
//        // We saw zero or more positioned arguments and then a named one
//        private final ProcessorState positionedThenNamed = new ProcessorState() {
//            @Override
//            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
//                assert argument.isNamed();
//
//                D candidate = candidateCall.getCandidateDescriptor();
//
//                ValueArgumentName argumentName = argument.getArgumentName();
//                assert argumentName != null;
//                ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(argumentName.getAsName());
//                CjSimpleNameExpression nameReference = argumentName.getReferenceExpression();
//
//                if (!languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//                    ReservedCheckingCj.checkReservedYield(nameReference, candidateCall.getTrace());
//                }
//                if (nameReference != null) {
//                    if (candidate instanceof MemberDescriptor && ((MemberDescriptor) candidate).isExpect() &&
//                            candidate.getContainingDeclaration() instanceof ClassDescriptor) {
//                        // We do not allow named arguments for members of expected classes until we're able to use both
//                        // expected and actual definitions when compiling platform code
//                        report(NAMED_ARGUMENTS_NOT_ALLOWED.on(nameReference, EXPECTED_CLASS_MEMBER));
//                    }
//                    else if (!candidate.hasStableParameterNames()) {
//                        BadNamedArgumentsTarget badNamedArgumentsTarget;
//                        if (candidate instanceof FunctionInvokeDescriptor) {
//                            badNamedArgumentsTarget = INVOKE_ON_FUNCTION_TYPE;
//                        } else if (candidate instanceof DeserializedCallableMemberDescriptor) {
//                            badNamedArgumentsTarget = INTEROP_FUNCTION;
//                        } else {
//                            badNamedArgumentsTarget = NON_KOTLIN_FUNCTION;
//                        }
//
//                        report(NAMED_ARGUMENTS_NOT_ALLOWED.on(nameReference, badNamedArgumentsTarget));
//                    }
//                }
//
//                if (candidate.hasStableParameterNames() && nameReference != null  &&
//                        candidate instanceof CallableMemberDescriptor && ((CallableMemberDescriptor)candidate).getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
//                    if (valueParameterDescriptor == null) {
//                        valueParameterDescriptor = getParameterByNameInOverriddenMethods(argumentName.getAsName());
//                    }
//
//                    if (valueParameterDescriptor != null) {
//                        for (ValueParameterDescriptor parameterFromSuperclass : valueParameterDescriptor.getOverriddenDescriptors()) {
//                            if (OverrideResolver.Companion.shouldReportParameterNameOverrideWarning(valueParameterDescriptor, parameterFromSuperclass)) {
//                                report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference));
//                            }
//                        }
//                    }
//                }
//
//                if (valueParameterDescriptor == null) {
//                    if (nameReference != null) {
//                        report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference));
//                    }
//                    setStatus(WEAK_ERROR);
//                }
//                else {
//                    if (nameReference != null) {
//                        candidateCall.getTrace().record(REFERENCE_TARGET, nameReference, valueParameterDescriptor);
//                    }
//                    if (!usedParameters.add(valueParameterDescriptor)) {
//                        if (nameReference != null) {
//                            report(ARGUMENT_PASSED_TWICE.on(nameReference));
//                        }
//                        setStatus(WEAK_ERROR);
//                    }
//                    else {
//                        putVararg(valueParameterDescriptor, argument);
//                    }
//                }
//
//                return positionedThenNamed;
//            }
//
//            @Override
//            public ProcessorState processPositionedArgument(@NotNull ValueArgument argument) {
//                report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(argument.asElement()));
//                setStatus(WEAK_ERROR);
//
//                return positionedThenNamed;
//            }
//
//            @Override
//            public ProcessorState processArraySetRHS(@NotNull ValueArgument argument) {
//                throw new IllegalStateException("Array set RHS cannot appear after a named argument syntactically: " + argument);
//            }
//        };
//
//        public void process() {
//            ProcessorState state = positionedOnly;
//            boolean isArraySetMethod = call.getCallType() == Call.CallType.ARRAY_SET_METHOD;
//            List<? extends ValueArgument> argumentsInParentheses = CallUtilCj.getValueArgumentsInParentheses(call);
//            for (Iterator<? extends ValueArgument> iterator = argumentsInParentheses.iterator(); iterator.hasNext(); ) {
//                ValueArgument valueArgument = iterator.next();
//                if (valueArgument.isNamed()) {
//                    state = state.processNamedArgument(valueArgument);
//                }
//                else if (isArraySetMethod && !iterator.hasNext()) {
//                    state = state.processArraySetRHS(valueArgument);
//                }
//                else {
//                    state = state.processPositionedArgument(valueArgument);
//                }
//            }
//
//            for (Map.Entry<ValueParameterDescriptor, VarargValueArgument> entry : varargs.entrySet()) {
//                candidateCall.recordValueArgument(entry.getKey(), entry.getValue());
//            }
//
//            processFunctionLiteralArguments();
//            reportUnmappedParameters();
//        }
//
//        private void processFunctionLiteralArguments() {
//            List<? extends LambdaArgument> functionLiteralArguments = call.getFunctionLiteralArguments();
//            if (functionLiteralArguments.isEmpty()) return;
//
//            LambdaArgument lambdaArgument = functionLiteralArguments.get(0);
//            CjExpression possiblyLabeledFunctionLiteral = lambdaArgument.getArgumentExpression();
//
//            if (parameters.isEmpty()) {
//                CallUtilCj.reportTrailingLambdaErrorOr(
//                        candidateCall.getTrace(), possiblyLabeledFunctionLiteral,
//                        expression -> TOO_MANY_ARGUMENTS.on(expression, candidateCall.getCandidateDescriptor())
//                );
//                setStatus(ERROR);
//            }
//            else {
//                ValueParameterDescriptor lastParameter = CollectionsKt.last(parameters);
////                if (lastParameter.getVarargElementType() != null) {
////                    CallUtilCj.reportTrailingLambdaErrorOr(
////                            candidateCall.getTrace(), possiblyLabeledFunctionLiteral,
////                            expression -> VARARG_OUTSIDE_PARENTHESES.on(expression)
////                    );
////                    setStatus(ERROR);
////                }
//                else if (!usedParameters.add(lastParameter)) {
//                    CallUtilKt.reportTrailingLambdaErrorOr(
//                            candidateCall.getTrace(), possiblyLabeledFunctionLiteral,
//                            expr -> TOO_MANY_ARGUMENTS.on(expr, candidateCall.getCandidateDescriptor())
//                    );
//                    setStatus(Status.WEAK_ERROR);
//                }
//                else {
//                    putVararg(lastParameter, lambdaArgument);
//                }
//            }
//
//            for (int i = 1; i < functionLiteralArguments.size(); i++) {
//                CjExpression argument = functionLiteralArguments.get(i).getArgumentExpression();
//                if (argument instanceof CjLambdaExpression) {
//                    report(MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(argument));
//                    if (CallUtilKt.isTrailingLambdaOnNewLIne((CjLambdaExpression) argument)) {
//                        report(UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on((CjLambdaExpression) argument));
//                    }
//                }
//                setStatus(WEAK_ERROR);
//            }
//        }
//
//        private void reportUnmappedParameters() {
//            for (ValueParameterDescriptor valueParameter : parameters) {
//                if (!usedParameters.contains(valueParameter)) {
//                    if (ArgumentsUtilsCj.hasDefaultValue(valueParameter)) {
//                        candidateCall.recordValueArgument(valueParameter, DefaultValueArgument.DEFAULT);
//                    }
//                    else if (valueParameter.getVarargElementType() != null) {
//                        candidateCall.recordValueArgument(valueParameter, new VarargValueArgument());
//                    }
//                    else {
//                        tracing.noValueForParameter(candidateCall.getTrace(), valueParameter);
//                        setStatus(ERROR);
//                    }
//                }
//            }
//        }
//
//        private void putVararg(ValueParameterDescriptor valueParameterDescriptor, ValueArgument valueArgument) {
//            if (valueParameterDescriptor.getVarargElementType() != null) {
//                VarargValueArgument vararg = varargs.computeIfAbsent(valueParameterDescriptor, k -> new VarargValueArgument());
//                vararg.addArgument(valueArgument);
//            }
//            else {
//                LeafPsiElement spread = valueArgument.getSpreadElement();
//                if (spread != null) {
//                    candidateCall.getTrace().report(NON_VARARG_SPREAD.onError(spread));
//                    setStatus(WEAK_ERROR);
//                }
//                ResolvedValueArgument argument = new ExpressionValueArgument(valueArgument);
//                candidateCall.recordValueArgument(valueParameterDescriptor, argument);
//            }
//        }
//
//        private void setStatus(@NotNull Status newStatus) {
//            status = status.compose(newStatus);
//        }
//
//        private void report(Diagnostic diagnostic) {
//            candidateCall.getTrace().report(diagnostic);
//        }
//
//        private interface ProcessorState {
//            ProcessorState processNamedArgument(@NotNull ValueArgument argument);
//
//            ProcessorState processPositionedArgument(@NotNull ValueArgument argument);
//
//            ProcessorState processArraySetRHS(@NotNull ValueArgument argument);
//        }
//    }
//
//    private ValueArgumentsToParametersMapper() {}
//}
