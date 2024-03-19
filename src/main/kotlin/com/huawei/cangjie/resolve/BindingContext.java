package com.huawei.cangjie.resolve;

import com.google.common.collect.ImmutableMap;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.Diagnostics;

import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.CjElement;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.psi.CjReferenceExpression;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.utils.ReadOnly;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.slicedMap.BasicWritableSlice;
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice;
import com.huawei.cangjie.utils.slicedMap.Slices;
import com.huawei.cangjie.utils.slicedMap.WritableSlice;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Collections;

import static com.huawei.cangjie.utils.slicedMap.RewritePolicy.DO_NOTHING;

public interface BindingContext {
    BindingContext EMPTY = new BindingContext() {
        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            return Diagnostics.Companion.getEMPTY();
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return null;
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            return Collections.emptyList();
        }

        @NotNull
        @TestOnly
        @Override
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            return ImmutableMap.of();
        }

        @Nullable
        @Override
        public CangJieType getType(@NotNull CjExpression expression) {
            return null;
        }

        @Override
        public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
            // Do nothing
        }
    };
    @NotNull
    Diagnostics getDiagnostics();
    @Nullable
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must be true
    @NotNull
    @ReadOnly
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);

    /** This method should be used only for debug and testing */
    @TestOnly
    @NotNull
    <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice);

    @Nullable
    CangJieType getType(@NotNull CjExpression expression);

    void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics);


    WritableSlice<CjReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<CjExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET =
            new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<Call, ResolvedCall<?>> RESOLVED_CALL = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjElement, Call> CALL = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<CjReferenceExpression, PsiElement> LABEL_TARGET = Slices.createSimpleSlice();

    WritableSlice<CjReferenceExpression, Collection<? extends PsiElement>> AMBIGUOUS_LABEL_TARGET = Slices.createSimpleSlice();

    WritableSlice<PsiElement, ClassDescriptor> CLASS = Slices.createSimpleSlice();
    WritableSlice<CjExpression, CangJieTypeInfo> EXPRESSION_TYPE_INFO = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[] {
            CLASS
//            , TYPE_PARAMETER, FUNCTION, CONSTRUCTOR, VARIABLE, VALUE_PARAMETER, PROPERTY_ACCESSOR,
//            PRIMARY_CONSTRUCTOR_PARAMETER, SCRIPT, TYPE_ALIAS
    };


    @SuppressWarnings("unchecked")
    ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR =
            Slices.<PsiElement, DeclarationDescriptor>sliceBuilder()
                    .setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS)
                    .build();
}
