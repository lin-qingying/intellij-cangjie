//package cn.cangnova.cangjie.descriptors.impl;
//
//import cn.cangnova.cangjie.descriptors.DeclarationDescriptor;
//import org.jetbrains.annotations.NotNull;
//
//public abstract class DeclarationDescriptorImpl  implements DeclarationDescriptor {
//    @NotNull
//    private final Name name;
//
//    public DeclarationDescriptorImpl(@NotNull Annotations annotations, @NotNull Name name) {
//        super(annotations);
//        this.name = name;
//    }
//
//    @NotNull
//    @Override
//    public Name getName() {
//        return name;
//    }
//
//    @NotNull
//    @Override
//    public DeclarationDescriptor getOriginal() {
//        return this;
//    }
//
//    @Override
//    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
//        accept(visitor, null);
//    }
//
//    @Override
//    public String toString() {
//        return toString(this);
//    }
//
//    @NotNull
//    public static String toString(@NotNull DeclarationDescriptor descriptor) {
//        try {
//            return DescriptorRenderer.DEBUG_TEXT.render(descriptor) +
//                    "[" + descriptor.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(descriptor)) + "]";
//        } catch (Throwable e) {
//            // DescriptionRenderer may throw if this is not yet completely initialized
//            // It is very inconvenient while debugging
//            return descriptor.getClass().getSimpleName() + " " + descriptor.getName();
//        }
//    }
//}
