package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.descriptors.impl.TypeAliasConstructorDescriptor;
import com.huawei.cangjie.resolve.DescriptorUtils;
import com.huawei.cangjie.resolve.DescriptorUtilsKt;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.utils.CollectionsKt;
import com.huawei.cangjie.utils.ModuleVisibilityHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DescriptorVisibilities {

    /**
     * This value should be used for receiverValue parameter of Visibility.isVisible
     * iff there is intention to determine if member is visible without receiver related checks being performed.
     */
    public static final ReceiverValue ALWAYS_SUITABLE_RECEIVER = new ReceiverValue() {
        @NotNull
        @Override
        public CangJieType getType() {
            throw new IllegalStateException("This method should not be called");
        }

        @NotNull
        @Override
        public ReceiverValue replaceType(@NotNull CangJieType newType) {
            throw new IllegalStateException("This method should not be called");
        }

        @NotNull
        @Override
        public ReceiverValue getOriginal() {
            return this;
        }
    };
    @NotNull
    public static final DescriptorVisibility INHERITED = new DelegatedDescriptorVisibility(Visibilities.Inherited.INSTANCE) {
        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptor what,
                @NotNull DeclarationDescriptor from,
                boolean useSpecialRulesForPrivateSealedConstructors
        ) {
            throw new IllegalStateException("Visibility is unknown yet"); //This method shouldn't be invoked for INHERITED visibility
        }
    };
    /* Visibility for fake override invisible members (they are created for better error reporting) */
    @NotNull
    public static final DescriptorVisibility INVISIBLE_FAKE = new DelegatedDescriptorVisibility(Visibilities.InvisibleFake.INSTANCE) {
        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptor what,
                @NotNull DeclarationDescriptor from,
                boolean useSpecialRulesForPrivateSealedConstructors
        ) {
            return false;
        }
    };
    /**
     * private*****可见*******************可见********************可见**********************可见
     *****************************/


//    当前文件可见
    @NotNull
    public static final DescriptorVisibility PRIVATE = new DelegatedDescriptorVisibility(Visibilities.Private.INSTANCE) {
        //        private boolean hasContainingSourceFile(@NotNull DeclarationDescriptor descriptor) {
//            return DescriptorUtils.getContainingSourceFile(descriptor) != SourceFile.NO_SOURCE_FILE;
//        }
        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from, boolean useSpecialRulesForPrivateSealedConstructors) {
            if (DescriptorUtils.isTopLevelDeclaration(what)/* && hasContainingSourceFile(from)*/) {
                return inSameFile(what, from);
            }

            if (what instanceof ConstructorDescriptor) {
                ClassifierDescriptorWithTypeParameters classDescriptor = ((ConstructorDescriptor) what).getContainingDeclaration();
                if (useSpecialRulesForPrivateSealedConstructors
                        && DescriptorUtils.isSealedClass(classDescriptor)
                        && DescriptorUtils.isTopLevelDeclaration(classDescriptor)
                        && from instanceof ConstructorDescriptor
                        && DescriptorUtils.isTopLevelDeclaration(from.getContainingDeclaration())
                        && inSameFile(what, from)) {
                    return true;
                }
            }

            DeclarationDescriptor parent = what;
            while (parent != null) {
                parent = parent.getContainingDeclaration();
                if ((parent instanceof ClassDescriptor) ||
                        parent instanceof PackageFragmentDescriptor) {
                    break;
                }
            }
            if (parent == null) {
                return false;
            }
            DeclarationDescriptor fromParent = from;
            while (fromParent != null) {
                if (parent == fromParent) {
                    return true;
                }
                if (fromParent instanceof PackageFragmentDescriptor) {
                    return parent instanceof PackageFragmentDescriptor
                            && ((PackageFragmentDescriptor) parent).getFqName().equals(((PackageFragmentDescriptor) fromParent).getFqName())
                            && DescriptorUtils.areInSameModule(fromParent, parent);
                }
                fromParent = fromParent.getContainingDeclaration();
            }
            return false;
        }
    };
    //所有可见
    @NotNull
    public static final DescriptorVisibility PUBLIC = new DelegatedDescriptorVisibility(Visibilities.Public.INSTANCE) {

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from, boolean useSpecialRulesForPrivateSealedConstructors) {
            return true;
        }
    };
    public static final DescriptorVisibility DEFAULT_VISIBILITY = PUBLIC;


    /***********************************************访问修饰符规则***************************************************************/
    /**************文件****************包 & 子包*******************模块************************所有包*****************************/
    /**private*****可见******************不可见********************不可见**********************不可见*****************************/
    /**internal****可见*******************可见********************不可见**********************不可见*****************************/
    /**private*****可见*******************可见*********************可见**********************不可见*****************************/
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

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from, boolean useSpecialRulesForPrivateSealedConstructors) {
            return false;
        }
    };
    @NotNull
    public static final DescriptorVisibility LOCAL = new DelegatedDescriptorVisibility(Visibilities.Local.INSTANCE) {
        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from, boolean useSpecialRulesForPrivateSealedConstructors) {
            return false;
        }
