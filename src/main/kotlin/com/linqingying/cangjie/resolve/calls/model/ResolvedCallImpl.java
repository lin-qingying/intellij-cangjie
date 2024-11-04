package com.linqingying.cangjie.resolve.calls.model;


import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor;
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor;
import com.linqingying.cangjie.psi.Call;
import com.linqingying.cangjie.psi.ValueArgument;
import com.linqingying.cangjie.resolve.DelegatingBindingTrace;
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystem;
import com.linqingying.cangjie.resolve.calls.inference.model.ResolvedValueArgument;
import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus;
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind;
import com.linqingying.cangjie.resolve.calls.tasks.OldResolutionCandidate;
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy;
import com.linqingying.cangjie.resolve.calls.util.CallResolverUtilKt;
import com.linqingying.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.linqingying.cangjie.resolve.scopes.receivers.*;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.TypeProjection;
import com.linqingying.cangjie.types.TypeSubstitutor;
import com.linqingying.cangjie.types.Variance;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.SmartList;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.linqingying.cangjie.resolve.calls.results.ResolutionStatus.INCOMPLETE_TYPE_INFERENCE;
import static com.linqingying.cangjie.resolve.calls.results.ResolutionStatus.UNKNOWN_STATUS;

public class ResolvedCallImpl<D extends CallableDescriptor> implements MutableResolvedCall<D> {
    private static final Logger LOG = Logger.getInstance(ResolvedCallImpl.class);
    private final Call call;
    private final D candidateDescriptor;
    private final ExplicitReceiverKind explicitReceiverKind;
    private final TypeSubstitutor knownTypeParametersSubstitutor;
    @NotNull
    private final Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments;
    private final MutableDataFlowInfoForArguments dataFlowInfoForArguments;
    @NotNull
    private final Map<ValueArgument, ArgumentMatchImpl> argumentToParameterMap;
    private D resultingDescriptor; // Probably substituted
    private ReceiverValue dispatchReceiver; // receiver object of a method
    private ReceiverValue extensionReceiver; // receiver of an extension function
    @NotNull
    private Map<TypeParameterDescriptor, CangJieType> typeArguments;
    private DelegatingBindingTrace trace;
    private TracingStrategy tracing;
    private ResolutionStatus status = UNKNOWN_STATUS;
    private ConstraintSystem constraintSystem = null;
    private Boolean hasInferredReturnType = null;
    private boolean completed = false;
    private CangJieType smartCastDispatchReceiverType = null;
    private Queue<Function0<Unit>> remainingTasks = null;

    private ResolvedCallImpl(
            @NotNull OldResolutionCandidate<D> candidate,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        this.call = candidate.getCall();
        this.candidateDescriptor = candidate.getDescriptor();
        this.dispatchReceiver = candidate.getDispatchReceiver();
        this.extensionReceiver = null; // ResolutionCandidate can have only dispatch receiver
        this.explicitReceiverKind = candidate.getExplicitReceiverKind();
        this.knownTypeParametersSubstitutor = candidate.getKnownTypeParametersResultingSubstitutor();
        this.trace = trace;
        this.tracing = tracing;
        this.dataFlowInfoForArguments = dataFlowInfoForArguments;
        this.typeArguments = createTypeArgumentsMap(candidateDescriptor);
        this.valueArguments = createValueArgumentsMap(candidateDescriptor);
        this.argumentToParameterMap = createArgumentsToParameterMap(candidateDescriptor);
    }

    public ResolvedCallImpl(
            @NotNull Call call,
            @NotNull D candidateDescriptor,
            @Nullable ReceiverValue dispatchReceiver,
            @Nullable ReceiverValue extensionReceiver,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @Nullable TypeSubstitutor knownTypeParametersSubstitutor,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        this.call = call;
        this.candidateDescriptor = candidateDescriptor;
        this.dispatchReceiver = dispatchReceiver;
        this.extensionReceiver = extensionReceiver;
        this.explicitReceiverKind = explicitReceiverKind;
        this.knownTypeParametersSubstitutor = knownTypeParametersSubstitutor;
        this.trace = trace;
        this.tracing = tracing;
        this.dataFlowInfoForArguments = dataFlowInfoForArguments;
        this.typeArguments = createTypeArgumentsMap(candidateDescriptor);
        this.valueArguments = createValueArgumentsMap(candidateDescriptor);
        this.argumentToParameterMap = createArgumentsToParameterMap(candidateDescriptor);
    }

    @NotNull
    public static <D extends CallableDescriptor> ResolvedCallImpl<D> create(
            @NotNull OldResolutionCandidate<D> candidate,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new ResolvedCallImpl<>(candidate, trace, tracing, dataFlowInfoForArguments);
    }

    @NotNull
    private static Map<ValueParameterDescriptor, ResolvedValueArgument> createValueArgumentsMap(CallableDescriptor descriptor) {
        return descriptor.getValueParameters().isEmpty() ? Collections.emptyMap() : new LinkedHashMap<>();
    }

