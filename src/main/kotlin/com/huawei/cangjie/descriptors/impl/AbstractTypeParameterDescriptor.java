package com.huawei.cangjie.descriptors.impl;


import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.huawei.cangjie.resolve.scopes.LazyScopeAdapter;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.resolve.scopes.TypeIntersectionScope;
import com.huawei.cangjie.storage.NotNullLazyValue;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.types.error.ErrorTypeKind;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTypeParameterDescriptor extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    private final Variance variance;
//    private final boolean reified;
    private final int index;
    private final NotNullLazyValue<TypeConstructor> typeConstructor;
    private final NotNullLazyValue<SimpleType> defaultType;
    private final StorageManager storageManager;

    protected AbstractTypeParameterDescriptor(
            @NotNull final StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull final Name name,
            @NotNull Variance variance,
//            boolean isReified,
            int index,
            @NotNull SourceElement source,
            @NotNull final SupertypeLoopChecker supertypeLoopChecker
    ) {
        super(containingDeclaration, annotations, name, source);
        this.variance = variance;
//        this.reified = isReified;
        this.index = index;

        this.typeConstructor = storageManager.createLazyValue(() -> new TypeParameterTypeConstructor(storageManager, supertypeLoopChecker));
        this.defaultType = storageManager.createLazyValue(new Function0<SimpleType>() {
            @Override
            public SimpleType invoke() {
                return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
                        TypeAttributes.Companion.getEmpty(),
                        getTypeConstructor(), Collections.emptyList(), false,
                        new LazyScopeAdapter(
                                new Function0<MemberScope>() {
                                    @Override
                                    public MemberScope invoke() {
                                        return TypeIntersectionScope.create("Scope for type parameter " + name.asString(), getUpperBounds());
                                    }
                                }
                        )
                );
            }
        });
        this.storageManager = storageManager;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getOriginal() {
        return (TypeParameterDescriptor) super.getOriginal();
    }

    @Override
    public @NotNull SimpleType getDefaultType() {
        return defaultType.invoke();
    }

    @NotNull
    @Override
    public Variance getVariance() {
        return variance;
    }

    @Override
    public <R, D> R accept(@NotNull DeclarationDescriptorVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitTypeParameterDescriptor(this, data);

    }

//    @Override
//    public boolean isReified() {
//        return false;
//    }

    @Override
    public @NotNull List<CangJieType> getUpperBounds() {
        return ((TypeParameterTypeConstructor) getTypeConstructor()).getSupertypes();

    }

    @Override
    public @NotNull TypeConstructor getTypeConstructor() {
        return typeConstructor.invoke();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isCapturedFromOuterDeclaration() {
        return false;
    }

    @NotNull
    @Override
    public StorageManager getStorageManager() {
        return storageManager;
    }

    protected abstract void reportSupertypeLoopError(@NotNull CangJieType type);

    @NotNull
    protected List<CangJieType> processBoundsWithoutCycles(@NotNull List<CangJieType> bounds) {
        return bounds;
    }

    @Override
    public void validate() {
        super.validate();
    }

    @NotNull
    protected abstract List<CangJieType> resolveUpperBounds();

    private class TypeParameterTypeConstructor extends AbstractTypeConstructor {

        private final SupertypeLoopChecker supertypeLoopChecker;

        public TypeParameterTypeConstructor(@NotNull StorageManager storageManager, SupertypeLoopChecker supertypeLoopChecker) {
            super(storageManager);
            this.supertypeLoopChecker = supertypeLoopChecker;
        }

        @NotNull
        @Override
        protected Collection<CangJieType> computeSupertypes() {
            return resolveUpperBounds();
        }
        @Override
        protected @NotNull Collection<CangJieType> computeExtendSuperTypes(@Nullable String extendId) {
            return List.of();
        }
        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public boolean isFinal() {
            return false;
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @NotNull
        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return AbstractTypeParameterDescriptor.this;
        }

        @NotNull
        @Override
        public CangJieBuiltIns getBuiltIns() {
            return DescriptorUtilsKt.getBuiltIns(AbstractTypeParameterDescriptor.this);
        }

        @Override
        public String toString() {
            return getName().toString();
        }

        @NotNull
        @Override
        protected SupertypeLoopChecker getSupertypeLoopChecker() {
            return supertypeLoopChecker;
        }

        @Override
        protected void reportSupertypeLoopError(@NotNull CangJieType type) {
            AbstractTypeParameterDescriptor.this.reportSupertypeLoopError(type);
        }

        @NotNull
        @Override
        protected List<CangJieType> processSupertypesWithoutCycles(@NotNull List<CangJieType> supertypes) {
            return processBoundsWithoutCycles(supertypes);
        }

        @Nullable
        @Override
        protected CangJieType defaultSupertypeIfEmpty() {
            return ErrorUtils.createErrorType(ErrorTypeKind.CYCLIC_UPPER_BOUNDS);
        }

        @Override
        protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
            return classifier instanceof TypeParameterDescriptor /*&&
                    DescriptorEquivalenceForOverrides.INSTANCE.areTypeParametersEquivalent(
                            AbstractTypeParameterDescriptor.this,
                            (TypeParameterDescriptor) classifier,
                            true
                    )*/;
        }


    }


}
