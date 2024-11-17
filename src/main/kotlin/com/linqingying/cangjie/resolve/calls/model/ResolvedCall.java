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

package com.linqingying.cangjie.resolve.calls.model;

import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor;
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor;
import com.linqingying.cangjie.psi.Call;
import com.linqingying.cangjie.psi.ValueArgument;
import com.linqingying.cangjie.resolve.calls.inference.model.ResolvedValueArgument;
import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus;
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind;
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.linqingying.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface ResolvedCall<D extends CallableDescriptor> {
    /**
     * A target callable descriptor as it was accessible in the corresponding scope, i.e. with type arguments not substituted.
     * <p>
     * Note that <b>only type parameters from the declaration itself (i.e. declared type parameters) remain unsubstituted</b>.
     * <p>
     * Type parameters from the other declarations (e.g. a containing class' type parameters) are <b>substituted</b> just as in
     * {@link  ResolvedCall#getResultingDescriptor()}.
     */
    @NotNull
    D getCandidateDescriptor();

    /**
     * Values (arguments) for value parameters indexed by parameter index
     */
    @Nullable
    List<ResolvedValueArgument> getValueArgumentsByIndex();

    @NotNull
    Call getCall();

    @Nullable
    CangJieType getSmartCastDispatchReceiverType();

    /**
     * Determines whether receiver argument or this object is substituted for explicit receiver
     */
    @NotNull
    ExplicitReceiverKind getExplicitReceiverKind();

    /**
     * The result of mapping the value argument to a parameter
     */
    @NotNull
    ArgumentMapping getArgumentMapping(@NotNull ValueArgument valueArgument);

    /**
     * Values (arguments) for value parameters
     */
    @NotNull
    Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments();

    /**
     * If the target was an extension function or property, this is the value for its receiver parameter
     */
    @Nullable
    ReceiverValue getExtensionReceiver();

    @NotNull
    ResolutionStatus getStatus();

    /**
     * If the target was a function or property with context receivers, this is the value for its context receiver parameters
     */
    @NotNull
    List<ReceiverValue> getContextReceivers();

    /**
     * Data flow info for each argument and the result data flow info
     */
    @NotNull
    DataFlowInfoForArguments getDataFlowInfoForArguments();

    /**
     * What's substituted for type parameters
     */
    @NotNull
    Map<TypeParameterDescriptor, CangJieType> getTypeArguments();

    /**
     * If the target was a member of a class, this is the object of that class to call it on
     */
    @Nullable
    ReceiverValue getDispatchReceiver();
    /** If the target was an extension function or property, this is the value for its receiver parameter */
//    @Nullable
//    ReceiverValue getExtensionReceiver();

    /**
     * A target callable descriptor with all type arguments substituted.
     * <p>
     * The resulting descriptor must not have any unsubstituted type. However, the descriptor's
     * {@link CallableDescriptor#getTypeParameters()} are unchanged and still refer to the declaration's declared type parameters.
     *
     * @see ResolvedCall#getTypeArguments()
     */
    @NotNull
    D getResultingDescriptor();
}