    @NotNull
    private static Map<ValueArgument, ArgumentMatchImpl> createArgumentsToParameterMap(CallableDescriptor descriptor) {
        return descriptor.getValueParameters().isEmpty() ? Collections.emptyMap() : new HashMap<>();
    }

    @NotNull
    private static Map<TypeParameterDescriptor, CangJieType> createTypeArgumentsMap(CallableDescriptor descriptor) {
        return descriptor.getTypeParameters().isEmpty() ? Collections.emptyMap() : new LinkedHashMap<>();
    }

    @Override
    @NotNull
    public ResolutionStatus getStatus() {
        return status;
    }

    @Override
    public void addStatus(@NotNull ResolutionStatus status) {
        this.status = this.status.combine(status);
    }

    @Override
    public void setStatusToSuccess() {
        assert status == INCOMPLETE_TYPE_INFERENCE || status == UNKNOWN_STATUS;
        status = ResolutionStatus.SUCCESS;
    }

    @Override
    @NotNull
    public DelegatingBindingTrace getTrace() {
        assertNotCompleted("Trace");
        return trace;
    }

    @Override
    @NotNull
    public TracingStrategy getTracingStrategy() {
        assertNotCompleted("TracingStrategy");
        return tracing;
    }

    @NotNull
    @Override
    public Call getCall() {
        return call;
    }

    @Override
    @NotNull
    public D getCandidateDescriptor() {
        return candidateDescriptor;
    }

    @Override
    @NotNull
    public D getResultingDescriptor() {
        return resultingDescriptor == null ? candidateDescriptor : resultingDescriptor;
    }

    @SuppressWarnings("unchecked")
    public void setResultingSubstitutor(@NotNull TypeSubstitutor substitutor) {
        D descriptorToSubstitute = resultingDescriptor != null && DescriptorUtilsKt.shouldBeSubstituteWithStubTypes(resultingDescriptor)
                ? resultingDescriptor
                : candidateDescriptor;
        resultingDescriptor = (D) descriptorToSubstitute.substitute(substitutor);
    }

