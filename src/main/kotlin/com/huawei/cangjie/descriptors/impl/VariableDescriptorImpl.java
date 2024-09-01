package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.scopes.receivers.ContextReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ExtensionReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ImplicitContextReceiver;
import com.huawei.cangjie.types.*;
import com.huawei.cangjie.utils.ReadOnly;
import com.huawei.cangjie.utils.SmartSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class VariableDescriptorImpl extends VariableDescriptorWithInitializerImpl implements VariableDescriptor {
    private final Modality modality;
    private final VariableDescriptor original;
    private final CallableMemberDescriptor.Kind kind;

    private DescriptorVisibility visibility;
    private Collection<? extends  VariableDescriptor> overriddenProperties = null;
    private List<ReceiverParameterDescriptor> contextReceiverParameters = Collections.emptyList();
    private ReceiverParameterDescriptor dispatchReceiverParameter;
    private ReceiverParameterDescriptor extensionReceiverParameter;
    private List<TypeParameterDescriptor> typeParameters;
    //    private PropertyGetterDescriptorImpl getter;
//    private PropertySetterDescriptor setter;

//    private FieldDescriptor backingField;
//    private FieldDescriptor delegateField;

    protected VariableDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable VariableDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Modality modality,
            @NotNull DescriptorVisibility visibility,
            boolean isVar,
            @NotNull Name name,
            @NotNull CallableMemberDescriptor.Kind kind,
            @Nullable CangJieType outType,
            @NotNull SourceElement source


    ) {
        super(containingDeclaration, annotations, name, outType, isVar, source);
        this.modality = modality;
        this.visibility = visibility;
        this.original = original == null ? this : original;
        this.kind = kind;
//        this.lateInit = lateInit;
//        this.isConst = isConst;
//        this.isExpect = isExpect;
//        this.isActual = isActual;
//        this.isExternal = isExternal;

    }
    protected VariableDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable VariableDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Modality modality,
            @NotNull DescriptorVisibility visibility,
            boolean isVar,
            @NotNull Name name,
            @NotNull CallableMemberDescriptor.Kind kind,
            @NotNull SourceElement source


    ) {
        super(containingDeclaration, annotations, name, null, isVar, source);
        this.modality = modality;
        this.visibility = visibility;
        this.original = original == null ? this : original;
        this.kind = kind;
//        this.lateInit = lateInit;
//        this.isConst = isConst;
//        this.isExpect = isExpect;
//        this.isActual = isActual;
//        this.isExternal = isExternal;

    }
    @NotNull
    public static VariableDescriptorImpl create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Modality modality,
            @NotNull DescriptorVisibility visibility,
            boolean isVar,
            @NotNull Name name,
            @NotNull CallableMemberDescriptor.Kind kind,
            @NotNull SourceElement source
//            bool lateInit,
//            bool isConst,
//            bool isExpect,
//            bool isActual,
//            bool isExternal,
//            boolean isDelegated
    ) {
        return new VariableDescriptorImpl(containingDeclaration, null, annotations,
                modality, visibility, isVar, name, kind, source
//                , lateInit, isConst,
//                isExpect, isActual, isExternal

        );
    }

