package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;
import com.huawei.cangjie.types.checker.CangJieTypePreparator;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*;

public class OverridingUtil {


    public static final OverridingUtil DEFAULT;
    private static final List<ExternalOverridabilityCondition> EXTERNAL_CONDITIONS =
            CollectionsKt.toList(ServiceLoader.load(
                    ExternalOverridabilityCondition.class,
                    ExternalOverridabilityCondition.class.getClassLoader()
            ));
    private static final CangJieTypeChecker.TypeConstructorEquality DEFAULT_TYPE_CONSTRUCTOR_EQUALITY =
            Object::equals;

    static {
        DEFAULT = new OverridingUtil(
                DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, CangJieTypeRefiner.Default.INSTANCE, CangJieTypePreparator.Default.INSTANCE,
                null
        );
    }

    private final CangJieTypeRefiner cangjieTypeRefiner;
    private final CangJieTypePreparator cangjieTypePreparator;
    private final CangJieTypeChecker.TypeConstructorEquality equalityAxioms;
    private final Function2<CangJieType, CangJieType, Boolean> customSubtype;
    private OverridingUtil(
            @NotNull CangJieTypeChecker.TypeConstructorEquality axioms,
            @NotNull CangJieTypeRefiner cangjieTypeRefiner,
            @NotNull CangJieTypePreparator cangjieTypePreparator,
            @Nullable Function2<CangJieType, CangJieType, Boolean> customSubtype
    ) {
        equalityAxioms = axioms;
        this.cangjieTypeRefiner = cangjieTypeRefiner;
        this.cangjieTypePreparator = cangjieTypePreparator;
        this.customSubtype = customSubtype;
    }

    @NotNull
    public static OverridingUtil create(
            @NotNull CangJieTypeRefiner cangjieTypeRefiner,
            @NotNull CangJieTypeChecker.TypeConstructorEquality equalityAxioms
    ) {
        return new OverridingUtil(equalityAxioms, cangjieTypeRefiner, CangJieTypePreparator.Default.INSTANCE, null);
    }

    @NotNull
    public static OverridingUtil createWithTypeRefiner(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return new OverridingUtil(DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, cangjieTypeRefiner, CangJieTypePreparator.Default.INSTANCE, null);
    }


