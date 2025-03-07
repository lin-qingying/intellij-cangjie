/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.descriptors.impl;


import cn.cangnova.cangjie.descriptors.*;
import cn.cangnova.cangjie.descriptors.annotations.Annotations;
import cn.cangnova.cangjie.name.Name;
import cn.cangnova.cangjie.name.SpecialNames;
import cn.cangnova.cangjie.resolve.scopes.receivers.ContextReceiver;
import cn.cangnova.cangjie.resolve.scopes.receivers.ExtensionReceiver;
import cn.cangnova.cangjie.resolve.scopes.receivers.ImplicitContextReceiver;
import cn.cangnova.cangjie.types.*;
import cn.cangnova.cangjie.utils.ReadOnly;
import cn.cangnova.cangjie.utils.SmartSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cn.cangnova.cangjie.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;


public class PropertyDescriptorImpl extends VariableDescriptorWithInitializerImpl implements PropertyDescriptor {
    private final Modality modality;
    private final PropertyDescriptor original;
    private final CallableMemberDescriptor.Kind kind;

    private DescriptorVisibility visibility;
    private Collection<? extends PropertyDescriptor> overriddenProperties = null;
//    private List<ReceiverParameterDescriptor> _contextReceiverParameters = Collections.emptyList();
//    private ReceiverParameterDescriptor dispatchReceiverParameter;
//    private ReceiverParameterDescriptor extensionReceiverParameter;
//    private List<TypeParameterDescriptor> typeParameters;
        private PropertyGetterDescriptorImpl getter;
    private PropertySetterDescriptor setter;
    private boolean setterProjectedOut;
//    private FieldDescriptor backingField;
//    private FieldDescriptor delegateField;

