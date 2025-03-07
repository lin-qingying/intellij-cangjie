//package cn.cangnova.cangjie.descriptors.impl;
//
//import cn.cangnova.cangjie.descriptors.*;
//import cn.cangnova.cangjie.descriptors.annotations.Annotations;
//import cn.cangnova.cangjie.name.Name;
//import cn.cangnova.cangjie.name.SpecialNames;
//import cn.cangnova.cangjie.types.CangJieType;
//import cn.cangnova.cangjie.types.TypeSubstitutor;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//public class ClassConstructorDescriptorImpl extends FunctionDescriptorImpl implements ClassConstructorDescriptor {
//
//    protected final boolean isPrimary;
//
//    protected ClassConstructorDescriptorImpl(
//            @NotNull ClassDescriptor containingDeclaration,
//            @Nullable ConstructorDescriptor original,
//            @NotNull Annotations annotations,
//            boolean isPrimary,
//            @NotNull Kind kind,
//            @NotNull SourceElement source
//
//    ) {
//
//        super(containingDeclaration, original, annotations, SpecialNames.INIT, kind, source);
//
//
//        this.isPrimary = isPrimary;
//    }
//
//
//
//    @NotNull
//    public static ClassConstructorDescriptorImpl create(
//            @NotNull ClassDescriptor containingDeclaration,
//            @NotNull Annotations annotations,
//            boolean isPrimary,
//            @NotNull SourceElement source
//
//    ) {
//        return create(containingDeclaration, annotations, isPrimary, source, false);
//    }
//
//    @NotNull
//    public static ClassConstructorDescriptorImpl create(
//            @NotNull ClassDescriptor containingDeclaration,
//            @NotNull Annotations annotations,
//            boolean isPrimary,
//            @NotNull SourceElement source,
//            boolean isEnd
//    ) {
//        return new ClassConstructorDescriptorImpl(containingDeclaration, null, annotations, isPrimary, Kind.DECLARATION, source);
//    }
//
//    @NotNull
//    public static ClassConstructorDescriptorImpl createSynthesized(
//            @NotNull ClassDescriptor containingDeclaration,
//            @NotNull Annotations annotations,
//            boolean isPrimary,
//            @NotNull SourceElement source
//    ) {
//        return new ClassConstructorDescriptorImpl(containingDeclaration, null, annotations, isPrimary, Kind.SYNTHESIZED, source);
//    }
//
//    public ClassConstructorDescriptorImpl initialize(
//            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
//            @NotNull DescriptorVisibility visibility,
//            @NotNull List<TypeParameterDescriptor> typeParameterDescriptors
//    ) {
//        super.initialize(
//                null, calculateDispatchReceiverParameter(), calculateContextReceiverParameters(),
//                typeParameterDescriptors,
//                unsubstitutedValueParameters, null,
//                Modality.FINAL, visibility);
//        return this;
//    }
//
//    public ClassConstructorDescriptorImpl initialize(
//            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
//            @NotNull DescriptorVisibility visibility
//    ) {
//        initialize(unsubstitutedValueParameters, visibility, getContainingDeclaration().getDeclaredTypeParameters());
//        return this;
//    }
//
//    @Nullable
//    public ReceiverParameterDescriptor calculateDispatchReceiverParameter() {
//        return null;
//    }
//
//    @NotNull
//    private List<ReceiverParameterDescriptor> calculateContextReceiverParameters() {
//        ClassDescriptor classDescriptor = getContainingDeclaration();
//        if (!classDescriptor.getContextReceivers().isEmpty()) {
//            return classDescriptor.getContextReceivers();
//        }
//        return Collections.emptyList();
//    }
//
//    @NotNull
//    @Override
//    public ClassDescriptor getContainingDeclaration() {
//        return (ClassDescriptor) super.getContainingDeclaration();
//    }
//
//    @NotNull
//    @Override
//    public ClassDescriptor getConstructedClass() {
//        return getContainingDeclaration();
//    }
//
//    @NotNull
//    @Override
//    public ClassConstructorDescriptor getOriginal() {
//        return (ClassConstructorDescriptor) super.getOriginal();
//    }
//
//    @Nullable
//    @Override
//    public ClassConstructorDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
//        return (ClassConstructorDescriptor) super.substitute(originalSubstitutor);
//    }
//
//    @Override
//    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
//        return visitor.visitConstructorDescriptor(this, data);
//    }
//
//    @Override
//    public boolean isPrimary() {
//        return isPrimary;
//    }
//
//    @NotNull
//    @Override
//    public Collection<? extends FunctionDescriptor> getOverriddenDescriptors() {
//        return Collections.emptySet();
//    }
//
//    @Override
//    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
//        assert overriddenDescriptors.isEmpty() : "Constructors cannot override anything";
//    }
//
//    @NotNull
//    @Override
//    protected ClassConstructorDescriptorImpl createSubstitutedCopy(
//            @NotNull DeclarationDescriptor newOwner,
//            @Nullable FunctionDescriptor original,
//            @NotNull Kind kind,
//            @Nullable Name newName,
//            @NotNull Annotations annotations,
//            @NotNull SourceElement source
//    ) {
//        if (kind != Kind.DECLARATION && kind != Kind.SYNTHESIZED) {
//            throw new IllegalStateException("Attempt at creating a constructor that is not a declaration: \n" +
//                    "copy from: " + this + "\n" +
//                    "newOwner: " + newOwner + "\n" +
//                    "kind: " + kind);
//        }
//        assert newName == null : "Attempt to rename constructor: " + this;
//        return new ClassConstructorDescriptorImpl(
//                (ClassDescriptor) newOwner,
//                this,
//                annotations,
//                isPrimary,
//                Kind.DECLARATION,
//                source
//        );
//    }
//
//    @NotNull
//    @Override
//    public ClassConstructorDescriptor copy(
//            DeclarationDescriptor newOwner,
//            Modality modality,
//            DescriptorVisibility visibility,
//            Kind kind,
//            boolean copyOverrides
//    ) {
//        return (ClassConstructorDescriptor) super.copy(newOwner, modality, visibility, kind, copyOverrides);
//    }
//}