    /**
     * @return whether f overrides g
     */
    public static <D extends CallableDescriptor> boolean overrides(
            @NotNull D f,
            @NotNull D g,
            boolean allowDeclarationCopies,
            boolean distinguishExpectsAndNonExpects
    ) {
        // In a multi-module project different "copies" of the same class may be present in different libraries,
        // that's why we use structural equivalence for members (DescriptorEquivalenceForOverrides).

        // This first check cover the case of duplicate classes in different modules:
        // when B is defined in modules m1 and m2, and C (indirectly) inherits from both versions,
        // we'll be getting sets of members that do not override each other, but are structurally equivalent.
        // As other code relies on no equal descriptors passed here, we guard against f == g, but this may not be necessary
        // Note that this is needed for the usage of this function in the IDE code
        if (!f.equals(g)
                && DescriptorEquivalenceForOverrides.INSTANCE.areEquivalent(
                f.getOriginal(),
                g.getOriginal(),
                allowDeclarationCopies,
                distinguishExpectsAndNonExpects
        )
        ) {
            return true;
        }

        CallableDescriptor originalG = g.getOriginal();
        for (D overriddenFunction : DescriptorUtils.getAllOverriddenDescriptors(f)) {
            if (DescriptorEquivalenceForOverrides.INSTANCE.areEquivalent(
                    originalG,
                    overriddenFunction,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
            )) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static <D> Set<D> filterOverrides(
            @NotNull Set<D> candidateSet,
            boolean allowDescriptorCopies,
            @Nullable Function0<?> cancellationCallback,
            @NotNull Function2<? super D, ? super D, Pair<CallableDescriptor, CallableDescriptor>> transformFirst
    ) {
        if (candidateSet.size() <= 1) return candidateSet;

        Set<D> result = new LinkedHashSet<>();
        outerLoop:
        for (D meD : candidateSet) {
            if (cancellationCallback != null) {
                cancellationCallback.invoke();
            }
            for (Iterator<D> iterator = result.iterator(); iterator.hasNext(); ) {
                D otherD = iterator.next();
                Pair<CallableDescriptor, CallableDescriptor> meAndOther = transformFirst.invoke(meD, otherD);
                CallableDescriptor me = meAndOther.component1();
                CallableDescriptor other = meAndOther.component2();
                if (overrides(me, other, allowDescriptorCopies, true)) {
                    iterator.remove();
                } else if (overrides(other, me, allowDescriptorCopies, true)) {
                    continue outerLoop;
                }
            }
            result.add(meD);
        }

        assert !result.isEmpty() : "All candidates filtered out from " + candidateSet;

        return result;
    }

    private static boolean isVisibilityMoreSpecific(
            @NotNull DeclarationDescriptorWithVisibility a,
            @NotNull DeclarationDescriptorWithVisibility b
    ) {
        Integer result = DescriptorVisibilities.compare(a.getVisibility(), b.getVisibility());
        return result == null || result >= 0;
    }

    private static boolean isReturnTypeMoreSpecific(
            @NotNull CallableDescriptor a,
            @NotNull CangJieType aReturnType,
            @NotNull CallableDescriptor b,
            @NotNull CangJieType bReturnType,
            @NotNull TypeCheckerState typeCheckerState
    ) {
        return AbstractTypeChecker.INSTANCE.isSubtypeOf(typeCheckerState, aReturnType.unwrap(), bReturnType.unwrap());
    }

    public static boolean isMoreSpecific(@NotNull CallableDescriptor a, @NotNull CallableDescriptor b) {
        CangJieType aReturnType = a.getReturnType();
        CangJieType bReturnType = b.getReturnType();

        assert aReturnType != null : "Return type of " + a + " is null";
        assert bReturnType != null : "Return type of " + b + " is null";

        if (!isVisibilityMoreSpecific(a, b)) return false;


        TypeCheckerState checkerState =
                DEFAULT.createTypeCheckerState(a.getTypeParameters(), b.getTypeParameters());

        if (a instanceof FunctionDescriptor) {
            assert b instanceof FunctionDescriptor : "b is " + b.getClass();

            return isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checkerState);
        }
        if (a instanceof VariableDescriptor pa) {
            assert b instanceof VariableDescriptor : "b is " + b.getClass();

            VariableDescriptor pb = (VariableDescriptor) b;

//            if (!isAccessorMoreSpecific(pa.getSetter(), pb.getSetter())) return false;

            if (pa.isVar() && pb.isVar()) {
                // TODO(dsavvinov): using DEFAULT here looks suspicious
                return AbstractTypeChecker.INSTANCE.equalTypes(checkerState, aReturnType.unwrap(), bReturnType.unwrap());
            } else {
                // both vals or var vs val: val can't be more specific then var
                return !(!pa.isVar() && pb.isVar()) && isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checkerState);
            }
        }
        throw new IllegalArgumentException("Unexpected callable: " + a.getClass());
    }

    private static boolean isMoreSpecificThenAllOf(@NotNull CallableDescriptor candidate, @NotNull Collection<CallableDescriptor> descriptors) {
        // NB subtyping relation in CangJie is not transitive in presence of flexible types:
        //  String? <: String! <: String, but not String? <: String
        for (CallableDescriptor descriptor : descriptors) {
            if (!isMoreSpecific(candidate, descriptor)) {
                return false;
            }
        }
        return true;
    }

    //    private static bool isAccessorMoreSpecific(@Nullable PropertyAccessorDescriptor a, @Nullable PropertyAccessorDescriptor b) {
//        if (a == null || b == null) return true;
//        return isVisibilityMoreSpecific(a, b);
//    }
    @NotNull
    public static <H> H selectMostSpecificMember(
            @NotNull Collection<H> overridables,
            @NotNull Function1<H, CallableDescriptor> descriptorByHandle
    ) {
        assert !overridables.isEmpty() : "Should have at least one overridable descriptor";

        if (overridables.size() == 1) {
            return CollectionsKt.first(overridables);
        }

        Collection<H> candidates = new ArrayList<>(2);
        List<CallableDescriptor> callableMemberDescriptors = CollectionsKt.map(overridables, descriptorByHandle);

        H transitivelyMostSpecific = CollectionsKt.first(overridables);
        CallableDescriptor transitivelyMostSpecificDescriptor = descriptorByHandle.invoke(transitivelyMostSpecific);

        for (H overridable : overridables) {
            CallableDescriptor descriptor = descriptorByHandle.invoke(overridable);
            if (isMoreSpecificThenAllOf(descriptor, callableMemberDescriptors)) {
                candidates.add(overridable);
            }
            if (isMoreSpecific(descriptor, transitivelyMostSpecificDescriptor)
                    && !isMoreSpecific(transitivelyMostSpecificDescriptor, descriptor)) {
                transitivelyMostSpecific = overridable;
            }
        }

        if (candidates.isEmpty()) {
            return transitivelyMostSpecific;
        } else if (candidates.size() == 1) {
            return CollectionsKt.first(candidates);
        }

        H firstNonFlexible = null;
        for (H candidate : candidates) {
            //noinspection ConstantConditions
            if (!FlexibleTypesKt.isFlexible(descriptorByHandle.invoke(candidate).getReturnType())) {
                firstNonFlexible = candidate;
                break;
            }
        }
        if (firstNonFlexible != null) {
            return firstNonFlexible;
        }

        return CollectionsKt.first(candidates);
    }

