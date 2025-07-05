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

package cn.cangnova.cangjie.types;

import cn.cangnova.cangjie.builtins.CangJieBuiltIns;
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor;
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor;
import cn.cangnova.cangjie.resolve.calls.inference.CallHandle;
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystem;
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl;
import cn.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind;
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker;
import cn.cangnova.cangjie.types.error.ErrorTypeKind;
import cn.cangnova.cangjie.types.util.TypeUtils;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cn.cangnova.cangjie.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class TypeIntersector {

    public static boolean isIntersectionEmpty(@NotNull CangJieType typeA, @NotNull CangJieType typeB) {
        return intersectTypes(new LinkedHashSet<>(Arrays.asList(typeA, typeB))) == null;
    }
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
            return TypeUtils.makeOptionalAsSpecified(nothingOrNullableNothing, allNullable);
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
                if (relativeToAll) return TypeUtils.makeOptionalAsSpecified(type, allNullable);
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
            return TypeUtils.makeOptionalAsSpecified(bestRepresentative, allNullable);
        }

        if (resultingTypes.size() == 1) {
            return TypeUtils.makeOptionalAsSpecified(resultingTypes.get(0), allNullable);
        }

        return new IntersectionTypeConstructor(resultingTypes).createType();
    }

    @NotNull
    public static CangJieType getUpperBoundsAsType(@NotNull TypeParameterDescriptor descriptor) {
        return intersectUpperBounds(descriptor, descriptor.upperBounds);
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
            ClassifierDescriptor descriptor = type.getConstructor().declarationDescriptor;
            if (descriptor instanceof TypeParameterDescriptor) {
                if (containsParameter.invoke((TypeParameterDescriptor) descriptor)) return;

                result.invoke(new TypeParameterUsage((TypeParameterDescriptor) descriptor, howThisTypeIsUsed));

                for (CangJieType superType : type.getConstructor().supertypes) {
                    processAllTypeParameters(superType, howThisTypeIsUsed, result, containsParameter);
                }
            }
            for (TypeProjection projection : type.getArguments()) {

                processAllTypeParameters(projection.type, projection.projectionKind, result, containsParameter);
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