    public void setResolvedCallSubstitutor(@NotNull TypeSubstitutor substitutor) {
        for (TypeParameterDescriptor typeParameter : candidateDescriptor.getTypeParameters()) {
            TypeProjection typeArgumentProjection = substitutor.getSubstitution().get(typeParameter.getDefaultType());
            if (typeArgumentProjection != null) {
                typeArguments.put(typeParameter, typeArgumentProjection.getType());
            }
        }

        typeArguments = typeArguments.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> substitutor.safeSubstitute(e.getValue(), e.getKey().getVariance())));

        if (dispatchReceiver instanceof ExpressionReceiver) {
            dispatchReceiver = dispatchReceiver.replaceType(substitutor.safeSubstitute(dispatchReceiver.getType(), Variance.INVARIANT));
        }
        if (extensionReceiver instanceof ExtensionReceiver) {
            extensionReceiver =
                    extensionReceiver.replaceType(substitutor.safeSubstitute(extensionReceiver.getType(), Variance.INVARIANT));
        }

        if (candidateDescriptor.getValueParameters().isEmpty()) return;

        List<ValueParameterDescriptor> substitutedParameters = resultingDescriptor.getValueParameters();

        Collection<Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>> valueArgumentsBeforeSubstitution =
                new SmartList<>(valueArguments.entrySet());

        valueArguments.clear();

        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArgumentsBeforeSubstitution) {
            ValueParameterDescriptor substitutedVersion = substitutedParameters.get(entry.getKey().getIndex());
            assert substitutedVersion != null : entry.getKey();
            valueArguments.put(substitutedVersion, entry.getValue());
        }

        Collection<Map.Entry<ValueArgument, ArgumentMatchImpl>> unsubstitutedArgumentMappings =
                new SmartList<>(argumentToParameterMap.entrySet());

        argumentToParameterMap.clear();
        for (Map.Entry<ValueArgument, ArgumentMatchImpl> entry : unsubstitutedArgumentMappings) {
            ArgumentMatchImpl argumentMatch = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = argumentMatch.getValueParameter();
            ValueParameterDescriptor substitutedVersion = substitutedParameters.get(valueParameterDescriptor.getIndex());
            assert substitutedVersion != null : valueParameterDescriptor;
            argumentToParameterMap.put(entry.getKey(), argumentMatch.replaceValueParameter(substitutedVersion));
        }
    }

    @Override
    public void setSubstitutor(@NotNull TypeSubstitutor substitutor) {
        setResultingSubstitutor(substitutor);
        setResolvedCallSubstitutor(substitutor);
    }

    @Nullable
    @Override
    public ConstraintSystem getConstraintSystem() {
        assertNotCompleted("ConstraintSystem");
        return constraintSystem;
    }

    @Override
    public void setConstraintSystem(@NotNull ConstraintSystem constraintSystem) {
        this.constraintSystem = constraintSystem;
    }

    @Override
    public void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument) {
        assert !valueArguments.containsKey(valueParameter) : valueParameter + " -> " + valueArgument;
        valueArguments.put(valueParameter, valueArgument);
        for (ValueArgument argument : valueArgument.getArguments()) {
            argumentToParameterMap.put(argument, new ArgumentMatchImpl(valueParameter));
        }
    }

    @Override
    @Nullable
    public ReceiverValue getExtensionReceiver() {
        return extensionReceiver;
    }

    @Override
    @Nullable
    public ReceiverValue getDispatchReceiver() {
        return dispatchReceiver;
    }

    @NotNull
    @Override
    public List<ReceiverValue> getContextReceivers() {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return explicitReceiverKind;
    }

    @Override
    @NotNull
    public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
        return valueArguments;
    }

    @Nullable
    @Override
    public List<ResolvedValueArgument> getValueArgumentsByIndex() {
        List<ResolvedValueArgument> arguments = new ArrayList<>(candidateDescriptor.getValueParameters().size());
        for (int i = 0; i < candidateDescriptor.getValueParameters().size(); ++i) {
            arguments.add(null);
        }

        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument value = entry.getValue();
            ResolvedValueArgument oldValue = arguments.set(parameterDescriptor.getIndex(), value);
            if (oldValue != null) {
                return null;
            }
        }

        for (int i = 0; i < arguments.size(); i++) {
            Object o = arguments.get(i);
            if (o == null) {
                return null;
            }
        }

        return arguments;
    }

    @Override
    public void recordArgumentMatchStatus(@NotNull ValueArgument valueArgument, @NotNull ArgumentMatchStatus matchStatus) {
        ArgumentMatchImpl argumentMatch = argumentToParameterMap.get(valueArgument);
        argumentMatch.recordMatchStatus(matchStatus);
    }

    @NotNull
    @Override
    public ArgumentMapping getArgumentMapping(@NotNull ValueArgument valueArgument) {
        ArgumentMatch argumentMatch = argumentToParameterMap.get(valueArgument);
        if (argumentMatch == null) {
            if (ArgumentMappingKt.isReallySuccess(this)) {
                LOG.error("ArgumentUnmapped for " + valueArgument + " in successfully resolved call: " + call.getCallElement().getText());
            }
            return ArgumentUnmapped.INSTANCE;
        }
        return argumentMatch;
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, CangJieType> getTypeArguments() {
        return typeArguments;
    }

    @NotNull
    @Override
    public MutableDataFlowInfoForArguments getDataFlowInfoForArguments() {
        return dataFlowInfoForArguments;
    }

    @Override
    public boolean hasInferredReturnType() {
        if (!completed) {
            hasInferredReturnType = constraintSystem == null ||
                    CallResolverUtilKt.hasInferredReturnType(candidateDescriptor, constraintSystem);
        }
        assert hasInferredReturnType != null : "The property 'hasInferredReturnType' was not set when the call was completed.";
        return hasInferredReturnType;
    }

    @Override
    public void markCallAsCompleted() {
        if (!completed) {
            hasInferredReturnType();
        }
        trace = null;
        constraintSystem = null;
        tracing = null;
        completed = true;
        remainingTasks = null;
    }

    @Override
    public void addRemainingTasks(Function0<Unit> task) {
        if (remainingTasks == null) {
            remainingTasks = new ArrayDeque<>();
        }
        remainingTasks.add(task);
    }

    @Override
    public void performRemainingTasks() {
        if (remainingTasks == null) return;
        while (!remainingTasks.isEmpty()) {
            remainingTasks.poll().invoke();
        }
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    private void assertNotCompleted(String elementName) {
        assert !completed : elementName + " is erased after resolution completion.";
    }

    @Override
    @Nullable
    public TypeSubstitutor getKnownTypeParametersSubstitutor() {
        return knownTypeParametersSubstitutor;
    }

    @Override
    @Nullable
    public CangJieType getSmartCastDispatchReceiverType() {
        return smartCastDispatchReceiverType;
    }

    @Override
    public void setSmartCastDispatchReceiverType(@NotNull CangJieType smartCastDispatchReceiverType) {
        this.smartCastDispatchReceiverType = smartCastDispatchReceiverType;
    }

    @Override
    public void updateExtensionReceiverWithSmartCastIfNeeded(@NotNull CangJieType smartCastExtensionReceiverType) {
        if (extensionReceiver instanceof ImplicitClassReceiver) {
            extensionReceiver = new CastImplicitClassReceiver(
                    ((ImplicitClassReceiver) extensionReceiver).getClassDescriptor(),
                    smartCastExtensionReceiverType
            );
        }
    }
}
