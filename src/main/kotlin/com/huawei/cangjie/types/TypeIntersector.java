package com.huawei.cangjie.types;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.resolve.calls.inference.CallHandle;
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystem;
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl;
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind;
import com.huawei.cangjie.types.checker.CangJieTypeChecker;
import com.huawei.cangjie.types.error.ErrorTypeKind;
import com.huawei.cangjie.types.util.TypeUtils;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class TypeIntersector {


    @Nullable
    public static CangJieType intersectTypes(@NotNull Collection<CangJieType> types) {
        assert !types.isEmpty() : "Attempting to intersect empty collection of types, this case should be dealt with on the call site.";

        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Intersection of T1..Tn is an intersection of their non-null versions,
        //   made nullable is they all were nullable
        CangJieType nothingOrNullableNothing = null;
        boolean allNullable = true;
        List<CangJieType> nullabilityStripped = new ArrayList<>(types.size());
        for (CangJieType type : types) {
            if (CangJieTypeKt.isError(type)) continue;

            if (CangJieBuiltIns.isNothing (type)) {
                nothingOrNullableNothing = type;
            }
            allNullable &= type.isMarkedOption();
            nullabilityStripped.add(TypeUtils.makeNotNullable(type));
        }

        if (nothingOrNullableNothing != null) {
            return TypeUtils.makeNullableAsSpecified(nothingOrNullableNothing, allNullable);
        }

        if (nullabilityStripped.isEmpty()) {
            // All types were errors
            return ErrorUtils.createErrorType(ErrorTypeKind.INTERSECTION_OF_ERROR_TYPES, types.toString());
        }

        CangJieTypeChecker typeChecker = CangJieTypeChecker.DEFAULT;
        // Now we remove types that have subtypes in the list
        List<CangJieType> resultingTypes = new ArrayList<>();
        outer:
        for (CangJieType type : nullabilityStripped) {
            if (!TypeUtils.canHaveSubtypes(typeChecker, type)) {
                boolean relativeToAll = true;
                for (CangJieType other : nullabilityStripped) {
                    // It makes sense to check for subtyping (other <: type), despite that
                    // type is not supposed to be open, for there're enums
                    boolean mayBeEqual = TypeUnifier.mayBeEqual(type, other);
                    boolean relative = typeChecker.isSubtypeOf(type, other) || typeChecker.isSubtypeOf(other, type);
                    if (!mayBeEqual && !relative) {
                        return null;
                    } else if (!relative) {
                        // To build T & (final A), instead of returning just A as intersection
                        relativeToAll = false;
                        break;
                    }
                }
                if (relativeToAll) return TypeUtils.makeNullableAsSpecified(type, allNullable);
            }
            for (CangJieType other : nullabilityStripped) {
                if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                    continue outer;
                }
            }

            // Don't add type if it is already present, to avoid trivial type intersections in result
            for (CangJieType other : resultingTypes) {
                if (typeChecker.equalTypes(other, type)) {
                    continue outer;
                }
            }
            resultingTypes.add(type);
        }

        if (resultingTypes.isEmpty()) {
            // If we ended up here, it means that all types from `nullabilityStripped` were excluded by the code above
            // most likely, this is because they are all semantically interchangeable (e.g. List<Foo>! and List<Foo>),
            // in that case, we can safely select the best representative out of that set and return it
            // TODO: maybe return the most specific among the types that are subtypes to all others in the `nullabilityStripped`?
            // TODO: e.g. among {Int, Int?, Int!}, return `Int` (now it returns `Int!`).
            CangJieType bestRepresentative = FlexibleTypesKt.singleBestRepresentative(nullabilityStripped);

            if (bestRepresentative == null) {
                bestRepresentative = UtilsKt.hackForTypeIntersector(nullabilityStripped);
            }

            if (bestRepresentative == null) {
                return null;
            }
            return TypeUtils.makeNullableAsSpecified(bestRepresentative, allNullable);
        }

        if (resultingTypes.size() == 1) {
            return TypeUtils.makeNullableAsSpecified(resultingTypes.get(0), allNullable);
        }

        return new IntersectionTypeConstructor(resultingTypes).createType();
    }

    @NotNull
    public static CangJieType getUpperBoundsAsType(@NotNull TypeParameterDescriptor descriptor) {
        return intersectUpperBounds(descriptor, descriptor.getUpperBounds());
    }

    public static CangJieType intersectUpperBounds(@NotNull TypeParameterDescriptor descriptor, @NotNull List<CangJieType> upperBounds) {
        assert !upperBounds.isEmpty() : "Upper bound list is empty: " + descriptor;
        CangJieType upperBoundsAsType = intersectTypes(upperBounds);
        return upperBoundsAsType != null ? upperBoundsAsType : getBuiltIns(descriptor).getNothingType();
    }

    private static class TypeUnifier {
        public static boolean mayBeEqual(@NotNull CangJieType type, @NotNull CangJieType other) {
            return unify(type, other);
        }

        private static boolean unify(CangJieType withParameters, CangJieType expected) {
            // T -> how T is used
            Map<TypeParameterDescriptor, Variance> parameters = new HashMap<>();
            Function1<TypeParameterUsage, Unit> processor = parameterUsage -> {
                Variance howTheTypeIsUsedBefore = parameters.get(parameterUsage.typeParameterDescriptor);
                if (howTheTypeIsUsedBefore == null) {
                    howTheTypeIsUsedBefore = Variance.INVARIANT;
                }
                parameters.put(parameterUsage.typeParameterDescriptor,
                        parameterUsage.howTheTypeParameterIsUsed.superpose(howTheTypeIsUsedBefore));
                return Unit.INSTANCE;
            };
            processAllTypeParameters(withParameters, Variance.INVARIANT, processor, parameters::containsKey);
            processAllTypeParameters(expected, Variance.INVARIANT, processor, parameters::containsKey);
            ConstraintSystem.Builder constraintSystem = new ConstraintSystemBuilderImpl();
            TypeSubstitutor substitutor = constraintSystem.registerTypeVariables(CallHandle.NONE.INSTANCE, parameters.keySet(), false);
            constraintSystem.addSubtypeConstraint(withParameters, substitutor.substitute(expected, Variance.INVARIANT), ConstraintPositionKind.SPECIAL.position());

            return constraintSystem.build().getStatus().isSuccessful();
        }

        private static void processAllTypeParameters(
                CangJieType type,
                Variance howThisTypeIsUsed,
                Function1<TypeParameterUsage, Unit> result,
                Function1<TypeParameterDescriptor, Boolean> containsParameter
        ) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof TypeParameterDescriptor) {
                if (containsParameter.invoke((TypeParameterDescriptor) descriptor)) return;

                result.invoke(new TypeParameterUsage((TypeParameterDescriptor) descriptor, howThisTypeIsUsed));

                for (CangJieType superType : type.getConstructor().getSupertypes()) {
                    processAllTypeParameters(superType, howThisTypeIsUsed, result, containsParameter);
                }
            }
            for (TypeProjection projection : type.getArguments()) {
                if (projection.isStarProjection()) continue;
                processAllTypeParameters(projection.getType(), projection.getProjectionKind(), result, containsParameter);
            }
        }

        private static class TypeParameterUsage {
            private final TypeParameterDescriptor typeParameterDescriptor;
            private final Variance howTheTypeParameterIsUsed;

            public TypeParameterUsage(TypeParameterDescriptor typeParameterDescriptor, Variance howTheTypeParameterIsUsed) {
                this.typeParameterDescriptor = typeParameterDescriptor;
                this.howTheTypeParameterIsUsed = howTheTypeParameterIsUsed;
            }
        }
    }

}
