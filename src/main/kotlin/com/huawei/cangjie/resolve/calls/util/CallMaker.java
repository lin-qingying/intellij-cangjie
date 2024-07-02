package com.huawei.cangjie.resolve.calls.util;

import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.scopes.receivers.Receiver;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.utils.slicedMap.BasicWritableSlice;
import com.huawei.cangjie.utils.slicedMap.WritableSlice;
import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CallMaker {
    @NotNull
    public static ValueArgument makeExternalValueArgument(@NotNull CjExpression expression) {
        return new ExpressionValueArgument(expression, expression, true);
    }

    @NotNull
    public static Call makeCallWithExpressions(
            @NotNull CjElement callElement, @Nullable Receiver explicitReceiver,
            @Nullable ASTNode callOperationNode, @NotNull CjExpression calleeExpression,
            @NotNull List<CjExpression> argumentExpressions, @NotNull Call.CallType callType,
            boolean isSemanticallyEquivalentToSafeCall
    ) {
        List<ValueArgument> arguments;
        if (argumentExpressions.isEmpty()) {
            arguments = Collections.emptyList();
        } else {
            arguments = new ArrayList<>(argumentExpressions.size());
            for (CjExpression argumentExpression : argumentExpressions) {
                arguments.add(makeValueArgument(argumentExpression, calleeExpression));
            }
        }
        return makeCall(
                callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType, isSemanticallyEquivalentToSafeCall
        );
    }

    @NotNull
    public static ValueArgument makeValueArgument(@NotNull CjExpression expression) {
        return makeValueArgument(expression, expression);
    }

    @NotNull
    public static ValueArgument makeValueArgument(@Nullable CjExpression expression, @NotNull CjElement reportErrorsOn) {
        return new ExpressionValueArgument(expression, reportErrorsOn, false);
    }

    @NotNull
    public static Call makeCall(CjElement callElement, @Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode, CjExpression calleeExpression, List<? extends ValueArgument> arguments) {
        return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, Call.CallType.DEFAULT);
    }

    @NotNull
    public static Call makeCall(
            CjElement callElement, @Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode,
            CjExpression calleeExpression, List<? extends ValueArgument> arguments, Call.CallType callType
    ) {
        return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType, false);
    }

    @NotNull
    public static Call makeCall(
            CjElement callElement,
            @Nullable Receiver explicitReceiver,
            @Nullable ASTNode callOperationNode,
            CjExpression calleeExpression,
            List<? extends ValueArgument> arguments,
            Call.CallType callType,
            boolean isSemanticallyEquivalentToSafeCall
    ) {
        return new CallImpl(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType, isSemanticallyEquivalentToSafeCall);
    }

    @NotNull
    public static Call makeCallWithExpressions(@NotNull CjElement callElement, @Nullable Receiver explicitReceiver,
                                               @Nullable ASTNode callOperationNode, @NotNull CjExpression calleeExpression,
                                               @NotNull List<CjExpression> argumentExpressions) {
        return makeCallWithExpressions(callElement, explicitReceiver, callOperationNode, calleeExpression, argumentExpressions, Call.CallType.DEFAULT,
                false);
    }

    @NotNull
    public static Call makePropertyCall(@Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull CjSimpleNameExpression nameExpression) {
        return makeCallWithExpressions(nameExpression, explicitReceiver, callOperationNode, nameExpression, Collections.emptyList());
    }

    private static class ExpressionValueArgument implements ValueArgument {

        private final CjExpression expression;

        private final CjElement reportErrorsOn;

        private final boolean isExternal;

        private ExpressionValueArgument(
                @Nullable CjExpression expression,
                @NotNull CjElement reportErrorsOn,
                boolean isExternal
        ) {
            this.expression = expression;
            this.reportErrorsOn = expression == null ? reportErrorsOn : expression;
            this.isExternal = isExternal;
        }

        @Override
        public boolean isExternal() {
            return isExternal;
        }

        @Override
        public CjExpression getArgumentExpression() {
            return expression;
        }

        @Override
        public ValueArgumentName getArgumentName() {
            return null;
        }

        @Override
        public boolean isNamed() {
            return false;
        }

        @NotNull
        @Override
        public CjElement asElement() {
            return reportErrorsOn;
        }

        @Override
        public LeafPsiElement getSpreadElement() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExpressionValueArgument argument = (ExpressionValueArgument) o;

            return Objects.equals(expression, argument.expression);
        }

        @Override
        public int hashCode() {
            return expression != null ? expression.hashCode() : 0;
        }
    }

    private  static class CallImpl implements Call {

        private final CjElement callElement;
        private final Receiver explicitReceiver;
        private final ASTNode callOperationNode;
        private final CjExpression calleeExpression;
        private final List<? extends ValueArgument> valueArguments;
        private final Call.CallType callType;
        private final boolean isSemanticallyEquivalentToSafeCall;

        protected CallImpl(
                @NotNull CjElement callElement,
                @NotNull Receiver explicitReceiver,
                @Nullable ASTNode callOperationNode,
                @Nullable CjExpression calleeExpression,
                @NotNull List<? extends ValueArgument> valueArguments
        ) {
            this(callElement, explicitReceiver, callOperationNode, calleeExpression, valueArguments, CallType.DEFAULT, false);
        }

        protected CallImpl(
                @NotNull CjElement callElement,
                @Nullable Receiver explicitReceiver,
                @Nullable ASTNode callOperationNode,
                @Nullable CjExpression calleeExpression,
                @NotNull List<? extends ValueArgument> valueArguments,
                @NotNull CallType callType,
                boolean isSemanticallyEquivalentToSafeCall
        ) {
            this.callElement = callElement;
            this.explicitReceiver = explicitReceiver;
            this.callOperationNode = callOperationNode;
            this.calleeExpression = calleeExpression;
            this.valueArguments = valueArguments;
            this.callType = callType;
            this.isSemanticallyEquivalentToSafeCall = isSemanticallyEquivalentToSafeCall;
        }

//        @Override
//        public ASTNode getCallOperationNode() {
//            return callOperationNode;
//        }

        @Override
        public boolean isSemanticallyEquivalentToSafeCall() {
            return isSemanticallyEquivalentToSafeCall || Call.super.isSemanticallyEquivalentToSafeCall();
        }


                @Nullable
        @Override
        public Receiver getExplicitReceiver() {
            return explicitReceiver;
        }
//
        @Nullable
        @Override
        public ReceiverValue getDispatchReceiver() {
            return null;
        }

        @Override
        public CjExpression getCalleeExpression() {
            return calleeExpression;
        }


        @NotNull
        @Override
        public List<? extends ValueArgument> getValueArguments() {
            return valueArguments;
        }

        @NotNull
        @Override
        public CjElement getCallElement() {
            return callElement;
        }

//        @Override
//        public CjValueArgumentList getValueArgumentList() {
//            return null;
//        }

        @NotNull
        @Override
        public List<LambdaArgument> getFunctionLiteralArguments() {
            return Collections.emptyList();
        }
        @NotNull
        @Override
        public List<CjTypeProjection> getTypeArguments() {
            return Collections.emptyList();
        }

//        @Override
//        public CjTypeArgumentList getTypeArgumentList() {
//            return null;
//        }

        @Override
        public String toString() {
            return getCallElement().getText();
        }

        @NotNull
        @Override
        public CallType getCallType() {
            return callType;
        }
    }
}