//    private static DescriptorVisibility normalizeVisibility(DescriptorVisibility prev, Kind kind) {
//        if (kind == Kind.FAKE_OVERRIDE && DescriptorVisibilities.isPrivate(prev.normalize())) {
//            return DescriptorVisibilities.INVISIBLE_FAKE;
//        }
//        return prev;
//    }

    private static ReceiverParameterDescriptor substituteParameterDescriptor(
            TypeSubstitutor substitutor,
            VariableDescriptor substitutedVariableDescriptor,
            ReceiverParameterDescriptor receiverParameterDescriptor
    ) {
        CangJieType substitutedType = substitutor.substitute(receiverParameterDescriptor.getType(), Variance.IN_VARIANCE);
        if (substitutedType == null) return null;
        return new ReceiverParameterDescriptorImpl(
                substitutedVariableDescriptor,
                new ExtensionReceiver(substitutedVariableDescriptor, substitutedType, receiverParameterDescriptor.getValue()),
                receiverParameterDescriptor.getAnnotations()
        );
    }

    private static ReceiverParameterDescriptor substituteContextParameterDescriptor(
            TypeSubstitutor substitutor,
            VariableDescriptor substitutedVariableDescriptor,
            ReceiverParameterDescriptor receiverParameterDescriptor
    ) {
        CangJieType substitutedType = substitutor.substitute(receiverParameterDescriptor.getType(), Variance.IN_VARIANCE);
        if (substitutedType == null) return null;
        return new ReceiverParameterDescriptorImpl(
                substitutedVariableDescriptor,
                new ContextReceiver(substitutedVariableDescriptor,
                        substitutedType,
                        ((ImplicitContextReceiver) receiverParameterDescriptor.getValue()).getCustomLabelName(),
                        receiverParameterDescriptor.getValue()),
                receiverParameterDescriptor.getAnnotations()
        );
    }

//    private static FunctionDescriptor getSubstitutedInitialSignatureDescriptor(
//            @NotNull TypeSubstitutor substitutor,
//            @NotNull PropertyAccessorDescriptor accessorDescriptor
//    ) {
//        return accessorDescriptor.getInitialSignatureDescriptor() != null
//                ? accessorDescriptor.getInitialSignatureDescriptor().substitute(substitutor)
//                : null;
//    }


    public void setInType(@NotNull CangJieType inType) {
        /* Do nothing as the corresponding setter is generated by default */
    }

    @kotlin.Deprecated(message = "This method is left for binary compatibility with android.nav.safearg plugin. Used in SafeArgSyntheticDescriptorGenerator.kt")
    public void setType(
            @NotNull CangJieType outType,
            @ReadOnly @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter
    ) {
        setType(outType, typeParameters, dispatchReceiverParameter, extensionReceiverParameter,
                Collections.emptyList());
    }

    public void setType(
            @NotNull CangJieType outType,
            @ReadOnly @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @NotNull List<ReceiverParameterDescriptor> contextReceiverParameters
    ) {
        setOutType(outType);

        this.typeParameters = new ArrayList<TypeParameterDescriptor>(typeParameters);

        this.extensionReceiverParameter = extensionReceiverParameter;
        this.dispatchReceiverParameter = dispatchReceiverParameter;
        this.contextReceiverParameters = contextReceiverParameters;
    }

//    public void initialize(
//            @Nullable PropertyGetterDescriptorImpl getter,
//            @Nullable PropertySetterDescriptor setter
//    ) {
//        initialize(getter, setter, null, null);
//    }

