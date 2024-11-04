package com.linqingying.cangjie.types.expressions;

import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.config.LanguageFeature;
import com.linqingying.cangjie.descriptors.DeclarationDescriptor;
import com.linqingying.cangjie.descriptors.FunctionDescriptor;
import com.linqingying.cangjie.descriptors.PropertyDescriptor;
import com.linqingying.cangjie.descriptors.VariableDescriptor;
import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.psi.*;
import com.linqingying.cangjie.resolve.BindingContext;
import com.linqingying.cangjie.resolve.BindingContextUtils;
import com.linqingying.cangjie.resolve.TemporaryBindingTrace;
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver;
import com.linqingying.cangjie.resolve.calls.checkers.AssignmentChecker;
import com.linqingying.cangjie.resolve.calls.checkers.CallCheckerContext;
import com.linqingying.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker;
import com.linqingying.cangjie.resolve.calls.context.CallPosition;
import com.linqingying.cangjie.resolve.calls.context.ContextDependency;
import com.linqingying.cangjie.resolve.calls.context.ResolutionContext;
import com.linqingying.cangjie.resolve.calls.context.TemporaryTraceAndCache;
import com.linqingying.cangjie.resolve.calls.inference.BuilderInferenceSession;
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall;
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResults;
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsImpl;
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsUtil;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.linqingying.cangjie.resolve.extensions.AssignResolutionAltererExtension;
import com.linqingying.cangjie.resolve.scopes.LexicalWritableScope;
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.CangJieTypeKt;
import com.linqingying.cangjie.types.checker.CangJieTypeChecker;
import com.linqingying.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.linqingying.cangjie.types.util.TypeUtils;
import com.linqingying.cangjie.utils.OperatorNameConventions;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
import com.linqingying.cangjie.utils.exceptions.OperatorConventions;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.tree.IElementType;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static com.linqingying.cangjie.diagnostics.Errors.*;
import static com.linqingying.cangjie.psi.CjPsiUtil.deparenthesize;
import static com.linqingying.cangjie.resolve.BindingContext.*;
import static com.linqingying.cangjie.resolve.calls.context.ContextDependency.INDEPENDENT;
import static com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;
import static com.linqingying.cangjie.types.util.TypeUtils.noExpectedType;
import static com.linqingying.cangjie.utils.exceptions.OperatorConventions.getNameForOperationSymbol;

@SuppressWarnings("SuspiciousMethodCalls")
public class ExpressionTypingVisitorForStatements extends ExpressionTypingVisitor {
    private final LexicalWritableScope scope;
    private final BasicExpressionTypingVisitor basic;
    private final ControlStructureTypingVisitor controlStructures;
    //    private final PatternMatchingTypingVisitor patterns;
    private final FunctionsTypingVisitor functions;

    public ExpressionTypingVisitorForStatements(
            @NotNull ExpressionTypingInternals facade,
            @NotNull LexicalWritableScope scope,
            @NotNull BasicExpressionTypingVisitor basic,
            @NotNull ControlStructureTypingVisitor controlStructures,
//            @NotNull PatternMatchingTypingVisitor patterns,
            @NotNull FunctionsTypingVisitor functions
    ) {
        super(facade);
        this.scope = scope;
        this.basic = basic;
        this.controlStructures = controlStructures;
//        this.patterns = patterns;
        this.functions = functions;
    }

    @Nullable
    private static CangJieType refineTypeFromPropertySetterIfPossible(
            @NotNull BindingContext bindingContext,
            @Nullable CjElement leftOperand,
            @Nullable CangJieType leftOperandType
    ) {
        VariableDescriptor descriptor = BindingContextUtils.extractVariableFromResolvedCall(bindingContext, leftOperand);

//        if (descriptor instanceof PropertyDescriptor) {
//            PropertySetterDescriptor setter = ((PropertyDescriptor) descriptor).getSetter();
//            if (setter != null) return setter.getValueParameters().get(0).getType();
//        }

        return leftOperandType;
    }

    @Nullable
    private static CangJieType refineTypeByPropertyInType(
            @NotNull BindingContext bindingContext,
            @Nullable CjElement leftOperand,
            @Nullable CangJieType leftOperandType
    ) {
        VariableDescriptor descriptor = BindingContextUtils.extractVariableFromResolvedCall(bindingContext, leftOperand);

        if (descriptor instanceof PropertyDescriptor) {
            CangJieType inType = ((PropertyDescriptor) descriptor).getInType();
            if (inType != null) return inType;
        }

        return leftOperandType;
    }

