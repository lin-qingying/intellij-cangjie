package com.huawei.cangjie.resolve.calls.results;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.config.LanguageFeature;
import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.resolve.calls.context.CallResolutionContext;
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode;
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy;
import com.huawei.cangjie.resolve.calls.tower.TowerUtilsKt;
import com.huawei.cangjie.resolve.calls.util.CallUtilKt;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import com.huawei.cangjie.utils.CancellationChecker;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.huawei.cangjie.analyzer.CjAnalysisSessionKt.createOverloadingConflictResolver;
import static com.huawei.cangjie.resolve.calls.results.ResolutionStatus.*;

public class ResolutionResultsHandler {

    private final OverloadingConflictResolver<ResolvedCall<?>> overloadingConflictResolver;

    public ResolutionResultsHandler(
            @NotNull CangJieBuiltIns builtIns,
            @NotNull ModuleDescriptor module,
            @NotNull TypeSpecificityComparator specificityComparator,
            @NotNull PlatformOverloadsSpecificityComparator platformOverloadsSpecificityComparator,
            @NotNull CancellationChecker cancellationChecker,
            @NotNull CangJieTypeRefiner kotlinTypeRefiner
    ) {
        overloadingConflictResolver = createOverloadingConflictResolver(
                builtIns, module, specificityComparator, platformOverloadsSpecificityComparator, cancellationChecker, kotlinTypeRefiner
        );
    }

    @NotNull
    private static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> recordFailedInfo(
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull Collection<MutableResolvedCall<D>> candidates
    ) {
        if (candidates.size() == 1) {
            MutableResolvedCall<D> failed = candidates.iterator().next();
            failed.getTrace().moveAllMyDataTo(trace);
            return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
        }
        tracing.noneApplicable(trace, candidates);
        tracing.recordAmbiguity(trace, candidates);
        return OverloadResolutionResultsImpl.manyFailedCandidates(candidates);
    }

    private static <D extends CallableDescriptor> boolean allIncomplete(@NotNull Collection<MutableResolvedCall<D>> results) {
        for (MutableResolvedCall<D> result : results) {
            if (result.getStatus() != INCOMPLETE_TYPE_INFERENCE) return false;
        }
        return true;
    }

    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            @NotNull CallResolutionContext context,
            @NotNull TracingStrategy tracing,
            @NotNull Collection<MutableResolvedCall<D>> candidates,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        Set<MutableResolvedCall<D>> successfulCandidates = new LinkedHashSet<>();
        Set<MutableResolvedCall<D>> failedCandidates = new LinkedHashSet<>();
        Set<MutableResolvedCall<D>> incompleteCandidates = new LinkedHashSet<>();
        Set<MutableResolvedCall<D>> candidatesWithWrongReceiver = new LinkedHashSet<>();
        for (MutableResolvedCall<D> candidateCall : candidates) {
            ResolutionStatus status = candidateCall.getStatus();
            assert status != UNKNOWN_STATUS : "No resolution for " + candidateCall.getCandidateDescriptor();
            if (status.isSuccess()) {
                successfulCandidates.add(candidateCall);
            } else if (status == INCOMPLETE_TYPE_INFERENCE) {
                incompleteCandidates.add(candidateCall);
            } else if (candidateCall.getStatus() == RECEIVER_TYPE_ERROR) {
                candidatesWithWrongReceiver.add(candidateCall);
            } else if (candidateCall.getStatus() != RECEIVER_PRESENCE_ERROR) {
                failedCandidates.add(candidateCall);
            }
        }
        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific

