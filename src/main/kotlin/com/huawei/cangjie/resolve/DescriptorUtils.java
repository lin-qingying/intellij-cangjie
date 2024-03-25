package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
//import com.huawei.cangjie.descriptors.ModuleDescriptor;
import com.huawei.cangjie.descriptors.PackageFragmentDescriptor;
import com.huawei.cangjie.descriptors.PackageViewDescriptor;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.name.FqNameUnsafe;
import com.huawei.cangjie.types.ErrorUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.descriptors.annotations.AnnotationDescriptorKt.getFqNameUnsafe;

public class DescriptorUtils {
    @Nullable
    private static FqName getFqNameSafeIfPossible(@NotNull DeclarationDescriptor descriptor) {
        if (/*descriptor instanceof ModuleDescriptor || */ErrorUtils.isError(descriptor)) {
            return FqName.ROOT;
        }

        if (descriptor instanceof PackageViewDescriptor) {
            return ((PackageViewDescriptor) descriptor).getFqName();
        }
        else if (descriptor instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) descriptor).getFqName();
        }

        return null;
    }
    @NotNull
    public static FqNameUnsafe getFqName(@NotNull DeclarationDescriptor descriptor) {
        FqName safe = getFqNameSafeIfPossible(descriptor);
        return safe != null ? safe.toUnsafe() : getFqNameUnsafe(descriptor);
    }
}
