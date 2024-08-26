package com.huawei.cangjie.types;

import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.descriptors.SupertypeLoopChecker;
import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.resolve.DescriptorUtils;
import com.huawei.cangjie.storage.StorageManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassTypeConstructorImpl extends AbstractClassTypeConstructor implements TypeConstructor{
    private final ClassDescriptor classDescriptor;
    private final List<TypeParameterDescriptor> parameters;
    private final Collection<CangJieType> supertypes;

    public ClassTypeConstructorImpl(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull Collection<CangJieType> supertypes,
            @NotNull StorageManager storageManager
    ) {
        super(storageManager);
        this.classDescriptor = classDescriptor;
        this.parameters = List.copyOf(parameters);
        this.supertypes = Collections.unmodifiableCollection(supertypes);
    }

    @Override
    public boolean isDenotable() {
        return true;
    }

    @Override
    public String toString() {
        return DescriptorUtils.getFqName(classDescriptor).asString();
    }


    @Override
    public @NotNull ClassDescriptor getDeclarationDescriptor() {
        return classDescriptor;

    }

    @Override
    public @NotNull List<TypeParameterDescriptor> getParameters() {
        return parameters;

    }

    @NotNull
    @Override
    protected Collection<CangJieType> computeSupertypes() {
        return supertypes;

    }

    @NotNull
    @Override
    protected SupertypeLoopChecker getSupertypeLoopChecker() {
        return SupertypeLoopChecker.EMPTY.INSTANCE;

    }


}
