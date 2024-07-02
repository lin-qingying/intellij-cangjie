package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;
import com.huawei.cangjie.types.checker.CangJieTypePreparator;
import com.huawei.cangjie.types.checker.CangJieTypeRefiner;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*;

public class OverridingUtil {


    private static final List<ExternalOverridabilityCondition> EXTERNAL_CONDITIONS =
            CollectionsKt.toList(ServiceLoader.load(
                    ExternalOverridabilityCondition.class,
                    ExternalOverridabilityCondition.class.getClassLoader()
            ));

    public static final OverridingUtil DEFAULT;

    private static final CangJieTypeChecker.TypeConstructorEquality DEFAULT_TYPE_CONSTRUCTOR_EQUALITY =
            new CangJieTypeChecker.TypeConstructorEquality() {
                @Override
                public boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
                    return a.equals(b);
                }
            };

    static {
        DEFAULT = new OverridingUtil(
                DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, CangJieTypeRefiner.Default.INSTANCE, CangJieTypePreparator.Default.INSTANCE,
                null
        );
    }



    private final CangJieTypeRefiner kotlinTypeRefiner;
    private final CangJieTypePreparator kotlinTypePreparator;
    private final CangJieTypeChecker.TypeConstructorEquality equalityAxioms;
    private final Function2<CangJieType, CangJieType, Boolean> customSubtype;

    private OverridingUtil(
            @NotNull CangJieTypeChecker.TypeConstructorEquality axioms,
            @NotNull CangJieTypeRefiner kotlinTypeRefiner,
            @NotNull CangJieTypePreparator kotlinTypePreparator,
            @Nullable Function2<CangJieType, CangJieType, Boolean> customSubtype
    ) {
        equalityAxioms = axioms;
        this.kotlinTypeRefiner = kotlinTypeRefiner;
        this.kotlinTypePreparator = kotlinTypePreparator;
        this.customSubtype = customSubtype;
    }

    @NotNull
    public static OverridingUtil createWithTypeRefiner(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return new OverridingUtil(DEFAULT_TYPE_CONSTRUCTOR_EQUALITY, cangjieTypeRefiner, CangJieTypePreparator.Default.INSTANCE, null);
    }


    public static class OverrideCompatibilityInfo {
        public enum Result {
            OVERRIDABLE,
            INCOMPATIBLE,
            CONFLICT,
        }

        private static final OverrideCompatibilityInfo SUCCESS = new OverrideCompatibilityInfo(OVERRIDABLE, "SUCCESS");

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

        private final Result overridable;
        private final String debugMessage;

        public OverrideCompatibilityInfo(@NotNull Result success, @NotNull String debugMessage) {
            this.overridable = success;
            this.debugMessage = debugMessage;
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
        if (a instanceof VariableDescriptor) {
            assert b instanceof VariableDescriptor : "b is " + b.getClass();

            VariableDescriptor pa = (VariableDescriptor) a;
            VariableDescriptor pb = (VariableDescriptor) b;

//            if (!isAccessorMoreSpecific(pa.getSetter(), pb.getSetter())) return false;

            if (pa.isVar() && pb.isVar()) {
                // TODO(dsavvinov): using DEFAULT here looks suspicious
                return AbstractTypeChecker.INSTANCE.equalTypes(checkerState, aReturnType.unwrap(), bReturnType.unwrap());
            }
            else {
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

//    private static boolean isAccessorMoreSpecific(@Nullable PropertyAccessorDescriptor a, @Nullable PropertyAccessorDescriptor b) {
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

        Collection<H> candidates = new ArrayList<H>(2);
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
        }
        else if (candidates.size() == 1) {
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
        Collection<H> overridable = new ArrayList<H>();
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
            }
            else if (finalResult == CONFLICT) {
                onConflict.invoke(candidate);
                iterator.remove();
            }
        }
        return overridable;
    }
    private static List<CangJieType> compiledValueParameters(CallableDescriptor callableDescriptor) {
        ReceiverParameterDescriptor receiverParameter = callableDescriptor.getExtensionReceiverParameter();
        List<CangJieType> parameters = new ArrayList<CangJieType>();
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

        OverrideCompatibilityInfo receiverAndParameterResult = checkReceiverAndParameterCount(superDescriptor, subDescriptor);
        if (receiverAndParameterResult != null) {
            return receiverAndParameterResult;
        }

        return null;
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


    @NotNull
    private TypeCheckerState createTypeCheckerState(
            @NotNull List<TypeParameterDescriptor> firstParameters,
            @NotNull List<TypeParameterDescriptor> secondParameters
    ) {
        assert firstParameters.size() == secondParameters.size() :
                "Should be the same number of type parameters: " + firstParameters + " vs " + secondParameters;

        if (firstParameters.isEmpty()) {
            return new OverridingUtilTypeSystemContext(
                    null, equalityAxioms, kotlinTypeRefiner, kotlinTypePreparator, customSubtype
            ).newTypeCheckerState(true, true);
        }

        Map<TypeConstructor, TypeConstructor> matchingTypeConstructors = new HashMap<TypeConstructor, TypeConstructor>();
        for (int i = 0; i < firstParameters.size(); i++) {
            matchingTypeConstructors.put(firstParameters.get(i).getTypeConstructor(), secondParameters.get(i).getTypeConstructor());
        }

        return new OverridingUtilTypeSystemContext(
                matchingTypeConstructors, equalityAxioms, kotlinTypeRefiner, kotlinTypePreparator, customSubtype
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

        if (superDescriptor instanceof FunctionDescriptor && subDescriptor instanceof FunctionDescriptor &&
                ((FunctionDescriptor) superDescriptor).isSuspend() != ((FunctionDescriptor) subDescriptor).isSuspend()) {
            return OverrideCompatibilityInfo.conflict("Incompatible suspendability");
        }

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
        List<CangJieType> subBounds = new ArrayList<CangJieType>(subTypeParameter.getUpperBounds());
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
            if (wasSuccess && externalCondition.getContract() == ExternalOverridabilityCondition.Contract.SUCCESS_ONLY) continue;

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
}