//    public void initialize(
//            @Nullable PropertyGetterDescriptorImpl getter,
//            @Nullable PropertySetterDescriptor setter,
//            @Nullable FieldDescriptor backingField,
//            @Nullable FieldDescriptor delegateField
//    ) {
//        this.getter = getter;
//        this.setter = setter;
//        this.backingField = backingField;
//        this.delegateField = delegateField;
//    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        List<TypeParameterDescriptor> parameters = typeParameters;
        // Diagnostics for EA-212070
        if (parameters == null) {
            throw new IllegalStateException("typeParameters == null for " + this);
        }
        return parameters;
    }

    @Override
    public void validate() {
        getTypeParameters();
    }

    @Override
    @NotNull
    public List<ReceiverParameterDescriptor> getContextReceiverParameters() {
        return contextReceiverParameters;
    }

    @Override
    @Nullable
    public ReceiverParameterDescriptor getExtensionReceiverParameter() {
        return extensionReceiverParameter;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getDispatchReceiverParameter() {
        return dispatchReceiverParameter;
    }

    @NotNull
    @Override
    public CangJieType getReturnType() {
        return getType();
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public DescriptorVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(@NotNull DescriptorVisibility visibility) {
        this.visibility = visibility;
    }

//    @Override
//    @Nullable
//    public PropertyGetterDescriptorImpl getGetter() {
//        return getter;
//    }

//    @Override
//    @Nullable
//    public PropertySetterDescriptor getSetter() {
//        return setter;
//    }


    //    @Override
//    public bool isLateInit() {
//        return lateInit;
//    }
//
//    @Override
//    public bool isConst() {
//        return isConst;
//    }
//
//    @Override
//    public bool isExternal() {
//        return isExternal;
//    }
//
//    @Override
//    public boolean isDelegated() {
//        return isDelegated;
//    }

//    @Override
//    @NotNull
//    public List<PropertyAccessorDescriptor> getAccessors() {
//        List<PropertyAccessorDescriptor> result = new ArrayList<PropertyAccessorDescriptor>(2);
//        if (getter != null) {
//            result.add(getter);
//        }
//        if (setter != null) {
//            result.add(setter);
//        }
//        return result;
//    }

    @Override
    public VariableDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }

        return Objects.requireNonNull(newCopyBuilder()
                .setSubstitution(originalSubstitutor.getSubstitution())
                .setOriginal(getOriginal())
                .build());
    }

    @NotNull
    @Override
    public CopyConfiguration newCopyBuilder() {
        return new CopyConfiguration();
    }

    @NotNull
    private SourceElement getSourceToUseForCopy(boolean preserveSource, @Nullable VariableDescriptor original) {
        return preserveSource
                ? (original != null ? original : getOriginal()).getSource()
                : SourceElement.NO_SOURCE;
    }

    @Nullable
    protected VariableDescriptor doSubstitute(@NotNull CopyConfiguration copyConfiguration) {
        VariableDescriptorImpl substitutedDescriptor = createSubstitutedCopy(
                copyConfiguration.owner, copyConfiguration.modality, copyConfiguration.visibility,
                copyConfiguration.original, copyConfiguration.kind, copyConfiguration.name,
                getSourceToUseForCopy(copyConfiguration.preserveSourceElement, copyConfiguration.original));

        List<TypeParameterDescriptor> originalTypeParameters =
                copyConfiguration.newTypeParameters == null ? getTypeParameters() : copyConfiguration.newTypeParameters;
        List<TypeParameterDescriptor> substitutedTypeParameters = new ArrayList<TypeParameterDescriptor>(originalTypeParameters.size());
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(
                originalTypeParameters, copyConfiguration.substitution, substitutedDescriptor, substitutedTypeParameters
        );

        CangJieType originalOutType = copyConfiguration.returnType;
        CangJieType outType = substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }

        CangJieType inType = substitutor.substitute(originalOutType, Variance.IN_VARIANCE);

        if (inType != null) {
            substitutedDescriptor.setInType(inType);
        }

        ReceiverParameterDescriptor substitutedDispatchReceiver;
        ReceiverParameterDescriptor dispatchReceiver = copyConfiguration.dispatchReceiverParameter;
        if (dispatchReceiver != null) {
            substitutedDispatchReceiver = dispatchReceiver.substitute(substitutor);
            if (substitutedDispatchReceiver == null) return null;
        } else {
            substitutedDispatchReceiver = null;
        }

        ReceiverParameterDescriptor substitutedExtensionReceiver;
        if (extensionReceiverParameter != null) {
            substitutedExtensionReceiver = substituteParameterDescriptor(substitutor, substitutedDescriptor, extensionReceiverParameter);
        } else {
            substitutedExtensionReceiver = null;
        }

        List<ReceiverParameterDescriptor> substitutedContextReceivers = new ArrayList<ReceiverParameterDescriptor>();
        for (ReceiverParameterDescriptor contextReceiverParameter : contextReceiverParameters) {
            ReceiverParameterDescriptor substitutedContextReceiver = substituteContextParameterDescriptor(substitutor, substitutedDescriptor,
                    contextReceiverParameter);
            if (substitutedContextReceiver != null) {
                substitutedContextReceivers.add(substitutedContextReceiver);
            }
        }

        substitutedDescriptor.setType(outType, substitutedTypeParameters, substitutedDispatchReceiver, substitutedExtensionReceiver,
                substitutedContextReceivers);