    private static boolean atLeastOneOperation(Collection<? extends ResolvedCall<FunctionDescriptor>> calls, Name operationName) {
        for (ResolvedCall<FunctionDescriptor> call : calls) {
            if (call.getCandidateDescriptor().getName().equals(operationName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CangJieTypeInfo visitNamedFunction(@NotNull CjNamedFunction function, ExpressionTypingContext context) {
        return functions.visitNamedFunction(function, context, /* isDeclaration = */ function.getName() != null, scope);
    }

    @Override
    public CangJieTypeInfo visitBlockExpression(@NotNull CjBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, true);
    }

    @Override
    public CangJieTypeInfo visitDeclaration(@NotNull CjDeclaration dcl, ExpressionTypingContext context) {
        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(dcl, context), context);
    }

    @Override
    public CangJieTypeInfo visitIfExpression(@NotNull CjIfExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitIfExpression(expression, context);
    }


    @Override
    public CangJieTypeInfo visitCjElement(@NotNull CjElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @Nullable
    private CangJieType checkAssignmentType(
            @Nullable CangJieType assignmentType,
            @NotNull CjBinaryExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        if (assignmentType != null && !CangJieBuiltIns.isUnit(assignmentType) && !noExpectedType(context.expectedType) &&
                !CangJieTypeKt.isError(context.expectedType) && TypeUtils.equalTypes(context.expectedType, assignmentType)) {
            context.trace.report(ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return components.dataFlowAnalyzer.checkStatementType(expression, context);
    }

    @NotNull
    protected CangJieTypeInfo visitAssignment(CjBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(scope).replaceContextDependency(INDEPENDENT);
        CjExpression leftOperand = expression.getLeft();
//        if (leftOperand instanceof CjArrayAccessExpression) {
//            basic.resolveAnnotationsOnExpression((CjArrayAccessExpression) leftOperand, context);
//        }
        CjExpression left = deparenthesize(leftOperand);
        CjExpression right = expression.getRight();
//        if (left instanceof CjArrayAccessExpression) {
//            CjArrayAccessExpression arrayAccessExpression = (CjArrayAccessExpression) left;
//            if (right == null) return TypeInfoFactoryKt.noTypeInfo(context);
//            CangJieTypeInfo typeInfo = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context, context.trace);
//            basic.checkLValue(context.trace, context, arrayAccessExpression, right, expression, true);
//            return typeInfo.replaceType(checkAssignmentType(typeInfo.getType(), expression, contextWithExpectedType));
//        }
        CangJieTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(
                left,
                context.replaceCallPosition(new CallPosition.VariableAssignment(left, true)),
                facade
        );

        BindingContext bindingContext = context.trace.getBindingContext();
        CangJieType leftType = leftInfo.getType();

        CangJieType expectedType =
                refineTypeFromPropertySetterIfPossible(bindingContext, leftOperand, leftType);

        List<AssignResolutionAltererExtension> assignAlterers = AssignResolutionAltererExtension.Companion.getInstances(expression.getProject());
        if (!assignAlterers.isEmpty()) {
            CangJieTypeInfo alteredTypeInfo = assignAlterers.stream()
                    .filter((it) -> it.needOverloadAssign(expression, leftType, bindingContext))
                    .map((it) -> it.resolveAssign(bindingContext, expression, leftOperand, left, leftInfo, context, components, scope))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (alteredTypeInfo != null) {
                return alteredTypeInfo;
            }
        }

        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();
        CangJieTypeInfo resultInfo;
        if (right != null) {
            resultInfo = facade.getTypeInfo(
                    right,
                    context.replaceDataFlowInfo(dataFlowInfo)
                            .replaceExpectedType(expectedType)
                            .replaceCallPosition(new CallPosition.VariableAssignment(leftOperand, false))
            );

            dataFlowInfo = resultInfo.getDataFlowInfo();
            CangJieType rightType = resultInfo.getType();
            if (left != null && expectedType != null && rightType != null) {
                DataFlowValue leftValue = components.dataFlowValueFactory.createDataFlowValue(left, expectedType, context);
                DataFlowValue rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context);
                // We cannot say here anything new about rightValue except it has the same value as leftValue
                resultInfo = resultInfo.replaceDataFlowInfo(dataFlowInfo.assign(leftValue, rightValue/*, components.languageVersionSettings*/));
                NewSchemeOfIntegerOperatorResolutionChecker.checkArgument(expectedType, right, context.trace, components.moduleDescriptor);
            }
        } else {
            resultInfo = leftInfo;
        }
        if (expectedType != null && leftOperand != null) { //if expectedType == null, some other error has been generated
            basic.checkLValue(context.trace, context, leftOperand, right, expression, false);

            CallCheckerContext callCheckerContext =
                    new CallCheckerContext(
                            context,
                            components.deprecationResolver,
                            components.moduleDescriptor,
                            components.missingSupertypesResolver,
                            components.callComponents,
                            context.trace
                    );
            for (AssignmentChecker checker : components.assignmentCheckers) {
                checker.check(expression, callCheckerContext);
            }
        }


        checkPropertyInTypeWithWarnings(
                context, expression, resultInfo.getType(), resultInfo.getDataFlowInfo(), leftOperand, leftType, expectedType
        );

        return resultInfo.replaceType(components.dataFlowAnalyzer.checkStatementType(expression, contextWithExpectedType));
    }

    private void checkPropertyInTypeWithWarnings(
            @NotNull ResolutionContext<?> context,
            @NotNull CjBinaryExpression expression,
            @Nullable CangJieType rhsType,
            @NotNull DataFlowInfo rhsDataFlowInfo,
            @Nullable CjExpression lhsOperand,
            @Nullable CangJieType lhsType,
            @Nullable CangJieType expectedType
    ) {
        if (rhsType == null || expectedType == null) return;

        CangJieType expectedTypeByInType = refineTypeByPropertyInType(context.trace.getBindingContext(), lhsOperand, lhsType);

        if (expectedTypeByInType != null && expectedType != expectedTypeByInType && !TypeUtils.equalTypes(expectedType, expectedTypeByInType)) {
            Ref<Boolean> hasErrorsOnTypeChecking = Ref.create(false);
            components.dataFlowAnalyzer.checkType(
                    rhsType,
                    expression,
                    context.replaceExpectedType(expectedTypeByInType)
                            .replaceDataFlowInfo(rhsDataFlowInfo)
                            .replaceCallPosition(new CallPosition.VariableAssignment(lhsOperand, false)),
                    hasErrorsOnTypeChecking,
                    false
            );
            if (hasErrorsOnTypeChecking.get()) {
                context.trace.report(TYPE_MISMATCH_WARNING.on(expression, expectedTypeByInType, rhsType));
            }
        }
    }

    @Override
    public CangJieTypeInfo visitBinaryExpression(@NotNull CjBinaryExpression expression, ExpressionTypingContext context) {
        CjSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        CangJieTypeInfo result;
        if (operationType == CjTokens.EQ) {
            result = visitAssignment(expression, context);
        } else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context);
        } else {
            return facade.getTypeInfo(expression, context);
        }
        return components.dataFlowAnalyzer.checkType(result, expression, context);
    }

    @NotNull
    protected CangJieTypeInfo visitAssignmentOperation(CjBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        //There is a temporary binding trace for an opportunity to resolve set method for array if needed (the initial trace should be used there)
        TemporaryTraceAndCache temporary = TemporaryTraceAndCache.create(
                contextWithExpectedType, "trace to resolve array set method for binary expression", expression);
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceTraceAndCache(temporary).replaceContextDependency(INDEPENDENT);

        CjSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        CjExpression leftOperand = expression.getLeft();
        CangJieTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(leftOperand, context, facade);
        CangJieType leftType = leftInfo.getType();

        CjExpression right = expression.getRight();
        CjExpression left = leftOperand == null ? null : deparenthesize(leftOperand);
        if (right == null || left == null) {
            temporary.commit();
            return leftInfo.clearType();
        }

        if (leftType == null) {
            CangJieTypeInfo rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign, operationSign));
            temporary.commit();
            return rightInfo.clearType();
        } else if (!ArgumentTypeResolver.isFunctionLiteralOrCallableReference(right, context) &&
                !context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        ) {
            // Cache the type info for the right hand side so that we don't evaluate it twice if there is no valid plusAssign.
            // We skip over function literals and references, since ArgumentTypeResolver will only resolve the shape of the
            // function type before attempting to resolve the call.
            facade.getTypeInfo(right, context.replaceContextDependency(ContextDependency.DEPENDENT));
        }
        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(left, leftType, context.trace.getBindingContext());

        // We check that defined only one of '+=' and '+' operations, and call it (in the case '+' we then also assign)
        // Check for '+='
        Name name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);
        TemporaryTraceAndCache temporaryForAssignmentOperation = TemporaryTraceAndCache.create(
                context, "trace to check assignment operation like '+=' for", expression);
        OverloadResolutionResults<FunctionDescriptor> assignmentOperationDescriptors =
                components.callResolver.resolveBinaryCall(
                        context.replaceTraceAndCache(temporaryForAssignmentOperation).replaceScope(scope),
                        receiver, expression, name
                );
        CangJieType assignmentOperationType = OverloadResolutionResultsUtil.getResultingType(assignmentOperationDescriptors, context);

