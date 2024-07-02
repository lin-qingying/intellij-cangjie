package com.huawei.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DescriptorVisibilities {
    @Nullable
    public static Integer compare(@NotNull DescriptorVisibility first, @NotNull DescriptorVisibility second) {
        Integer result = first.compareTo(second);
        if (result != null) {
            return result;
        }
        Integer oppositeResult = second.compareTo(first);
        if (oppositeResult != null) {
            return -oppositeResult;
        }
        return null;
    }

    @NotNull
    public static final DescriptorVisibility PROTECTED = new DelegatedDescriptorVisibility(Visibilities.Protected.INSTANCE) {

    };
    @NotNull
    public static final DescriptorVisibility PRIVATE = new DelegatedDescriptorVisibility(Visibilities.Private.INSTANCE) {

    };
    @NotNull
    public static final DescriptorVisibility PUBLIC = new DelegatedDescriptorVisibility(Visibilities.Public.INSTANCE) {

    };

    public static final DescriptorVisibility DEFAULT_VISIBILITY = PUBLIC;
    /**
     * This visibility is needed for the next case:
     * class A<in T>(t: T) {
     * private val t: T = t // visibility for t is PRIVATE_TO_THIS
     * <p>
     * fun test() {
     * val x: T = t // correct
     * val y: T = this.t // also correct
     * }
     * fun foo(a: A<String>) {
     * val x: String = a.t // incorrect, because a.t can be Any
     * }
     * }
     */
    @NotNull
    public static final DescriptorVisibility PRIVATE_TO_THIS = new DelegatedDescriptorVisibility(Visibilities.PrivateToThis.INSTANCE) {

    };
    @NotNull
    public static final DescriptorVisibility LOCAL = new DelegatedDescriptorVisibility(Visibilities.Local.INSTANCE) {
//        @Override
//        public boolean isVisible(
//                @Nullable ReceiverValue receiver,
//                @NotNull DeclarationDescriptorWithVisibility what,
//                @NotNull DeclarationDescriptor from,
//                boolean useSpecialRulesForPrivateSealedConstructors
//        ) {
//            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
//        }
    };
    // Currently used as default visibility of FunctionDescriptor
    // It's needed to prevent NPE when requesting non-nullable visibility of descriptor before `initialize` has been called
    @NotNull
    public static final DescriptorVisibility UNKNOWN = new DelegatedDescriptorVisibility(Visibilities.Unknown.INSTANCE) {
//        @Override
//        public boolean isVisible(
//                @Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from,
//                boolean useSpecialRulesForPrivateSealedConstructors
//        ) {
//            return false;
//        }
    };
    @NotNull
    private static final Map<Visibility, DescriptorVisibility> visibilitiesMapping = new HashMap<Visibility, DescriptorVisibility>();

    public static boolean isPrivate(@NotNull DescriptorVisibility visibility) {
        return visibility == PRIVATE || visibility == PRIVATE_TO_THIS;
    }

    @NotNull
    public static DescriptorVisibility toDescriptorVisibility(@NotNull Visibility visibility) {
        DescriptorVisibility correspondingVisibility = visibilitiesMapping.get(visibility);
        if (correspondingVisibility == null) {
            throw new IllegalArgumentException("Inapplicable visibility: " + visibility);
        }
        return correspondingVisibility;
    }
}