    /**
     * @param <H> is something that handles CallableDescriptor inside
     * @return
     */
    @NotNull
    public static <H> Collection<H> extractMembersOverridableInBothWays(
            @NotNull H overrider,
            @NotNull /*@Mutable*/ Collection<H> extractFrom,
            @NotNull Function1<H, CallableDescriptor> descriptorByHandle,
            @NotNull Function1<H, Unit> onConflict
    ) {
        Collection<H> overridable = new ArrayList<>();
        overridable.add(overrider);
        CallableDescriptor overriderDescriptor = descriptorByHandle.invoke(overrider);
        for (Iterator<H> iterator = extractFrom.iterator(); iterator.hasNext(); ) {
            H candidate = iterator.next();
            CallableDescriptor candidateDescriptor = descriptorByHandle.invoke(candidate);
            if (overrider == candidate) {
                iterator.remove();
                continue;
            }

            OverrideCompatibilityInfo.Result finalResult = getBothWaysOverridability(overriderDescriptor, candidateDescriptor);

            if (finalResult == OVERRIDABLE) {
                overridable.add(candidate);
                iterator.remove();
            } else if (finalResult == CONFLICT) {
                onConflict.invoke(candidate);
                iterator.remove();
            }
        }
        return overridable;
    }

    private static List<CangJieType> compiledValueParameters(CallableDescriptor callableDescriptor) {
        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getExtensionReceiverParameter();
        List<CangJieType> parameters = new ArrayList<>();
        if (receiverParameter != null) {
            parameters.add(receiverParameter.getType());
        }
        for (ValueParameterDescriptor valueParameterDescriptor : callableDescriptor.getValueParameters()) {
            parameters.add(valueParameterDescriptor.getType());
        }
        return parameters;
    }

    @Nullable
    public static OverrideCompatibilityInfo getBasicOverridabilityProblem(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor
    ) {
        if (superDescriptor instanceof FunctionDescriptor && !(subDescriptor instanceof FunctionDescriptor) ||
                superDescriptor instanceof VariableDescriptor && !(subDescriptor instanceof VariableDescriptor)) {
            return OverrideCompatibilityInfo.incompatible("Member kind mismatch");
        }

        if (!(superDescriptor instanceof FunctionDescriptor) && !(superDescriptor instanceof VariableDescriptor)) {
            throw new IllegalArgumentException("This type of CallableDescriptor cannot be checked for overridability: " + superDescriptor);
        }

        // TODO: check outside of this method
        if (!superDescriptor.getName().equals(subDescriptor.getName())) {
            return OverrideCompatibilityInfo.incompatible("Name mismatch");
        }

        return checkReceiverAndParameterCount(superDescriptor, subDescriptor);
    }

    @Nullable
    private static OverrideCompatibilityInfo checkReceiverAndParameterCount(
            CallableDescriptor superDescriptor,
            CallableDescriptor subDescriptor
    ) {
        if ((superDescriptor.getExtensionReceiverParameter() == null) != (subDescriptor.getExtensionReceiverParameter() == null)) {
            return OverrideCompatibilityInfo.incompatible("Receiver presence mismatch");
        }

        if (superDescriptor.getValueParameters().size() != subDescriptor.getValueParameters().size()) {
            return OverrideCompatibilityInfo.incompatible("Value parameter number mismatch");
        }

        return null;
    }

    private static boolean areTypesEquivalent(
            @NotNull CangJieType typeInSuper,
            @NotNull CangJieType typeInSub,
            @NotNull TypeCheckerState typeCheckerState
    ) {
        boolean bothErrors = CangJieTypeKt.isError(typeInSuper) && CangJieTypeKt.isError(typeInSub);
        if (bothErrors) return true;
        return AbstractTypeChecker.INSTANCE.equalTypes(typeCheckerState, typeInSuper.unwrap(), typeInSub.unwrap());
    }