        OverloadResolutionResults<FunctionDescriptor> binaryOperationDescriptors;
        CangJieType binaryOperationType;
        TemporaryTraceAndCache temporaryForBinaryOperation = TemporaryTraceAndCache.create(
                context, "trace to check binary operation like '+' for", expression);
        TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(context.trace, "Trace for checking assignability");
        ExpressionTypingContext contextForBinaryOperation = null;

        boolean lhsAssignable = basic.checkLValue(ignoreReportsTrace, context, left, right, expression, false);

        if (assignmentOperationType == null || lhsAssignable) {
            contextForBinaryOperation = context.replaceTraceAndCache(temporaryForBinaryOperation).replaceScope(scope);
            // Check for '+'
            // We should clear calls info for coroutine inference within right side as here we analyze it a second time in another context
            if (context.inferenceSession instanceof BuilderInferenceSession) {
                ((BuilderInferenceSession) context.inferenceSession).clearCallsInfoByContainingElement(right);
            }

//            || 和 && 是不可重载运算符，但是为了处理 ||= 和 &&= 调用Basic的bool处理
//            if (OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType) == OROR || OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType) == ANDAND) {
//                return basic.visitBooleanOperationExpression(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType), left, right, context);
//            }
            Name counterpartName = getNameForOperationSymbol(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));
            binaryOperationDescriptors =
                    components.callResolver.resolveBinaryCall(contextForBinaryOperation, receiver, expression, counterpartName);

