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
import cn.cangnova.cangjie.incremental.components.NoLookupLocation;
import cn.cangnova.cangjie.name.Name;
import cn.cangnova.cangjie.psi.CjFile;
import cn.cangnova.cangjie.resolve.DescriptorUtils;
import cn.cangnova.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor;
import cn.cangnova.cangjie.resolve.scopes.*;
import cn.cangnova.cangjie.storage.NotNullLazyValue;
import cn.cangnova.cangjie.storage.StorageManager;
import cn.cangnova.cangjie.types.*;
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner;
import cn.cangnova.cangjie.types.util.TypeUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractClassDescriptor extends ModuleAwareClassDescriptor {
    protected final NotNullLazyValue<SimpleType> defaultType;
    private final Name name;
    private final NotNullLazyValue<MemberScope> unsubstitutedInnerClassesScope;
    private final NotNullLazyValue<ReceiverParameterDescriptor> thisAsReceiverParameter;

    private final StorageManager storageManager;
    private Set<LazyExtendClassDescriptor> extendClassDescriptor = null;


    public AbstractClassDescriptor(@NotNull StorageManager storageManager, @NotNull Name name) {
        this.storageManager = storageManager;
        this.name = name;
        this.defaultType = storageManager.createLazyValue(new Function0<>() {
            @Override
            public SimpleType invoke() {

                return TypeUtils.makeUnsubstitutedType(
                        AbstractClassDescriptor.this, getUnsubstitutedMemberScope(),
                        new Function1<>() {
                            @Override
                            public SimpleType invoke(CangJieTypeRefiner cangjieTypeRefiner) {
                                ClassifierDescriptor descriptor = cangjieTypeRefiner.refineDescriptor(AbstractClassDescriptor.this);
                                // If we've refined descriptor
                                if (descriptor == null) return defaultType.invoke();

                                if (descriptor instanceof TypeAliasDescriptor) {
                                    return CangJieTypeFactory.computeExpandedType(
                                            (TypeAliasDescriptor) descriptor,
                                            TypeUtils.getDefaultTypeProjections(descriptor.typeConstructor.getParameters())
                                    );
                                }

                                if (descriptor instanceof ModuleAwareClassDescriptor) {
                                    TypeConstructor refinedConstructor = descriptor.typeConstructor.refine(cangjieTypeRefiner);
                                    return TypeUtils.makeUnsubstitutedType(
                                            refinedConstructor,
                                            ((ModuleAwareClassDescriptor) descriptor).getUnsubstitutedMemberScope(cangjieTypeRefiner),
                                            this
                                    );
                                }

                                return descriptor.defaultType;
                            }
                        }
                );
            }
        });
        this.unsubstitutedInnerClassesScope = storageManager.createLazyValue(new Function0<MemberScope>() {
            @Override
            public MemberScope invoke() {
                return new InnerClassesScopeWrapper(getUnsubstitutedMemberScope());
            }
        });
        this.thisAsReceiverParameter = storageManager.createLazyValue(() -> new LazyClassReceiverParameterDescriptor(AbstractClassDescriptor.this));


    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return thisAsReceiverParameter.invoke();
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getContextReceivers() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull DescriptorVisibility getVisibility() {
        return DescriptorVisibilities.PUBLIC;

    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope() {
        return getUnsubstitutedMemberScope(DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));
    }

    public Set<LazyExtendClassDescriptor> getExtendClassDescriptors() {
        if (extendClassDescriptor == null) {
            getExtendClass();
        }
        return extendClassDescriptor;
    }

    /**
     * 获取扩展类型  （权宜之计）
     *
     * @return
     */
    public Set<LazyExtendClassDescriptor> getExtendClass() {
        extendClassDescriptor = new HashSet<>(ScopeUtilsKt.getExtendClasss(getCurrentEditorScope(), name, NoLookupLocation.FROM_PACKAGE));

        return extendClassDescriptor;
    }

    /**
     * 获取相对于活动编辑的 LexicalScope   （权宜之计）
     */
    public LexicalScope getCurrentEditorScope() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(storageManager.getProject());
        Editor editor = fileEditorManager.getSelectedTextEditor();
        PsiFile file = editor == null ? null : PsiDocumentManager.getInstance(storageManager.getProject()).getPsiFile(editor.getDocument());
        if (file instanceof CjFile) {
            return ScopeUtilsKt.getScope((CjFile) file);
        }
        return null;
    }


    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments, @NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        assert typeArguments.size() == typeConstructor.getParameters().size() : "Illegal number of type arguments: expected "
                + typeConstructor.getParameters().size() + " but was " + typeArguments.size()
                + " for " + typeConstructor + " " + typeConstructor.getParameters();
        if (typeArguments.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner);

        TypeSubstitutor substitutor = TypeConstructorSubstitution.create(typeConstructor, typeArguments).buildSubstitutor();
        return new SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution, @NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        if (typeSubstitution.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner);

        TypeSubstitutor substitutor = TypeSubstitutor.create(typeSubstitution);
        return new SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        return getMemberScope(typeArguments, DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
        return getMemberScope(typeSubstitution, DescriptorUtilsKt.getCangJieTypeRefiner(DescriptorUtils.getContainingModule(this)));
    }


    @NotNull
    @Override
    public ClassDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedInnerClassesScope() {
//        throw new UnsupportedOperationException("Should not be called on " + getClass());
        return unsubstitutedInnerClassesScope.invoke();
    }


    //
    @NotNull
    @Override
    public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) {
            return this;
        }
        return new LazySubstitutingClassDescriptor(this, substitutor);
    }

    @NotNull
    @Override
    public SimpleType getDefaultType() {
//        throw new UnsupportedOperationException("Should not be called on " + getClass());
        return defaultType.invoke();

    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitClassDescriptor(this, null);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Nullable
    @Override
    public SimpleType getDefaultFunctionTypeForSamInterface() {
        return null;
    }

    @Override
    public boolean isDefinitelyNotSamInterface() {
        return false;
    }

}