        if (!successfulCandidates.isEmpty() || !incompleteCandidates.isEmpty()) {
            return computeSuccessfulResult(
                    context, tracing, successfulCandidates, incompleteCandidates, context.checkArguments, languageVersionSettings);
        } else if (!failedCandidates.isEmpty()) {
            return computeFailedResult(tracing, context.trace, failedCandidates, context.checkArguments, languageVersionSettings);
        }
        if (!candidatesWithWrongReceiver.isEmpty()) {
            tracing.unresolvedReferenceWrongReceiver(context.trace, candidatesWithWrongReceiver);
            return OverloadResolutionResultsImpl.candidatesWithWrongReceiver(candidatesWithWrongReceiver);
        }
        tracing.unresolvedReference(context.trace);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeSuccessfulResult(
            @NotNull CallResolutionContext<?> context,
            @NotNull TracingStrategy tracing,
            @NotNull Set<MutableResolvedCall<D>> successfulCandidates,
            @NotNull Set<MutableResolvedCall<D>> incompleteCandidates,
            @NotNull CheckArgumentTypesMode checkArgumentsMode,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        Set<MutableResolvedCall<D>> successfulAndIncomplete = new LinkedHashSet<>();
        successfulAndIncomplete.addAll(successfulCandidates);
        successfulAndIncomplete.addAll(incompleteCandidates);
        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(
                successfulAndIncomplete, true, checkArgumentsMode, languageVersionSettings);
        if (results.isSingleResult()) {
            MutableResolvedCall<D> resultingCall = results.getResultingCall();
            resultingCall.getTrace().moveAllMyDataTo(context.trace);
            if (resultingCall.getStatus() == INCOMPLETE_TYPE_INFERENCE) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(resultingCall);
            }
        }
        if (results.isAmbiguity()) {
            tracing.recordAmbiguity(context.trace, results.getResultingCalls());
            boolean allCandidatesIncomplete = allIncomplete(results.getResultingCalls());
            // This check is needed for the following case:
            //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
            if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ||
                    !CallUtilKt.hasUnresolvedArguments(context.call, context)) {
                if (allCandidatesIncomplete) {
                    tracing.cannotCompleteResolve(context.trace, results.getResultingCalls());
                } else {
                    tracing.ambiguity(context.trace, results.getResultingCalls());
                }
            }
            if (allCandidatesIncomplete) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(results.getResultingCalls());
            }
        }
        return results;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeFailedResult(
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull Set<MutableResolvedCall<D>> failedCandidates,
            @NotNull CheckArgumentTypesMode checkArgumentsMode,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        if (failedCandidates.size() == 1) {
            return recordFailedInfo(tracing, trace, failedCandidates);
        }

        for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
            Set<MutableResolvedCall<D>> thisLevel = new LinkedHashSet<>();
            for (MutableResolvedCall<D> candidate : failedCandidates) {
                if (severityLevel.contains(candidate.getStatus())) {
                    thisLevel.add(candidate);
                }
            }
            if (!thisLevel.isEmpty()) {
                if (severityLevel.contains(ARGUMENTS_MAPPING_ERROR)) {
                    @SuppressWarnings("unchecked")
                    OverloadingConflictResolver<MutableResolvedCall<D>> myResolver = (OverloadingConflictResolver) overloadingConflictResolver;
                    return recordFailedInfo(tracing, trace, myResolver.filterOutEquivalentCalls(new LinkedHashSet<>(thisLevel)));
                }
                OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(
                        thisLevel, false, checkArgumentsMode, languageVersionSettings);
                return recordFailedInfo(tracing, trace, results.getResultingCalls());
            }
        }

        throw new AssertionError("Should not be reachable, cause every status must belong to some level: " + failedCandidates);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(
            @NotNull Set<MutableResolvedCall<D>> candidates,
            boolean discriminateGenerics,
            @NotNull CheckArgumentTypesMode checkArgumentsMode,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        OverloadingConflictResolver<MutableResolvedCall<D>> myResolver = (OverloadingConflictResolver) overloadingConflictResolver;

        Set<MutableResolvedCall<D>> refinedCandidates = candidates;
        if (!languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority)) {
            Set<MutableResolvedCall<D>> nonSynthesized = new HashSet<>();
            for (MutableResolvedCall<D> candidate : candidates) {
                if (!TowerUtilsKt.isSynthesized(candidate.getCandidateDescriptor())) {
                    nonSynthesized.add(candidate);
                }
            }

            if (!nonSynthesized.isEmpty()) {
                refinedCandidates = nonSynthesized;
            }
        }

        Set<MutableResolvedCall<D>> specificCalls =
                myResolver.chooseMaximallySpecificCandidates(refinedCandidates, checkArgumentsMode, discriminateGenerics);

        if (specificCalls.size() > 1) {
            specificCalls = specificCalls.stream()
                    .filter((call) ->
                                    false
//                            !call.getCandidateDescriptor().getAnnotations().hasAnnotation(
//                                    AnnotationsForResolveKt.getOVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME())
                    ).collect(Collectors.toSet());
        }

        if (specificCalls.size() == 1) {
            return OverloadResolutionResultsImpl.success(specificCalls.iterator().next());
        } else {
            return OverloadResolutionResultsImpl.ambiguity(specificCalls);
        }
    }
}