            binaryOperationType = OverloadResolutionResultsUtil.getResultingType(binaryOperationDescriptors, context);
        } else {
            binaryOperationDescriptors = OverloadResolutionResultsImpl.nameNotFound();
            binaryOperationType = null;
        }

        CangJieType type = assignmentOperationType != null ? assignmentOperationType : binaryOperationType;
        CangJieTypeInfo rightInfo = leftInfo;

        boolean hasRemAssignOperation = atLeastOneOperation(assignmentOperationDescriptors.getResultingCalls(), OperatorNameConventions.REM_ASSIGN);
        boolean hasRemBinaryOperation = atLeastOneOperation(binaryOperationDescriptors.getResultingCalls(), OperatorNameConventions.REM);

        boolean oneTypeOfModRemOperations = hasRemAssignOperation == hasRemBinaryOperation;

        boolean maybeAmbiguity = assignmentOperationDescriptors.isSuccess() && binaryOperationDescriptors.isSuccess() && oneTypeOfModRemOperations;
        boolean isResolvedToPlusAssign = assignmentOperationType != null &&
                (assignmentOperationDescriptors.isSuccess() || !binaryOperationDescriptors.isSuccess()) &&
                (!hasRemBinaryOperation || !binaryOperationDescriptors.isSuccess());

        CangJieTypeInfo rhsResolutionResult;
        // We complete resolution for 'plus' only if there may be ambiguity (in this case we can disambiguate it),
        // or it definitely won't be resolved to plus assign (in this case we would analyse right side twice)
        if (maybeAmbiguity || !isResolvedToPlusAssign) {
            rhsResolutionResult = completePlusResolution(contextForBinaryOperation, expression, binaryOperationType, left, leftInfo);
        } else {
            rhsResolutionResult = null;
        }

        if (maybeAmbiguity && rhsResolutionResult != null) {
            // Both 'plus()' and 'plusAssign()' available => ambiguity
            OverloadResolutionResults<FunctionDescriptor> ambiguityResolutionResults = OverloadResolutionResultsUtil.ambiguity(assignmentOperationDescriptors, binaryOperationDescriptors);
            context.trace.report(ASSIGN_OPERATOR_AMBIGUITY.on(operationSign, ambiguityResolutionResults.getResultingCalls()));
            Collection<DeclarationDescriptor> descriptors = new HashSet<>();
            for (ResolvedCall<?> resolvedCall : ambiguityResolutionResults.getResultingCalls()) {
                descriptors.add(resolvedCall.getResultingDescriptor());
            }
            rightInfo = rhsResolutionResult;
            context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors);
        } else if (isResolvedToPlusAssign) {
            // There's 'plusAssign()', so we do a.plusAssign(b)
            temporaryForAssignmentOperation.commit();
            if (!CangJieTypeChecker.DEFAULT.equalTypes(components.builtIns.getUnitType(), assignmentOperationType)) {
                context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
            }
        } else {
            if (rhsResolutionResult != null) {
                rightInfo = rhsResolutionResult;
            }
            // There's only 'plus()', so we try 'a = a + b'
            temporaryForBinaryOperation.commit();
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
        }
        temporary.commit();
        return rightInfo.replaceType(checkAssignmentType(type, expression, contextWithExpectedType));
    }

    @Override
    public CangJieTypeInfo visitExpression(@NotNull CjExpression expression, ExpressionTypingContext context) {
        return facade.getTypeInfo(expression, context);
    }

    private CangJieTypeInfo completePlusResolution(
            ExpressionTypingContext context,
            CjBinaryExpression expression,
            CangJieType binaryOperationType,
            CjExpression leftDeparentized,
            CangJieTypeInfo leftInfo
    ) {
        CjExpression leftOperand = expression.getLeft();
        CjExpression rightOperand = expression.getRight();

        if (leftOperand == null || rightOperand == null) return null;

//        if (leftDeparentized instanceof CjArrayAccessExpression) {
//            ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(
//                    context.trace, "trace to resolve array set method for assignment", expression));
//            basic.resolveImplicitArrayAccessSetMethod((CjArrayAccessExpression) leftDeparentized, rightOperand, contextForResolve, context.trace);
//        }
        CangJieTypeInfo rightInfo = facade.getTypeInfo(rightOperand, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));


        BindingContext bindingContext = context.trace.getBindingContext();
        CangJieType leftType = leftInfo.getType();

        CangJieType expectedType =
                refineTypeFromPropertySetterIfPossible(bindingContext, leftOperand, leftType);

        Ref<Boolean> hasErrorsOnTypeChecking = Ref.create(false);
        components.dataFlowAnalyzer.checkType(
                binaryOperationType,
                expression,
                context.replaceExpectedType(expectedType)
                        .replaceDataFlowInfo(rightInfo.getDataFlowInfo())
                        .replaceCallPosition(new CallPosition.VariableAssignment(leftDeparentized, false)),
                hasErrorsOnTypeChecking,
                true
        );
        basic.checkLValue(context.trace, context, leftOperand, rightOperand, expression, false);


        checkPropertyInTypeWithWarnings(
                context, expression, binaryOperationType, rightInfo.getDataFlowInfo(), leftOperand, leftType, expectedType
        );


        return !hasErrorsOnTypeChecking.get() ? rightInfo : null;
    }

    @Override
    public CangJieTypeInfo visitWhileExpression(@NotNull CjWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitWhileExpression(expression, context, true);
    }

    @Override
    public CangJieTypeInfo visitDoWhileExpression(@NotNull CjDoWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitDoWhileExpression(expression, context, true);
    }

    @Override
    public CangJieTypeInfo visitForExpression(@NotNull CjForExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitForExpression(expression, context, true);
    }

    @Override
    public CangJieTypeInfo visitVariable(@NotNull CjVariable variable, ExpressionTypingContext data) {
        Pair<CangJieTypeInfo, VariableDescriptor> typeInfoAndVariableDescriptor = components.localVariableResolver.process(variable, data, scope, facade);
        scope.addVariableDescriptor(typeInfoAndVariableDescriptor.getSecond());
        return typeInfoAndVariableDescriptor.getFirst();
    }

//    @Override
//    public CangJieTypeInfo visitPatternByBinding(@NotNull CjBindingPattern element, ExpressionTypingContext data) {
////        Pair<CangJieTypeInfo, VariableDescriptor> typeInfoAndVariableDescriptor = components.localVariableResolver.process(element, data, scope, facade);
////        scope.addVariableDescriptor(typeInfoAndVariableDescriptor.getSecond());
////        return typeInfoAndVariableDescriptor.getFirst();
//
//        VariableDescriptor variableDescriptor = data.trace.get(VARIABLE, element);
//
//
//        if (variableDescriptor == null) {
//            return TypeInfoFactoryKt.noTypeInfo(data);
//        } else {
//            scope.addVariableDescriptor(variableDescriptor);
//            return TypeInfoFactoryKt.createTypeInfo(variableDescriptor.getType());
//        }
//
//    }


}
