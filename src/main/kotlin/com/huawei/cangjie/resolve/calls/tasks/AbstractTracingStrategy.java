package com.huawei.cangjie.resolve.calls.tasks;


import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithVisibility;
import com.huawei.cangjie.descriptors.ValueParameterDescriptor;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.util.CallUtilKt;
import com.huawei.cangjie.types.CangJieType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

import static com.huawei.cangjie.diagnostics.Errors.*;
import static com.huawei.cangjie.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;

public abstract class AbstractTracingStrategy implements TracingStrategy {
    protected final CjExpression reference;
    protected final Call call;

    protected AbstractTracingStrategy(@NotNull CjExpression reference, @NotNull Call call) {
        this.reference = reference;
        this.call = call;
    }


    private void reportUnsafeCallOnBinaryExpression(@NotNull BindingTrace trace, @NotNull CjBinaryExpression binaryExpression) {
//        CjSimpleNameExpression operationReference = binaryExpression.getOperationReference();
//        boolean isInfixCall = operationReference.getReferencedNameElementType() == CjTokens.IDENTIFIER;
//        Name operationString = isInfixCall ?
//                Name.identifier(operationReference.getText()) :
//                OperatorConventions.getNameForOperationSymbol((CjTokens) operationReference.getReferencedNameElementType());
//
//        if (operationString == null) return;
//
//        CjExpression left = binaryExpression.getLeft();
//        CjExpression right = binaryExpression.getRight();
//        if (left == null || right == null) return;
//
//        if (isInfixCall) {
//            trace.report(UNSAFE_INFIX_CALL.on(reference, left, operationString.asString(), right));
//        }
//        else {
//            boolean inOperation = CjPsiUtil.isInOrNotInOperation(binaryExpression);
//            CjExpression receiver = inOperation ? right : left;
//            CjExpression argument = inOperation ? left : right;
//            trace.report(UNSAFE_OPERATOR_CALL.on(reference, receiver, operationString.asString(), argument));
//        }
    }
    @Override
    public void unsafeCall(@NotNull BindingTrace trace, @NotNull CangJieType type, boolean isCallForImplicitInvoke) {
        ASTNode callOperationNode = call.getCallOperationNode();
        if (callOperationNode != null && !isCallForImplicitInvoke) {
            trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
        }
        else {
            PsiElement callElement = call.getCallElement();
            if (callElement instanceof CjBinaryExpression) {
                reportUnsafeCallOnBinaryExpression(trace, (CjBinaryExpression) callElement);
            }
            else if (isCallForImplicitInvoke) {
                trace.report(UNSAFE_IMPLICIT_INVOKE_CALL.on(reference, type));
            }
            else {
                trace.report(UNSAFE_CALL.on(reference, type));
            }
        }
    }
    @Override
    public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {
        trace.report(NONE_APPLICABLE.on(reference, descriptors));
    }
    @Override
    public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {
        CjElement reportOn = CallUtilKt.getValueArgumentListOrElement(call);
        trace.report(NO_VALUE_FOR_PARAMETER.on(reportOn, valueParameter));
    }

    @Override
    public <D extends CallableDescriptor> void cannotCompleteResolve(
            @NotNull BindingTrace trace,
            @NotNull Collection<? extends ResolvedCall<D>> descriptors
    ) {
        trace.report(CANNOT_COMPLETE_RESOLVE.on(reference, descriptors));
    }
    @Override
    public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {
        trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getVisibility(), descriptor));
    }

    @Override
    public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls) {
        trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(reference, resolvedCalls));
    }

    @Override
    public <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
        Collection<D> descriptors = new HashSet<>();
        for (ResolvedCall<D> candidate : candidates) {
            descriptors.add(candidate.getCandidateDescriptor());
        }
        trace.record(AMBIGUOUS_REFERENCE_TARGET, reference, descriptors);
    }