//        @Override
//        public bool isVisible(
//                @Nullable ReceiverValue receiver,
//                @NotNull DeclarationDescriptorWithVisibility what,
//                @NotNull DeclarationDescriptor from,
//                bool useSpecialRulesForPrivateSealedConstructors
//        ) {
//            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
//        }
    };
    // Currently used as default visibility of FunctionDescriptor
    // It's needed to prevent NPE when requesting non-nullable visibility of descriptor before `initialize` has been called
    @NotNull
    public static final DescriptorVisibility UNKNOWN = new DelegatedDescriptorVisibility(Visibilities.Unknown.INSTANCE) {
        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from, boolean useSpecialRulesForPrivateSealedConstructors) {
            return false;
        }
//        @Override
//        public bool isVisible(
//                @Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from,
//                bool useSpecialRulesForPrivateSealedConstructors
//        ) {
//            return false;
//        }
    };
    @NotNull
    private static final ModuleVisibilityHelper MODULE_VISIBILITY_HELPER;
    // 文件  包以及子包可见
    @NotNull
    public static final DescriptorVisibility INTERNAL = new DelegatedDescriptorVisibility(Visibilities.Internal.INSTANCE) {
        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptor what,
                @NotNull DeclarationDescriptor from,
                boolean useSpecialRulesForPrivateSealedConstructors
        ) {
            if (from instanceof ModuleDescriptor) {
                return true;
            }
//            DescriptorUtils.getContainingModule(what);
            PackageData fromModule = DescriptorUtils.getPackageDeclarationDescriptor(from);

//            if (what instanceof  PackageViewDescriptor){
//                if (!fromModule.shouldSeeInternalsOf((PackageViewDescriptor)what)) return false;
//
//            }
            PackageData whatModule = DescriptorUtils.getPackageDeclarationDescriptor(what);

//            判断 fromModule 是不是 whatModule的子包或本包
//             fromModule 是否可见 whatModule

            if (whatModule instanceof PackageFragmentDescriptor) {
                if (!fromModule.shouldSeeInternalsOf((PackageFragmentDescriptor) whatModule)) return false;

            } else {
                if (!fromModule.shouldSeeInternalsOf(whatModule)) return false;

            }


            return MODULE_VISIBILITY_HELPER.isInFriendModule(what, from);

        }
    };
    //    模块内可见
    @NotNull
    public static final DescriptorVisibility PROTECTED = new DelegatedDescriptorVisibility(Visibilities.Protected.INSTANCE) {


        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from, boolean useSpecialRulesForPrivateSealedConstructors) {


            if (DescriptorUtilsKt.isSameModule(what, from)) {
                return true;
            }

            return MODULE_VISIBILITY_HELPER.isInFriendModule(what, from);
        }
    };
    /**
     * This value should be used for receiverValue parameter of Visibility.isVisible
     * iff there is intention to determine if member is visible for any receiver.
     */
    private static final ReceiverValue IRRELEVANT_RECEIVER = new ReceiverValue() {
        @NotNull
        @Override
        public CangJieType getType() {
            throw new IllegalStateException("This method should not be called");
        }

        @NotNull
        @Override
        public ReceiverValue replaceType(@NotNull CangJieType newType) {
            throw new IllegalStateException("This method should not be called");
        }

        @NotNull
        @Override
        public ReceiverValue getOriginal() {
            return this;
        }
    };
    @NotNull
    private static final Map<Visibility, DescriptorVisibility> visibilitiesMapping = new HashMap<>();
    private static final Map<DescriptorVisibility, Integer> ORDERED_VISIBILITIES;

    static {
        recordVisibilityMapping(PRIVATE);
        recordVisibilityMapping(PRIVATE_TO_THIS);
        recordVisibilityMapping(PROTECTED);
        recordVisibilityMapping(INTERNAL);
        recordVisibilityMapping(PUBLIC);
        recordVisibilityMapping(LOCAL);
        recordVisibilityMapping(INHERITED);
        recordVisibilityMapping(INVISIBLE_FAKE);
        recordVisibilityMapping(UNKNOWN);
    }

    static {
        Iterator<ModuleVisibilityHelper> iterator = ServiceLoader.load(ModuleVisibilityHelper.class, ModuleVisibilityHelper.class.getClassLoader()).iterator();
        MODULE_VISIBILITY_HELPER = iterator.hasNext() ? iterator.next() : ModuleVisibilityHelper.EMPTY.INSTANCE;
    }

    static {
        Map<DescriptorVisibility, Integer> visibilities = CollectionsKt.newHashMapWithExpectedSize(4);
        visibilities.put(PRIVATE_TO_THIS, 0);
        visibilities.put(PRIVATE, 0);
        visibilities.put(INTERNAL, 1);
        visibilities.put(PROTECTED, 1);
        visibilities.put(PUBLIC, 2);
        ORDERED_VISIBILITIES = Collections.unmodifiableMap(visibilities);
    }

    private static void recordVisibilityMapping(DescriptorVisibility visibility) {
        visibilitiesMapping.put(visibility.getDelegate(), visibility);
    }

    // Note that this method returns false if `from` declaration is `init` initializer
    // because initializer does not have source element
    public static boolean inSameFile(@NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from) {
        SourceFile fromContainingFile = DescriptorUtils.getContainingSourceFile(from);
        if (fromContainingFile != SourceFile.NO_SOURCE_FILE) {
            return fromContainingFile.equals(DescriptorUtils.getContainingSourceFile(what));
        }
        return false;
    }

    public static DescriptorVisibility formName(String name) {
        return switch (name) {
            case "public" -> PUBLIC;
            case "protected" -> PROTECTED;
            case "internal" -> INTERNAL;
            case "private" -> PRIVATE;
//            case "private_to_this" -> PRIVATE_TO_THIS;
//            case "local" -> LOCAL;
            default -> throw new IllegalArgumentException("unknown visibility name: " + name);
        };


    }
