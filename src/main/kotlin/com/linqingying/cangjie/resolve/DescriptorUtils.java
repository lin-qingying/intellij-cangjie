//package com.linqingying.cangjie.resolve;
//import com.linqingying.cangjie.descriptors.*;
////import com.linqingying.cangjie.descriptors.ModuleDescriptor;
//import com.linqingying.cangjie.name.FqName;
//import com.linqingying.cangjie.name.FqNameUnsafe;
//import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter;
//import com.linqingying.cangjie.resolve.scopes.MemberScope;
//import com.linqingying.cangjie.types.ErrorUtils;
//
//
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Collection;
//
//import static com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptorKt.getFqNameUnsafe;
//
//public class DescriptorUtils {
//
//
//    @Nullable
//    public static ReceiverParameterDescriptor getDispatchReceiverParameterIfNeeded(@NotNull DeclarationDescriptor containingDeclaration) {
//        if (containingDeclaration instanceof ClassDescriptor) {
//            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
//            return classDescriptor.getThisAsReceiverParameter();
//        }
//        return null;
//    }
//    public static bool isEnumEntry(@NotNull DeclarationDescriptor descriptor) {
//        return isKindOf(descriptor, ClassKind.ENUM_ENTRY);
//    }
//    private static bool isKindOf(@Nullable DeclarationDescriptor descriptor, @NotNull ClassKind classKind) {
//        return descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() == classKind;
//    }
//
//    @NotNull
//    public static Collection<DeclarationDescriptor> getAllDescriptors(@NotNull MemberScope scope) {
//        return scope.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.Companion.getALL_NAME_FILTER());
//    }
//    public static bool isInterface(@Nullable DeclarationDescriptor descriptor) {
//        return isKindOf(descriptor, ClassKind.INTERFACE);
//    }
//    @Nullable
//    private static FqName getFqNameSafeIfPossible(@NotNull DeclarationDescriptor descriptor) {
//        if (/*descriptor instanceof ModuleDescriptor || */ErrorUtils.isError(descriptor)) {
//            return FqName.ROOT;
//        }
//
//        if (descriptor instanceof PackageViewDescriptor) {
//            return ((PackageViewDescriptor) descriptor).getFqName();
//        }
//        else if (descriptor instanceof PackageFragmentDescriptor) {
//            return ((PackageFragmentDescriptor) descriptor).getFqName();
//        }
//
//        return null;
//    }
//    @NotNull
//    public static FqNameUnsafe getFqName(@NotNull DeclarationDescriptor descriptor) {
//        FqName safe = getFqNameSafeIfPossible(descriptor);
//        return safe != null ? safe.toUnsafe() : getFqNameUnsafe(descriptor);
//    }
//}
