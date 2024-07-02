package com.huawei.cangjie.resolve.calls;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.VariableDescriptor;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.psi.CjReferenceExpression;
import com.huawei.cangjie.psi.CjSimpleNameExpression;
import com.huawei.cangjie.resolve.BindingContextUtilsKt;
import com.huawei.cangjie.resolve.TemporaryBindingTrace;
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext;
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsImpl;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy;
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategyImpl;
import com.huawei.cangjie.resolve.calls.tower.NewResolutionOldInference;
import com.huawei.cangjie.resolve.calls.util.ResolveArgumentsMode;
import com.huawei.cangjie.types.expressions.ExpressionTypingVisitorDispatcher;
import com.huawei.cangjie.utils.PerformanceCounter;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.resolve.calls.tower.PSICallResolver;
public class CallResolver {
    private static final PerformanceCounter callResolvePerfCounter = PerformanceCounter.Companion.create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter);
    private ArgumentTypeResolver argumentTypeResolver;
    private NewResolutionOldInference newResolutionOldInference;
    private PSICallResolver PSICallResolver;
    @Inject
    public void setPSICallResolver(@NotNull PSICallResolver PSICallResolver) {
        this.PSICallResolver = PSICallResolver;
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull CjReferenceExpression referenceExpression,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
        TracingStrategy tracing = TracingStrategyImpl.create(referenceExpression, context.call);
        return computeTasksAndResolveCall(context, name, tracing, kind);
    }

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull TracingStrategy tracing,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
//        return null;
        return callResolvePerfCounter.time(() -> {
            ResolutionTask<D> resolutionTask = new ResolutionTask<>(kind, name
//                    , null
            );
            return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
        });
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        tracing.bindCall(context.trace, call);


//        boolean newInferenceEnabled = languageVersionSettings.supportsFeature(LanguageFeature.NewInference);
        NewResolutionOldInference.ResolutionKind resolutionKind = resolutionTask.resolutionKind;
        if (
//                newInferenceEnabled &&
                PSICallResolver.getDefaultResolutionKinds().contains(resolutionKind)) {
            assert resolutionTask.name != null;
            BindingContextUtilsKt.recordScope(context.trace, context.scope, context.call.getCalleeExpression());
            return PSICallResolver.runResolutionAndInference(context, resolutionTask.name, resolutionKind, tracing);
        }

        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call);

        BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);

        OverloadResolutionResultsImpl<D> results = doResolveCall(newContext, resolutionTask, tracing);

        return results;

    }

    // component dependency cycle
    @Inject
    public void setResolutionOldInference(@NotNull NewResolutionOldInference newResolutionOldInference) {
        this.newResolutionOldInference = newResolutionOldInference;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> doResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
//        DataFlowInfo initialInfo = context.dataFlowInfoForArguments.getResultInfo();
//        if (context.checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
//            argumentTypeResolver.analyzeArgumentsAndRecordTypes(context, ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS);
//        }

//        List<CjTypeProjection> typeArguments = context.call.getTypeArguments();
//        for (CjTypeProjection projection : typeArguments) {
//            if (projection.getProjectionKind() != CjProjectionKind.NONE) {
//                context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
//                ModifierCheckerCore.INSTANCE.check(projection, context.trace, null, languageVersionSettings);
//            }
//            CangJieType type = argumentTypeResolver.resolveTypeRefWithDefault(
//                    projection.getTypeReference(), context.scope, context.trace,
//                    null);
//            if (type != null) {
//                ForceResolveUtil.forceResolveAllContents(type);
//            }
//        }

        OverloadResolutionResultsImpl<D> result;
//        if (!(resolutionTask.resolutionKind instanceof NewResolutionOldInference.ResolutionKind.GivenCandidates)) {
//            assert resolutionTask.name != null;
        result = newResolutionOldInference.runResolution(context, resolutionTask.name, resolutionTask.resolutionKind, tracing);
//        }
//        else {
//            assert resolutionTask.givenCandidates != null;
//            result = newResolutionOldInference.runResolutionForGivenCandidates(context, tracing, resolutionTask.givenCandidates);
//        }

        // in code like
        //   assert(a!!.isEmpty())
        //   a.length
        // we should ignore data flow info from assert argument, since assertions can be disabled and
        // thus it will lead to NPE in runtime otherwise
//        if (languageVersionSettings.getFlag(AnalysisFlags.getIgnoreDataFlowInAssert()) && result.isSingleResult()) {
//            D descriptor = result.getResultingDescriptor();
//            if (descriptor.getName().equals(Name.identifier("assert"))) {
//                DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
//                if (declaration instanceof PackageFragmentDescriptor &&
//                        ((PackageFragmentDescriptor) declaration).getFqName().asString().equals("kotlin")) {
//                    context.dataFlowInfoForArguments.updateInfo(context.call.getValueArguments().get(0), initialInfo);
//                }
//            }
//        }
        return result;
    }

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicCallResolutionContext context) {
        CjExpression calleeExpression = context.call.getCalleeExpression();
        assert calleeExpression instanceof CjSimpleNameExpression;
        CjSimpleNameExpression nameExpression = (CjSimpleNameExpression) calleeExpression;
        Name referencedName = nameExpression.getReferencedNameAsName();
        return computeTasksAndResolveCall(
                context, referencedName, nameExpression,
                NewResolutionOldInference.ResolutionKind.Variable.INSTANCE);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class ResolutionTask<D extends CallableDescriptor> {

        @Nullable
        final Name name;

//        @Nullable
//        final Collection<OldResolutionCandidate<D>> givenCandidates;

        @NotNull
        final NewResolutionOldInference.ResolutionKind resolutionKind;

        private ResolutionTask(
                @NotNull NewResolutionOldInference.ResolutionKind kind,
                @Nullable Name name
//                ,
//                @Nullable Collection<OldResolutionCandidate<D>> candidates
        ) {
            this.name = name;
//            givenCandidates = candidates;
            resolutionKind = kind;
        }
    }
}