//    public static boolean isVisibleIgnoringReceiver(
//            @NotNull DeclarationDescriptorWithVisibility what,
//            @NotNull DeclarationDescriptor from,
//            boolean useSpecialRulesForPrivateSealedConstructors
//    ) {
//        return findInvisibleMember(ALWAYS_SUITABLE_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
//    }
//
//    public static boolean isVisibleWithAnyReceiver(
//            @NotNull DeclarationDescriptorWithVisibility what,
//            @NotNull DeclarationDescriptor from,
//            boolean useSpecialRulesForPrivateSealedConstructors
//    ) {
//        return findInvisibleMember(IRRELEVANT_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
//    }

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

    public static boolean isVisibleIgnoringReceiver(
            @NotNull DeclarationDescriptor what,
            @NotNull DeclarationDescriptor from,
            boolean useSpecialRulesForPrivateSealedConstructors
    ) {
        return findInvisibleMember(ALWAYS_SUITABLE_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
    }

    public static boolean isVisibleWithAnyReceiver(
            @NotNull DeclarationDescriptor what,
            @NotNull DeclarationDescriptor from,
            boolean useSpecialRulesForPrivateSealedConstructors
    ) {
        return findInvisibleMember(IRRELEVANT_RECEIVER, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
    }

    public static boolean isVisible(
            @Nullable ReceiverValue receiver,
            @NotNull DeclarationDescriptorWithVisibility what,
            @NotNull DeclarationDescriptor from,
            boolean useSpecialRulesForPrivateSealedConstructors
    ) {
        return findInvisibleMember(receiver, what, from, useSpecialRulesForPrivateSealedConstructors) == null;
    }

    //    @Nullable
//    public static DeclarationDescriptorWithVisibility findInvisibleMember(
//            @Nullable ReceiverValue receiver,
//            @NotNull DeclarationDescriptorWithVisibility what,
//            @NotNull DeclarationDescriptor from,
//            boolean useSpecialRulesForPrivateSealedConstructors
//    ) {
//        DeclarationDescriptorWithVisibility parent = (DeclarationDescriptorWithVisibility) what.getOriginal();
//        while (parent != null && parent.getVisibility() != LOCAL) {
//            if (!parent.getVisibility().isVisible(receiver, parent, from, useSpecialRulesForPrivateSealedConstructors)) {
//                return parent;
//            }
//            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
//        }
//
//        if (what instanceof TypeAliasConstructorDescriptor) {
//            DeclarationDescriptorWithVisibility invisibleUnderlying =
//                    findInvisibleMember(
//                            receiver,
//                            ((TypeAliasConstructorDescriptor) what).getUnderlyingConstructorDescriptor(),
//                            from,
//                            useSpecialRulesForPrivateSealedConstructors
//                    );
//            return invisibleUnderlying;
//        }
//
//        return null;
//    }
    @Nullable
    public static DeclarationDescriptor findInvisibleMember(
            @Nullable ReceiverValue receiver,
            @NotNull DeclarationDescriptor what,
            @NotNull DeclarationDescriptor from,
            boolean useSpecialRulesForPrivateSealedConstructors
    ) {
        DeclarationDescriptor parent = what.getOriginal();
        while (parent != null && parent.getVisibility() != LOCAL) {
            if (!parent.getVisibility().isVisible(receiver, parent, from, useSpecialRulesForPrivateSealedConstructors)) {
                return parent;
            }
            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }

        if (what instanceof TypeAliasConstructorDescriptor) {
            DeclarationDescriptor invisibleUnderlying =
                    findInvisibleMember(
                            receiver,
                            ((TypeAliasConstructorDescriptor) what).getUnderlyingConstructorDescriptor(),
                            from,
                            useSpecialRulesForPrivateSealedConstructors
                    );
            return invisibleUnderlying;
        }

        return null;
    }

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
