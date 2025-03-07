/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.calls;


import cn.cangnova.cangjie.psi.Call;
import cn.cangnova.cangjie.psi.CjElement;
import cn.cangnova.cangjie.psi.CjExpression;
import cn.cangnova.cangjie.resolve.calls.util.DelegatingCall;
import cn.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver;
import cn.cangnova.cangjie.resolve.scopes.receivers.Receiver;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CallTransformer {
    private CallTransformer() {
    }

    public static Call stripCallArguments(@NotNull Call call) {
        return new DelegatingCall(call) {
//            @Override
//            public CjValueArgumentList getValueArgumentList() {
//                return null;
//            }
//
//            @NotNull
//            @Override
//            public List<? extends ValueArgument> getValueArguments() {
//                return Collections.emptyList();
//            }
//
//            @NotNull
//            @Override
//            public List<LambdaArgument> getFunctionLiteralArguments() {
//                return Collections.emptyList();
//            }
//
//            @NotNull
//            @Override
//            public List<CjTypeProjection> getTypeArguments() {
//                return Collections.emptyList();
//            }
//
//            @Override
//            public CjTypeArgumentList getTypeArgumentList() {
//                return null;
//            }

            @NotNull
            @Override
            public CjElement getCallElement() {
                CjExpression calleeExpression = getCalleeExpression();
                assert calleeExpression != null : "No callee expression: " + getCallElement().getText();

                return calleeExpression;
            }
        };
    }

    public static Call stripReceiver(@NotNull Call variableCall) {
        return new DelegatingCall(variableCall) {
//            @Nullable
//            @Override
//            public ASTNode getCallOperationNode() {
//                return null;
//            }
//
//            @Nullable
//            @Override
//            public ReceiverValue getExplicitReceiver() {
//                return null;
//            }
        };
    }

    public static class CallForImplicitInvoke extends DelegatingCall {
        //        private final CjSimpleNameExpression fakeInvokeExpression;
        public final boolean itIsVariableAsFunctionCall;
        private final Call outerCall;
        private final Receiver explicitExtensionReceiver;
        private final ExpressionReceiver calleeExpressionAsDispatchReceiver;

        public CallForImplicitInvoke(
                @Nullable Receiver explicitExtensionReceiver,
                @NotNull ExpressionReceiver calleeExpressionAsDispatchReceiver,
                @NotNull Call call,
                boolean functionCall
        ) {
            super(call);
            this.outerCall = call;
            this.explicitExtensionReceiver = explicitExtensionReceiver;
            this.calleeExpressionAsDispatchReceiver = calleeExpressionAsDispatchReceiver;
//            this.fakeInvokeExpression =
//                    (CjSimpleNameExpression) new CjPsiFactory(call.getCallElement().getProject(), false)
//                            .createExpression(OperatorNameConventions.INVOKE.asString());
            itIsVariableAsFunctionCall = functionCall;
        }

        @Nullable
        @Override
        public ASTNode getCallOperationNode() {
            // if an explicit receiver corresponds to the implicit invoke, there is a corresponding call operation node:
            // a.b() or a?.b() (where b has an extension function type);
            // otherwise it's implicit
            return explicitExtensionReceiver != null ? super.getCallOperationNode() : null;
        }

        @Nullable
        @Override
        public Receiver getExplicitReceiver() {
            return explicitExtensionReceiver;
        }

        //
        @NotNull
        @Override
        public ExpressionReceiver getDispatchReceiver() {
            return calleeExpressionAsDispatchReceiver;
        }

//        @Override
//        public CjExpression getCalleeExpression() {
//            return fakeInvokeExpression;
//        }

        @NotNull
        @Override
        public CallType getCallType() {
            return CallType.INVOKE;
        }

        @NotNull
        public Call getOuterCall() {
            return outerCall;
        }
    }
}