    // See JLS 8, 8.4.4 Generic Methods
    private static boolean areTypeParametersEquivalent(
            @NotNull TypeParameterDescriptor superTypeParameter,
            @NotNull TypeParameterDescriptor subTypeParameter,
            @NotNull TypeCheckerState typeCheckerState
    ) {
        List<CangJieType> superBounds = superTypeParameter.getUpperBounds();
        List<CangJieType> subBounds = new ArrayList<>(subTypeParameter.getUpperBounds());
        if (superBounds.size() != subBounds.size()) return false;

        outer:
        for (CangJieType superBound : superBounds) {
            ListIterator<CangJieType> it = subBounds.listIterator();
            while (it.hasNext()) {
                CangJieType subBound = it.next();
                if (areTypesEquivalent(superBound, subBound, typeCheckerState)) {
                    it.remove();
                    continue outer;
                }
            }
            return false;
        }

        return true;
    }

    @Nullable
    public static OverrideCompatibilityInfo.Result getBothWaysOverridability(
            CallableDescriptor overriderDescriptor,
            CallableDescriptor candidateDescriptor
    ) {
        OverrideCompatibilityInfo.Result result1 = DEFAULT.isOverridableBy(candidateDescriptor, overriderDescriptor, null).getResult();
        OverrideCompatibilityInfo.Result result2 = DEFAULT.isOverridableBy(overriderDescriptor, candidateDescriptor, null).getResult();

        return result1 == OVERRIDABLE && result2 == OVERRIDABLE
                ? OVERRIDABLE
                : ((result1 == CONFLICT || result2 == CONFLICT) ? CONFLICT : INCOMPATIBLE);
    }

    @NotNull
    private TypeCheckerState createTypeCheckerState(
            @NotNull List<TypeParameterDescriptor> firstParameters,
            @NotNull List<TypeParameterDescriptor> secondParameters
    ) {
        assert firstParameters.size() == secondParameters.size() :
                "Should be the same number of type parameters: " + firstParameters + " vs " + secondParameters;

        if (firstParameters.isEmpty()) {
            return new OverridingUtilTypeSystemContext(
                    null, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
            ).newTypeCheckerState(true, true);
        }

        Map<TypeConstructor, TypeConstructor> matchingTypeConstructors = new HashMap<>();
        for (int i = 0; i < firstParameters.size(); i++) {
            matchingTypeConstructors.put(firstParameters.get(i).getTypeConstructor(), secondParameters.get(i).getTypeConstructor());
        }

        return new OverridingUtilTypeSystemContext(
                matchingTypeConstructors, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
        ).newTypeCheckerState(true, true);
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableByWithoutExternalConditions(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            boolean checkReturnType
    ) {
        OverrideCompatibilityInfo basicOverridability = getBasicOverridabilityProblem(superDescriptor, subDescriptor);
        if (basicOverridability != null) return basicOverridability;

        List<CangJieType> superValueParameters = compiledValueParameters(superDescriptor);
        List<CangJieType> subValueParameters = compiledValueParameters(subDescriptor);

        List<TypeParameterDescriptor> superTypeParameters = superDescriptor.getTypeParameters();
        List<TypeParameterDescriptor> subTypeParameters = subDescriptor.getTypeParameters();

        if (superTypeParameters.size() != subTypeParameters.size()) {
            for (int i = 0; i < superValueParameters.size(); ++i) {
                // TODO: compare erasure
                if (!CangJieTypeChecker.DEFAULT.equalTypes(superValueParameters.get(i), subValueParameters.get(i))) {
                    return OverrideCompatibilityInfo.incompatible("Type parameter number mismatch");
                }
            }
            return OverrideCompatibilityInfo.conflict("Type parameter number mismatch");
        }


        TypeCheckerState typeCheckerState = createTypeCheckerState(superTypeParameters, subTypeParameters);

        for (int i = 0; i < superTypeParameters.size(); i++) {
            if (!areTypeParametersEquivalent(
                    superTypeParameters.get(i),
                    subTypeParameters.get(i),
                    typeCheckerState
            )) {
                return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch");
            }
        }

        for (int i = 0; i < superValueParameters.size(); i++) {
            if (!areTypesEquivalent(
                    superValueParameters.get(i),
                    subValueParameters.get(i),
                    typeCheckerState)
            ) {
                return OverrideCompatibilityInfo.incompatible("Value parameter type mismatch");
            }
        }

//        if (superDescriptor instanceof FunctionDescriptor && subDescriptor instanceof FunctionDescriptor &&
//                ((FunctionDescriptor) superDescriptor).isSuspend() != ((FunctionDescriptor) subDescriptor).isSuspend()) {
//            return OverrideCompatibilityInfo.conflict("Incompatible suspendability");
//        }

        if (checkReturnType) {
            CangJieType superReturnType = superDescriptor.getReturnType();
            CangJieType subReturnType = subDescriptor.getReturnType();

            if (superReturnType != null && subReturnType != null) {
                boolean bothErrors = CangJieTypeKt.isError(subReturnType) && CangJieTypeKt.isError(superReturnType);
                if (!bothErrors &&
                        !AbstractTypeChecker.INSTANCE.isSubtypeOf(
                                typeCheckerState,
                                subReturnType.unwrap(),
                                superReturnType.unwrap()
                        )
                ) {
                    return OverrideCompatibilityInfo.conflict("Return type mismatch");
                }
            }
        }

        return OverrideCompatibilityInfo.success();
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            @Nullable ClassDescriptor subClassDescriptor,
            boolean checkReturnType
    ) {
        OverrideCompatibilityInfo basicResult = isOverridableByWithoutExternalConditions(superDescriptor, subDescriptor, checkReturnType);
        boolean wasSuccess = basicResult.getResult() == OVERRIDABLE;

        for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
            // Do not run CONFLICTS_ONLY while there was no success
            if (externalCondition.getContract() == ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) continue;
            if (wasSuccess && externalCondition.getContract() == ExternalOverridabilityCondition.Contract.SUCCESS_ONLY)
                continue;

            ExternalOverridabilityCondition.Result result =
                    externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor);

            switch (result) {
                case OVERRIDABLE:
                    wasSuccess = true;
                    break;
                case INCOMPATIBLE:
                    return OverrideCompatibilityInfo.incompatible("External condition");
                case UNKNOWN:
                    // do nothing
                    // go to the next external condition or default override check
            }
        }