    protected PropertyDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable PropertyDescriptor original,
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

    }

    @Override
    public boolean isStatic() {
        return super.isStatic();
    }

    @NotNull
    public static PropertyDescriptorImpl create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Modality modality,
            @NotNull DescriptorVisibility visibility,
            boolean isVar,
            @NotNull Name name,
            @NotNull CallableMemberDescriptor.Kind kind,
            @NotNull SourceElement source

    ) {
        return new PropertyDescriptorImpl(containingDeclaration, null, annotations,
                modality, visibility, isVar, name, kind, source
 

        );
    }

    private static DescriptorVisibility normalizeVisibility(DescriptorVisibility prev, Kind kind) {
        if (kind == Kind.FAKE_OVERRIDE && DescriptorVisibilities.isPrivate(prev.normalize())) {
            return DescriptorVisibilities.INVISIBLE_FAKE;
        }
        return prev;
    }

    private static ReceiverParameterDescriptor substituteParameterDescriptor(
            TypeSubstitutor substitutor,
            PropertyDescriptor substitutedPropertyDescriptor,
            ReceiverParameterDescriptor receiverParameterDescriptor
    ) {
        CangJieType substitutedType = substitutor.substitute(receiverParameterDescriptor.getType(), Variance.INVARIANT);
        if (substitutedType == null) return null;
        return new ReceiverParameterDescriptorImpl(
                substitutedPropertyDescriptor,
                new ExtensionReceiver(substitutedPropertyDescriptor, substitutedType, receiverParameterDescriptor.getValue()),
                receiverParameterDescriptor.getAnnotations()
        );
    }

    private static ReceiverParameterDescriptor substituteContextParameterDescriptor(
            TypeSubstitutor substitutor,
            PropertyDescriptor substitutedPropertyDescriptor,
            ReceiverParameterDescriptor receiverParameterDescriptor
    ) {
        CangJieType substitutedType = substitutor.substitute(receiverParameterDescriptor.getType(), Variance.INVARIANT);
        if (substitutedType == null) return null;
        return new ReceiverParameterDescriptorImpl(
                substitutedPropertyDescriptor,
                new ContextReceiver(substitutedPropertyDescriptor,
                        substitutedType,
                        ((ImplicitContextReceiver) receiverParameterDescriptor.getValue()).getCustomLabelName(),
                        receiverParameterDescriptor.getValue()),
                receiverParameterDescriptor.getAnnotations()
        );
    }

    private static FunctionDescriptor getSubstitutedInitialSignatureDescriptor(
            @NotNull TypeSubstitutor substitutor,
            @NotNull PropertyAccessorDescriptor accessorDescriptor
    ) {
        return accessorDescriptor.getInitialSignatureDescriptor() != null
                ? accessorDescriptor.getInitialSignatureDescriptor().substitute(substitutor)
                : null;
    }

    @Override
    public CangJieType getInType() {
        return setter != null ? setter.getValueParameters().get(0).getType() : null;

    }

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




    public void initialize(
            @Nullable PropertyGetterDescriptorImpl getter,
            @Nullable PropertySetterDescriptor setter

    ) {
        this.getter = getter;
        this.setter = setter;

    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        List<TypeParameterDescriptor> parameters = super.getTypeParameters();
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

    @Override
    @Nullable
    public PropertyGetterDescriptorImpl getGetter() {
        return getter;
    }

    @Override
    @Nullable
    public PropertySetterDescriptor getSetter() {
        return setter;
    }


    @Override
    public boolean isSetterProjectedOut() {
        return setterProjectedOut;
    }

    public void setSetterProjectedOut(boolean setterProjectedOut) {
        this.setterProjectedOut = setterProjectedOut;
    }


//    @Override
//    public boolean isConst() {
//        return isConst;
//    }

//    @Override
//    public bool isExternal() {
//        return isExternal;
//    }
//
//    @Override
//    public boolean isDelegated() {
//        return isDelegated;
//    }

    @Override
    @NotNull
    public List<PropertyAccessorDescriptor> getAccessors() {
        List<PropertyAccessorDescriptor> result = new ArrayList<PropertyAccessorDescriptor>(2);
        if (getter != null) {
            result.add(getter);
        }
        if (setter != null) {
            result.add(setter);
        }
        return result;
    }

    @Override
    public PropertyDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
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
    private SourceElement getSourceToUseForCopy(boolean preserveSource, @Nullable PropertyDescriptor original) {
        return preserveSource
                ? (original != null ? original : getOriginal()).getSource()
                : SourceElement.NO_SOURCE;
    }

    /**
 * 创建并返回一个属性描述符的替代副本，基于给定的复制配置。
 *
 * @param copyConfiguration 包含复制属性所需的各种配置信息的对象。
 * @return 替代的属性描述符，如果无法创建则返回null。
 */
@Nullable
protected PropertyDescriptor doSubstitute(@NotNull CopyConfiguration copyConfiguration) {
    // 创建替代副本的基本属性
    PropertyDescriptorImpl substitutedDescriptor = createSubstitutedCopy(
            copyConfiguration.owner, copyConfiguration.modality, copyConfiguration.visibility,
            copyConfiguration.original, copyConfiguration.kind, copyConfiguration.name,
            getSourceToUseForCopy(copyConfiguration.preserveSourceElement, copyConfiguration.original));

    // 根据配置确定原始类型参数
    List<TypeParameterDescriptor> originalTypeParameters =
            copyConfiguration.newTypeParameters == null ? getTypeParameters() : copyConfiguration.newTypeParameters;
    // 初始化替代类型参数列表
    List<TypeParameterDescriptor> substitutedTypeParameters = new ArrayList<>(originalTypeParameters.size());
    // 创建类型替换器并进行类型参数替换
    TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(
            originalTypeParameters, copyConfiguration.substitution, substitutedDescriptor, substitutedTypeParameters
    );

    // 获取并替换输出类型
    CangJieType originalOutType = copyConfiguration.returnType;
    CangJieType outType = substitutor.substitute(originalOutType, Variance.INVARIANT);
    if (outType == null) {
        return null; // TODO : 告知用户该属性已被投影出去
    }

    // 替换分派接收器
    ReceiverParameterDescriptor substitutedDispatchReceiver;
    ReceiverParameterDescriptor dispatchReceiver = copyConfiguration.dispatchReceiverParameter;
    if (dispatchReceiver != null) {
        substitutedDispatchReceiver = dispatchReceiver.substitute(substitutor);
        if (substitutedDispatchReceiver == null) return null;
    } else {
        substitutedDispatchReceiver = null;
    }

    // 替换扩展接收器
    ReceiverParameterDescriptor substitutedExtensionReceiver;
    if (getExtensionReceiverParameter() != null) {
        substitutedExtensionReceiver = substituteParameterDescriptor(substitutor, substitutedDescriptor, getExtensionReceiverParameter());
    } else {
        substitutedExtensionReceiver = null;
    }

    // 替换上下文接收器
    List<ReceiverParameterDescriptor> substitutedContextReceivers = new ArrayList<>();
    for (ReceiverParameterDescriptor contextReceiverParameter : getContextReceiverParameters()) {
        ReceiverParameterDescriptor substitutedContextReceiver = substituteContextParameterDescriptor(substitutor, substitutedDescriptor,
                contextReceiverParameter);
        if (substitutedContextReceiver != null) {
            substitutedContextReceivers.add(substitutedContextReceiver);
        }
    }

    // 设置替代描述符的类型信息
    substitutedDescriptor.setType(outType, substitutedTypeParameters, substitutedDispatchReceiver, substitutedExtensionReceiver,
            substitutedContextReceivers);

    // 创建并初始化替代的getter描述符
    PropertyGetterDescriptorImpl newGetter = getter == null ? null : new PropertyGetterDescriptorImpl(
            substitutedDescriptor, getter.getAnnotations(), copyConfiguration.modality, normalizeVisibility(getter.getVisibility(), copyConfiguration.kind),
            getter.isDefault(), copyConfiguration.kind,
            copyConfiguration.getOriginalGetter(),
            SourceElement.NO_SOURCE
    );
    if (newGetter != null) {
        CangJieType returnType = getter.getReturnType();
        newGetter.setInitialSignatureDescriptor(getSubstitutedInitialSignatureDescriptor(substitutor, getter));
        newGetter.initialize(returnType != null ? substitutor.substitute(returnType, Variance.INVARIANT) : null);
    }

    // 创建并初始化替代的setter描述符
    PropertySetterDescriptorImpl newSetter = setter == null ? null : new PropertySetterDescriptorImpl(
            substitutedDescriptor, setter.getAnnotations(), copyConfiguration.modality, normalizeVisibility(setter.getVisibility(), copyConfiguration.kind),
            setter.isDefault(), copyConfiguration.kind,
            copyConfiguration.getOriginalSetter(),
            SourceElement.NO_SOURCE
    );
    if (newSetter != null) {
        List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorImpl.getSubstitutedValueParameters(
                newSetter, setter.getValueParameters(), substitutor, /* dropOriginal = */ false,
                false, null
        );
        if (substitutedValueParameters == null) {
            // 设置setter被投影出去，并创建一个虚拟的setter参数
            substitutedDescriptor.setSetterProjectedOut(true);
            substitutedValueParameters = Collections.singletonList(
                    createSetterParameter(
                            newSetter,
                            getBuiltIns(copyConfiguration.owner).getNothingType(),
                            setter.getValueParameters().get(0).getAnnotations()
                    )
            );
        }
        if (substitutedValueParameters.size() != 1) {
            throw new IllegalStateException();
        }
        newSetter.setInitialSignatureDescriptor(getSubstitutedInitialSignatureDescriptor(substitutor, setter));
        newSetter.initialize(substitutedValueParameters.get(0));
    }

    // 初始化替代描述符的getter和setter
    substitutedDescriptor.initialize(newGetter, newSetter);

    // 如果配置要求，复制重写的描述符
    if (copyConfiguration.copyOverrides) {
        Collection<CallableMemberDescriptor> overridden = SmartSet.create();
        for (PropertyDescriptor propertyDescriptor : getOverriddenDescriptors()) {
            overridden.add(propertyDescriptor.substitute(substitutor));
        }
        substitutedDescriptor.setOverriddenDescriptors(overridden);
    }


    return substitutedDescriptor;
}

    public static ValueParameterDescriptorImpl createSetterParameter(
            @NotNull PropertySetterDescriptor setterDescriptor,
            @NotNull CangJieType type,
            @NotNull Annotations annotations
    ) {
        return new ValueParameterDescriptorImpl(
                setterDescriptor, null, 0, annotations, SpecialNames.IMPLICIT_SET_PARAMETER,false, type,
                /* declaresDefaultValue = */ false,
//                /* isCrossinline = */ false,
//                /* isNoinline = */ false,
               /* null,*/ SourceElement.NO_SOURCE
        );
    }
    @NotNull
    protected PropertyDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @NotNull Modality newModality,
            @NotNull DescriptorVisibility newVisibility,
            @Nullable PropertyDescriptor original,
            @NotNull Kind kind,
            @NotNull Name newName,
            @NotNull SourceElement source
    ) {
        return new PropertyDescriptorImpl(
                newOwner, original, getAnnotations(), newModality, newVisibility, isVar(), newName, kind, source
//
//                isLateInit(), isConst(), isExpect(), isActual(), isExternal()
//                , isDelegated()
        );
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }

    @NotNull
    @Override
    public PropertyDescriptor getOriginal() {
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

    @NotNull
    @Override
    public Collection<? extends PropertyDescriptor> getOverriddenDescriptors() {
        return overriddenProperties != null ? overriddenProperties : Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
        this.overriddenProperties = (Collection<? extends PropertyDescriptor>) overriddenDescriptors;
    }

    @NotNull
    @Override
    public PropertyDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides) {
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




    public class CopyConfiguration implements PropertyDescriptor.CopyBuilder<PropertyDescriptor> {
        private DeclarationDescriptor owner = getContainingDeclaration();
        private Modality modality = getModality();
        private DescriptorVisibility visibility = getVisibility();
        private PropertyDescriptor original = null;
        private boolean preserveSourceElement = false;
        private Kind kind = getKind();
        private TypeSubstitution substitution = TypeSubstitution.EMPTY;
        private boolean copyOverrides = true;
        private ReceiverParameterDescriptor dispatchReceiverParameter = PropertyDescriptorImpl.this.getDispatchReceiverParameter();
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
            this.original = (PropertyDescriptor) original;
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
        public CopyBuilder<PropertyDescriptor> setReturnType(@NotNull CangJieType type) {
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
        public CopyBuilder<PropertyDescriptor> setTypeParameters(@NotNull List<TypeParameterDescriptor> typeParameters) {
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
        public CopyBuilder<PropertyDescriptor> setName(@NotNull Name name) {
            this.name = name;
            return this;
        }

        @Nullable
        @Override
        public PropertyDescriptor build() {
            return doSubstitute(this);
        }

        PropertyGetterDescriptor getOriginalGetter() {
            if (original == null) return null;
            return original.getGetter();
        }

        PropertySetterDescriptor getOriginalSetter() {
            if (original == null) return null;
            return original.getSetter();
        }
    }
}
