package com.huawei.cangjie.descriptors.impl;


import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.scopes.MemberScope;
import com.huawei.cangjie.storage.NotNullLazyValue;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.*;

import kotlin.jvm.functions.Function0;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class AbstractTypeParameterDescriptor extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor {
    private final Variance variance;
    private final boolean reified;
    private final int index;
//    private final NotNullLazyValue<TypeConstructor> typeConstructor;
//    private final NotNullLazyValue<SimpleType> defaultType;
    private final StorageManager storageManager;

    protected AbstractTypeParameterDescriptor(
            @NotNull final StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull final Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source,
            @NotNull final SupertypeLoopChecker supertypeLoopChecker
    ) {
        super(containingDeclaration, annotations, name, source);
        this.variance = variance;
        this.reified = isReified;
        this.index = index;

//        this.typeConstructor = storageManager.createLazyValue(new Function0<TypeConstructor>() {
//            @Override
//            public TypeConstructor invoke() {
//                return new TypeParameterTypeConstructor(storageManager, supertypeLoopChecker);
//            }
//        });
//        this.defaultType = storageManager.createLazyValue(new Function0<SimpleType>() {
//            @Override
//            public SimpleType invoke() {
//                return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
//                        TypeAttributes.Companion.getEmpty(),
//                        getTypeConstructor(), Collections.<TypeProjection>emptyList(), false,
//                        new LazyScopeAdapter(
//                                new Function0<MemberScope>() {
//                                    @Override
//                                    public MemberScope invoke() {
//                                        return TypeIntersectionScope.create("Scope for type parameter " + name.asString(), getUpperBounds());
//                                    }
//                                }
//                        )
//                );
//            }
//        });
        this.storageManager = storageManager;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getOriginal() {
        return (TypeParameterDescriptor) super.getOriginal();
    }

    @Override
    public @NotNull SimpleType getDefaultType() {
        return null;
    }
    @NotNull
    @Override
    public Variance getVariance() {
        return variance;
    }

    @Override
    public <R, D> R accept(@NotNull DeclarationDescriptorVisitor<R, D> visitor, @Nullable D data) {
        return null;
    }

    @Override
    public boolean isReified() {
        return false;
    }

    @Override
    public @NotNull List<CangJieType> getUpperBounds() {
        return null;
    }

    @Override
    public @NotNull TypeConstructor getTypeConstructor() {
        return null;
    }

    @Override
    public int getIndex() {
        return 0;
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

    @Override
    public void validate() {
        super.validate();
    }
}