//    @Override
//    public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {
//        CjElement reportOn = CallUtilKt.getValueArgumentListOrElement(call);
//        trace.report(NO_VALUE_FOR_PARAMETER.on(reportOn, valueParameter));
//    }
//
//    @Override
//    public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver) {
//        trace.report(MISSING_RECEIVER.on(reference, expectedReceiver.getType()));
//    }
//
//    @Override
//    public void wrongReceiverType(
//            @NotNull BindingTrace trace,
//            @NotNull ReceiverParameterDescriptor receiverParameter,
//            @NotNull ReceiverValue receiverArgument,
//            @NotNull ResolutionContext<?> c
//    ) {
//        CjExpression reportOn = receiverArgument instanceof ExpressionReceiver
//                ? ((ExpressionReceiver) receiverArgument).getExpression()
//                : reference;
//
//        if (!DiagnosticUtilsCj.reportTypeMismatchDueToTypeProjection(
//                c, reportOn, receiverParameter.getType(), receiverArgument.getType())) {
//            trace.report(TYPE_MISMATCH.on(reportOn, receiverParameter.getType(), receiverArgument.getType()));
//        }
//    }
//
//    @Override
//    public void noReceiverAllowed(@NotNull BindingTrace trace) {
//        trace.report(NO_RECEIVER_ALLOWED.on(reference));
//    }
//
//    @Override
//    public void wrongNumberOfTypeArguments(
//            @NotNull BindingTrace trace, int expectedTypeArgumentCount, @NotNull CallableDescriptor descriptor
//    ) {
//        CjTypeArgumentList typeArgumentList = call.getTypeArgumentList();
//        trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
//                typeArgumentList != null ? typeArgumentList : reference, expectedTypeArgumentCount, descriptor
//        ));
//    }
//
//    @Override
//    public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls) {
//        trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(reference, resolvedCalls));
//    }
//
//    @Override
//    public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {
//        trace.report(NONE_APPLICABLE.on(reference, descriptors));
//    }
//
//    @Override
//    public <D extends CallableDescriptor> void cannotCompleteResolve(
//            @NotNull BindingTrace trace,
//            @NotNull Collection<? extends ResolvedCall<D>> descriptors
//    ) {
//        trace.report(CANNOT_COMPLETE_RESOLVE.on(reference, descriptors));
//    }
//
//    @Override
//    public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
//        trace.report(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(call.getCallElement()));
//    }
//
//    @Override
//    public void recursiveType(@NotNull BindingTrace trace, @NotNull LanguageVersionSettings languageVersionSettings, bool insideAugmentedAssignment) {
//        CjExpression expression = call.getCalleeExpression();
//        if (expression == null) return;
//        if (insideAugmentedAssignment) {
//            trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT.on(languageVersionSettings, expression));
//        } else {
//            trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(languageVersionSettings, expression));
//        }
//    }
//
//    @Override
//    public void abstractSuperCall(@NotNull BindingTrace trace) {
//        trace.report(ABSTRACT_SUPER_CALL.on(reference));
//    }
//
//    @Override
//    public void abstractSuperCallWarning(@NotNull BindingTrace trace) {
//        trace.report(ABSTRACT_SUPER_CALL_WARNING.on(reference));
//    }
//
//    @Override
//    public void nestedClassAccessViaInstanceReference(
//            @NotNull BindingTrace trace,
//            @NotNull ClassDescriptor classDescriptor,
//            @NotNull ExplicitReceiverKind explicitReceiverKind
//    ) {
//        if (explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER) {
//            DeclarationDescriptor importableDescriptor = DescriptorUtilsCj.getImportableDescriptor(classDescriptor);
//            if (DescriptorUtils.getFqName(importableDescriptor).isSafe()) {
//                FqName fqName = getFqNameFromTopLevelClass(importableDescriptor);
//                String qualifiedName;
//                if (reference.getParent() instanceof CjCallableReferenceExpression) {
//                    qualifiedName = fqName.parent() + "::" + classDescriptor.getName();
//                }
//                else {
//                    qualifiedName = fqName.asString();
//                }
//                trace.report(NESTED_CLASS_SHOULD_BE_QUALIFIED.on(reference, classDescriptor, qualifiedName));
//                return;
//            }
//        }
//        trace.report(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE.on(reference, classDescriptor));
//    }
//
//    @Override
//    public void unsafeCall(@NotNull BindingTrace trace, @NotNull CangJieType type, bool isCallForImplicitInvoke) {
//        ASTNode callOperationNode = call.getCallOperationNode();
//        if (callOperationNode != null && !isCallForImplicitInvoke) {
//            trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
//        }
//        else {
//            PsiElement callElement = call.getCallElement();
//            if (callElement instanceof CjBinaryExpression) {
//                reportUnsafeCallOnBinaryExpression(trace, (CjBinaryExpression) callElement);
//            }
//            else if (isCallForImplicitInvoke) {
//                trace.report(UNSAFE_IMPLICIT_INVOKE_CALL.on(reference, type));
//            }
//            else {
//                trace.report(UNSAFE_CALL.on(reference, type));
//            }
//        }
//    }
//
//    private void reportUnsafeCallOnBinaryExpression(@NotNull BindingTrace trace, @NotNull CjBinaryExpression binaryExpression) {
//        CjSimpleNameExpression operationReference = binaryExpression.getOperationReference();
//        bool isInfixCall = operationReference.getReferencedNameElementType() == CjTokens.IDENTIFIER;
//        Name operationString = isInfixCall ?
//                Name.identifier(operationReference.getText()) :
//                OperatorConventions.getNameForOperationSymbol((CjToken) operationReference.getReferencedNameElementType());
//
//        if (operationString == null) return;
//
//        CjExpression left = binaryExpression.getLeft();
//        CjExpression right = binaryExpression.getRight();
//        if (left == null || right == null) return;
//
//        if (isInfixCall) {
//            trace.report(UNSAFE_INFIX_CALL.on(reference, left, operationString.asString(), right));
//        }
//        else {
//            bool inOperation = CjPsiUtil.isInOrNotInOperation(binaryExpression);
//            CjExpression receiver = inOperation ? right : left;
//            CjExpression argument = inOperation ? left : right;
//            trace.report(UNSAFE_OPERATOR_CALL.on(reference, receiver, operationString.asString(), argument));
//        }
//    }
//
//    @Override
//    public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {
//        trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getVisibility(), descriptor));
//    }
//
//    @Override
//    public void typeInferenceFailed(@NotNull ResolutionContext<?> context, @NotNull InferenceErrorData data) {
//        Diagnostic diagnostic = typeInferenceFailedDiagnostic(context, data, reference, call);
//        if (diagnostic != null) {
//            context.trace.report(diagnostic);
//        }
//    }
//
//    @Nullable
//    public static Diagnostic typeInferenceFailedDiagnostic(
//            @NotNull ResolutionContext<?> context,
//            @NotNull InferenceErrorData data,
//            @NotNull CjExpression reference,
//            @NotNull Call call
//    ) {
//        ConstraintSystem constraintSystem = data.constraintSystem;
//        ConstraintSystemStatus status = constraintSystem.getStatus();
//        assert !status.isSuccessful() : "Report error only for not successful constraint system";
//
//        if (status.hasErrorInConstrainingTypes()) {
//            // Do not report type inference errors if there is one in the arguments
//            // (it's useful, when the arguments, e.g. lambdas or calls are incomplete)
//            return null;
//        }
//        if (status.hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION)) {
//            CangJieType declaredReturnType = data.descriptor.getReturnType();
//            if (declaredReturnType == null) return null;
//
//            ConstraintSystem systemWithoutExpectedTypeConstraint = filterConstraintsOut(constraintSystem, EXPECTED_TYPE_POSITION);
//            CangJieType substitutedReturnType = systemWithoutExpectedTypeConstraint.getResultingSubstitutor().substitute(
//                    declaredReturnType, Variance.OUT_VARIANCE);
//            assert substitutedReturnType != null; //todo
//
//            assert !noExpectedType(data.expectedType) : "Expected type doesn't exist, but there is an expected type mismatch error";
//            if (!DiagnosticUtilsCj.reportTypeMismatchDueToTypeProjection(
//                    context, call.getCallElement(), data.expectedType, substitutedReturnType)) {
//                return TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(call.getCallElement(), data.expectedType, substitutedReturnType);
//            }
//        }
//        else if (status.hasCannotCaptureTypesError()) {
//            return TYPE_INFERENCE_CANNOT_CAPTURE_TYPES.on(reference, data);
//        }
//        else if (status.hasViolatedUpperBound()) {
//            return TYPE_INFERENCE_UPPER_BOUND_VIOLATED.on(reference, data);
//        }
//        else if (status.hasParameterConstraintError()) {
//            return TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR.on(reference, data);
//        }
//        else if (status.hasConflictingConstraints()) {
//            return TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS.on(reference, data);
//        }
//        else if (status.hasTypeInferenceIncorporationError()) {
//            return TYPE_INFERENCE_INCORPORATION_ERROR.on(reference);
//        }
//        else if (status.hasTypeParameterWithUnsatisfiedOnlyInputTypesError()) {
//            //todo
//            return TYPE_INFERENCE_ONLY_INPUT_TYPES.getErrorFactory().on(reference, data.descriptor.getTypeParameters().get(0));
//        }
//        else {
//            assert status.hasUnknownParameters();
//            return TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(reference, data);
//        }
//
//        return null;
//    }
}