//
//        PropertyGetterDescriptorImpl newGetter = getter == null ? null : new PropertyGetterDescriptorImpl(
//                substitutedDescriptor, getter.getAnnotations(), copyConfiguration.modality, normalizeVisibility(getter.getVisibility(), copyConfiguration.kind),
//                getter.isDefault(), getter.isExternal(), getter.isInline(), copyConfiguration.kind,
//                copyConfiguration.getOriginalGetter(),
//                SourceElement.NO_SOURCE
//        );
//        if (newGetter != null) {
//            CangJieType returnType = getter.getReturnType();
//            newGetter.setInitialSignatureDescriptor(getSubstitutedInitialSignatureDescriptor(substitutor, getter));
//            newGetter.initialize(returnType != null ? substitutor.substitute(returnType, Variance.OUT_VARIANCE) : null);
//        }
//        PropertySetterDescriptorImpl newSetter = setter == null ? null : new PropertySetterDescriptorImpl(
//                substitutedDescriptor, setter.getAnnotations(), copyConfiguration.modality, normalizeVisibility(setter.getVisibility(), copyConfiguration.kind),
//                setter.isDefault(), setter.isExternal(), setter.isInline(), copyConfiguration.kind,
//                copyConfiguration.getOriginalSetter(),
//                SourceElement.NO_SOURCE
//        );
//        if (newSetter != null) {
//            List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorImpl.getSubstitutedValueParameters(
//                    newSetter, setter.getValueParameters(), substitutor, /* dropOriginal = */ false,
//                    false, null
//            );
//            if (substitutedValueParameters == null) {
//                // The setter is projected out, e.g. in this case:
//                //     trait Tr<T> { var v: T }
//                //     fun test(tr: Tr<out Any?>) { ... }
//                // we want to tell the user that although the property is declared as a var,
//                // it can not be assigned to because of the projection
//                substitutedDescriptor.setSetterProjectedOut(true);
//                substitutedValueParameters = Collections.<ValueParameterDescriptor>singletonList(
//                        PropertySetterDescriptorImpl.createSetterParameter(
//                                newSetter,
//                                getBuiltIns(copyConfiguration.owner).getNothingType(),
//                                setter.getValueParameters().get(0).getAnnotations()
//                        )
//                );
//            }
//            if (substitutedValueParameters.size() != 1) {
//                throw new IllegalStateException();
//            }
//            newSetter.setInitialSignatureDescriptor(getSubstitutedInitialSignatureDescriptor(substitutor, setter));
//            newSetter.initialize(substitutedValueParameters.get(0));
//        }

//        substitutedDescriptor.initialize(
//                newGetter,
//                newSetter,
//                backingField == null ? null : new FieldDescriptorImpl(backingField.getAnnotations(), substitutedDescriptor),
//                delegateField == null ? null : new FieldDescriptorImpl(delegateField.getAnnotations(), substitutedDescriptor)
//        );

        if (copyConfiguration.copyOverrides) {
            Collection<CallableMemberDescriptor> overridden = SmartSet.create();
            for (VariableDescriptor propertyDescriptor : getOverriddenDescriptors()) {
                overridden.add(propertyDescriptor.substitute(substitutor));
            }
            substitutedDescriptor.setOverriddenDescriptors(overridden);
        }

//        if (isConst() && compileTimeInitializerFactory != null) {
//            substitutedDescriptor.setCompileTimeInitializer(compileTimeInitializer, compileTimeInitializerFactory);
//        }

        return substitutedDescriptor;
    }

    @NotNull
    protected VariableDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @NotNull Modality newModality,
            @NotNull DescriptorVisibility newVisibility,
            @Nullable VariableDescriptor original,
            @NotNull Kind kind,
            @NotNull Name newName,
            @NotNull SourceElement source
    ) {
        return new VariableDescriptorImpl(
                newOwner, original, getAnnotations(), newModality, newVisibility, isVar(), newName, kind, source
//
//                isLateInit(), isConst(), isExpect(), isActual(), isExternal()
//                , isDelegated()
        );
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitVariableDescriptor(this, data);
    }

    @NotNull
    @Override
    public VariableDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @NotNull
    @Override
    public CallableMemberDescriptor.Kind getKind() {
        return kind;
    }



