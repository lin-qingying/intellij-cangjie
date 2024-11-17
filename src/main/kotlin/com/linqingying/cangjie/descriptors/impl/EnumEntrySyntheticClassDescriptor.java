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

package com.linqingying.cangjie.descriptors.impl;


import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.incremental.components.LookupLocation;
import com.linqingying.cangjie.incremental.components.NoLookupLocation;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.resolve.NonReportingOverrideStrategy;
import com.linqingying.cangjie.resolve.OverridingUtil;
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter;
import com.linqingying.cangjie.resolve.scopes.MemberScope;
import com.linqingying.cangjie.resolve.source.MemberScopeImpl;
import com.linqingying.cangjie.storage.MemoizedFunctionToNotNull;
import com.linqingying.cangjie.storage.NotNullLazyValue;
import com.linqingying.cangjie.storage.StorageManager;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.ClassTypeConstructorImpl;
import com.linqingying.cangjie.types.TypeConstructor;
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner;
import com.linqingying.cangjie.utils.Printer;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnumEntrySyntheticClassDescriptor extends ClassDescriptorBase {
    private final TypeConstructor typeConstructor;
    private final MemberScope scope;
    private final NotNullLazyValue<Set<Name>> enumMemberNames;
    private final Annotations annotations;
    private final ClassConstructorDescriptor primaryConstructor;

    private EnumEntrySyntheticClassDescriptor(
            @NotNull List<CangJieType> types,
            @NotNull StorageManager storageManager,
            @NotNull ClassDescriptor containingClass,
            @NotNull CangJieType supertype,
            @NotNull Name name,
            @NotNull NotNullLazyValue<Set<Name>> enumMemberNames,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        super(storageManager, containingClass, name, source, /* isExternal = */ false);


        assert containingClass.getKind() == ClassKind.ENUM;
        this.primaryConstructor = new EnumEntryConstructorDescriptor(this, null, SourceElement.NO_SOURCE,
                () -> types

        );
        this.annotations = annotations;
        this.typeConstructor = new ClassTypeConstructorImpl(
                this, Collections.emptyList(), Collections.singleton(supertype), storageManager
        );

        this.scope = new EnumEntryScope(storageManager);
        this.enumMemberNames = enumMemberNames;
    }

    /**
     * Creates and initializes descriptors for enum entry with the given name and its companion object
     *
     * @param enumMemberNames needed for fake overrides resolution
     */
    @NotNull
    public static EnumEntrySyntheticClassDescriptor create(
            @NotNull List<CangJieType> types,
            @NotNull StorageManager storageManager,
            @NotNull ClassDescriptor enumClass,
            @NotNull Name name,
            @NotNull NotNullLazyValue<Set<Name>> enumMemberNames,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        CangJieType enumType = enumClass.getDefaultType();

        return new EnumEntrySyntheticClassDescriptor(types, storageManager, enumClass, enumType, name, enumMemberNames, annotations, source);
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull CangJieTypeRefiner kotlinTypeRefiner) {
        return scope;
    }

    @NotNull
    @Override
    public MemberScope getStaticScope() {
        return MemberScope.Empty.INSTANCE;
    }

    @NotNull
    @Override
    public Collection<ClassConstructorDescriptor> getConstructors() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull Collection<ClassConstructorDescriptor> getEndConstructors() {
        return List.of();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }


    @NotNull
    @Override
    public ClassKind getKind() {
        return ClassKind.ENUM_ENTRY;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return Modality.FINAL;
    }


    @Override
    public boolean isValue() {
        return false;
    }

    @Override
    public boolean isFun() {
        return false;
    }


    @Nullable
    @Override
    public ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "enum entry " + getName();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getDeclaredTypeParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getSealedSubclasses() {
        return Collections.emptyList();
    }

//    @Nullable
//    @Override
//    public ValueClassRepresentation<SimpleType> getValueClassRepresentation() {
//        return null;
//    }

    private class EnumEntryScope extends MemberScopeImpl {
        private final MemoizedFunctionToNotNull<Name, Collection<? extends SimpleFunctionDescriptor>> functions;
        private final MemoizedFunctionToNotNull<Name, Collection<? extends PropertyDescriptor>> properties;
        private final NotNullLazyValue<Collection<DeclarationDescriptor>> allDescriptors;

        public EnumEntryScope(@NotNull StorageManager storageManager) {
            this.functions = storageManager.createMemoizedFunction(new Function1<Name, Collection<? extends SimpleFunctionDescriptor>>() {
                @Override
                public Collection<? extends SimpleFunctionDescriptor> invoke(Name name) {
                    return computeFunctions(name);
                }
            });

            this.properties = storageManager.createMemoizedFunction(new Function1<Name, Collection<? extends PropertyDescriptor>>() {
                @Override
                public Collection<? extends PropertyDescriptor> invoke(Name name) {
                    return computeProperties(name);
                }
            });
            this.allDescriptors = storageManager.createLazyValue(new Function0<Collection<DeclarationDescriptor>>() {
                @Override
                public Collection<DeclarationDescriptor> invoke() {
                    return computeAllDeclarations();
                }
            });
        }

        @NotNull
        @Override
        public Collection<? extends PropertyDescriptor> getContributedVariables(@NotNull Name name, @NotNull LookupLocation location) {
            return properties.invoke(name);
        }

        @NotNull
        private Collection<? extends PropertyDescriptor> computeProperties(@NotNull Name name) {
            return resolveFakeOverrides(name, getSupertypeScope().getContributedPropertys(name, NoLookupLocation.FOR_NON_TRACKED_SCOPE));
        }

        @Override
        public Collection<SimpleFunctionDescriptor> getContributedFunctions(@NotNull Name name, @NotNull LookupLocation location) {
            return (Collection<SimpleFunctionDescriptor>) functions.invoke(name);
        }

        @NotNull
        private Collection<? extends SimpleFunctionDescriptor> computeFunctions(@NotNull Name name) {
            return resolveFakeOverrides(name, getSupertypeScope().getContributedFunctions(name, NoLookupLocation.FOR_NON_TRACKED_SCOPE));
        }

        @NotNull
        private MemberScope getSupertypeScope() {
            Collection<CangJieType> supertype = getTypeConstructor().getSupertypes();
            assert supertype.size() == 1 : "Enum entry and its companion object both should have exactly one supertype: " + supertype;
            return supertype.iterator().next().getMemberScope();
        }

        @NotNull
        private <D extends CallableMemberDescriptor> Collection<? extends D> resolveFakeOverrides(
                @NotNull Name name,
                @NotNull Collection<? extends D> fromSupertypes
        ) {
            final Set<D> result = new LinkedHashSet<D>();

            OverridingUtil.DEFAULT.generateOverridesInFunctionGroup(
                    name, fromSupertypes, Collections.emptySet(), EnumEntrySyntheticClassDescriptor.this,
                    new NonReportingOverrideStrategy() {
                        @Override
                        @SuppressWarnings("unchecked")
                        public void addFakeOverride(@NotNull CallableMemberDescriptor fakeOverride) {
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null);
                            result.add((D) fakeOverride);
                        }

                        @Override
                        protected void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            // Do nothing
                        }
                    }
            );

            return result;
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getContributedDescriptors(
                @NotNull DescriptorKindFilter kindFilter,
                @NotNull Function1<? super Name, Boolean> nameFilter
        ) {
            return allDescriptors.invoke();
        }

        @NotNull
        private Collection<DeclarationDescriptor> computeAllDeclarations() {
            Collection<DeclarationDescriptor> result = new HashSet<DeclarationDescriptor>();
            for (Name name : enumMemberNames.invoke()) {
                result.addAll(getContributedFunctions(name, NoLookupLocation.FOR_NON_TRACKED_SCOPE));
                result.addAll(getContributedVariables(name, NoLookupLocation.FOR_NON_TRACKED_SCOPE));
            }
            return result;
        }

        @NotNull
        @Override
        public Set<Name> getFunctionNames() {
            return enumMemberNames.invoke();
        }

        @NotNull
        @Override
        public Set<Name> getClassifierNames() {
            return Collections.emptySet();
        }

        @NotNull
        @Override
        public Set<Name> getVariableNames() {
            return enumMemberNames.invoke();
        }

        @Override
        public void printScopeStructure(@NotNull Printer p) {
            p.println("enum entry scope for " + EnumEntrySyntheticClassDescriptor.this);
        }
    }
}