        if (!wasSuccess) {
            return basicResult;
        }

        // Search for conflicts from external conditions
        for (ExternalOverridabilityCondition externalCondition : EXTERNAL_CONDITIONS) {
            // Run all conditions that was not run before (i.e. CONFLICTS_ONLY)
            if (externalCondition.getContract() != ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) continue;

            ExternalOverridabilityCondition.Result result =
                    externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor);
            switch (result) {
                case INCOMPATIBLE:
                    return OverrideCompatibilityInfo.incompatible("External condition");
                case OVERRIDABLE:
                    throw new IllegalStateException(
                            "Contract violation in " + externalCondition.getClass().getName() + " condition. It's not supposed to end with success");
                case UNKNOWN:
                    // do nothing
                    // go to the next external condition or default override check
            }
        }

        return OverrideCompatibilityInfo.success();
    }

    @NotNull
    public OverrideCompatibilityInfo isOverridableBy(
            @NotNull CallableDescriptor superDescriptor,
            @NotNull CallableDescriptor subDescriptor,
            @Nullable ClassDescriptor subClassDescriptor
    ) {
        return isOverridableBy(superDescriptor, subDescriptor, subClassDescriptor, false);
    }

    public static class OverrideCompatibilityInfo {
        private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(OVERRIDABLE, "SUCCESS");
        private final Result overridable;
        private final String debugMessage;

        public OverrideCompatibilityInfo(@NotNull Result success, @NotNull String debugMessage) {
            this.overridable = success;
            this.debugMessage = debugMessage;
        }

        @NotNull
        public static OverrideCompatibilityInfo success() {
            return SUCCESS;
        }

        @NotNull
        public static OverrideCompatibilityInfo incompatible(@NotNull String debugMessage) {
            return new OverrideCompatibilityInfo(INCOMPATIBLE, debugMessage);
        }

        @NotNull
        public static OverrideCompatibilityInfo conflict(@NotNull String debugMessage) {
            return new OverrideCompatibilityInfo(CONFLICT, debugMessage);
        }

        @NotNull
        public Result getResult() {
            return overridable;
        }

        @NotNull
        public String getDebugMessage() {
            return debugMessage;
        }

        @Override
        public String toString() {
            return overridable + ": " + debugMessage;
        }

        public enum Result {
            OVERRIDABLE,
            INCOMPATIBLE,
            CONFLICT,
        }
    }
}