//    @Override
//    public bool isExpect() {
//        return isExpect;
//    }
//
//    @Override
//    public bool isActual() {
//        return isActual;
//    }
//
//    @Override
//    @Nullable
//    public FieldDescriptor getBackingField() {
//        return backingField;
//    }
//
//    @Override
//    @Nullable
//    public FieldDescriptor getDelegateField() {
//        return delegateField;
//    }

@Override

    public @NotNull Collection<? extends VariableDescriptor> getOverriddenDescriptors() {
        return overriddenProperties != null ? (Collection<VariableDescriptor>) overriddenProperties : Collections.emptyList();
    }
//


    @Override
    @SuppressWarnings("unchecked")
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
        this.overriddenProperties = (Collection<? extends VariableDescriptor>) overriddenDescriptors;
    }

    @NotNull
    @Override
    public VariableDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides) {
        //noinspection ConstantConditions
        return newCopyBuilder()
                .setOwner(newOwner)
                .setOriginal(null)
                .setModality(modality)
                .setVisibility(visibility)
                .setKind(kind)
                .setCopyOverrides(copyOverrides)
                .build();
    }


    @Nullable
    @Override
    public <V> V getUserData(UserDataKey<V> key) {
        return null;
    }


    public class CopyConfiguration implements VariableDescriptor.CopyBuilder<VariableDescriptor> {
        private DeclarationDescriptor owner = getContainingDeclaration();
        private Modality modality = getModality();
        private DescriptorVisibility visibility = getVisibility();
        private VariableDescriptor original = null;
        private boolean preserveSourceElement = false;
        private Kind kind = getKind();
        private TypeSubstitution substitution = TypeSubstitution.EMPTY;
        private boolean copyOverrides = true;
        private ReceiverParameterDescriptor dispatchReceiverParameter = VariableDescriptorImpl.this.dispatchReceiverParameter;
        private List<TypeParameterDescriptor> newTypeParameters = null;
        private Name name = getName();
        private CangJieType returnType = getType();

        @NotNull
        @Override
        public CopyConfiguration setOwner(@NotNull DeclarationDescriptor owner) {
            this.owner = owner;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setOriginal(@Nullable CallableMemberDescriptor original) {
            this.original = (VariableDescriptor) original;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setPreserveSourceElement() {
            preserveSourceElement = true;
            return this;
        }

        @NotNull
        @Override
        public CopyBuilder<VariableDescriptor> setReturnType(@NotNull CangJieType type) {
            returnType = type;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setModality(@NotNull Modality modality) {
            this.modality = modality;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setVisibility(@NotNull DescriptorVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setKind(@NotNull Kind kind) {
            this.kind = kind;
            return this;
        }

        @NotNull
        @Override
        public CopyBuilder<VariableDescriptor> setTypeParameters(@NotNull List<TypeParameterDescriptor> typeParameters) {
            this.newTypeParameters = typeParameters;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter) {
            this.dispatchReceiverParameter = dispatchReceiverParameter;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setSubstitution(@NotNull TypeSubstitution substitution) {
            this.substitution = substitution;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setCopyOverrides(boolean copyOverrides) {
            this.copyOverrides = copyOverrides;
            return this;
        }

        @NotNull
        @Override
        public CopyBuilder<VariableDescriptor> setName(@NotNull Name name) {
            this.name = name;
            return this;
        }

        @Nullable
        @Override
        public VariableDescriptor build() {
            return doSubstitute(this);
        }

//        PropertyGetterDescriptor getOriginalGetter() {
//            if (original == null) return null;
//            return original.getGetter();
//        }
//
//        PropertySetterDescriptor getOriginalSetter() {
//            if (original == null) return null;
//            return original.getSetter();
//        }
    }
}
