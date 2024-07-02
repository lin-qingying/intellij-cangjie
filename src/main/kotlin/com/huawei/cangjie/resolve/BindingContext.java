package com.huawei.cangjie.resolve;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.huawei.cangjie.contracts.model.Computation;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext;
import com.huawei.cangjie.resolve.calls.model.PartialCallContainer;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.resolve.scopes.receivers.Qualifier;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.utils.ReadOnly;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.slicedMap.BasicWritableSlice;
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice;
import com.huawei.cangjie.utils.slicedMap.Slices;
import com.huawei.cangjie.utils.slicedMap.WritableSlice;
import com.intellij.openapi.util.Ref;
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

    WritableSlice<CjTypeReference, CangJieType> TYPE = Slices.createSimpleSlice();
    WritableSlice<DeclarationDescriptor, Multimap<String, ReceiverParameterDescriptor>> DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP = Slices.createSimpleSlice();
    WritableSlice<CjTypeReference,CangJieType> ABBREVIATED_TYPE = Slices.createSimpleSlice();
    WritableSlice<Call, BasicCallResolutionContext> PARTIAL_CALL_RESOLUTION_CONTEXT = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjExpression, Call> DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<CjReferenceExpression, ReceiverParameterDescriptor> THIS_REFERENCE_TARGET = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjElement, Computation> EXPRESSION_EFFECTS = Slices.createSimpleSlice();
    WritableSlice<VariableDescriptor, DataFlowValue> BOUND_INITIALIZER_VALUE = Slices.createSimpleSlice();

    WritableSlice<CjSuperExpression, CangJieType> THIS_TYPE_FOR_SUPER_EXPRESSION = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<Call, PartialCallContainer> ONLY_RESOLVED_CALL = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<CjElement, LexicalScope> LEXICAL_SCOPE = Slices.createSimpleSlice();
    WritableSlice<CjExpression, DataFlowInfo> DATA_FLOW_INFO_BEFORE = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjExpression, CangJieType> EXPECTED_EXPRESSION_TYPE = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<CjExpression, Boolean> PROCESSED = Slices.createSimpleSlice();

    WritableSlice<CjAnnotationEntry, AnnotationDescriptor> ANNOTATION = Slices.createSimpleSlice();
    WritableSlice<FqName, Collection<CjFile>> PACKAGE_TO_FILES = Slices.createSimpleSlice();
    WritableSlice<PsiElement, SimpleFunctionDescriptor> FUNCTION = Slices.createSimpleSlice();
    WritableSlice<CjExpression, Qualifier> QUALIFIER = new BasicWritableSlice<>(DO_NOTHING);

    WritableSlice<CjElement, Call> CALL = new BasicWritableSlice<>(DO_NOTHING);


    WritableSlice<CjReferenceExpression, DeclarationDescriptor> REFERENCE_TARGET = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjExpression, Collection<? extends DeclarationDescriptor>> AMBIGUOUS_REFERENCE_TARGET =
            new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<Call, ResolvedCall<?>> RESOLVED_CALL = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjExpression, Ref<VariableDescriptor>> NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER = Slices.createSimpleSlice();
    WritableSlice<CjReferenceExpression, PsiElement> LABEL_TARGET = Slices.createSimpleSlice();
    WritableSlice<CjReferenceExpression, Collection<? extends PsiElement>> AMBIGUOUS_LABEL_TARGET = Slices.createSimpleSlice();
    WritableSlice<PsiElement, ClassDescriptor> CLASS = Slices.createSimpleSlice();
    WritableSlice<CjExpression, CangJieTypeInfo> EXPRESSION_TYPE_INFO = new BasicWritableSlice<>(DO_NOTHING);
    WritableSlice<CjTypeParameter, TypeParameterDescriptor> TYPE_PARAMETER = Slices.createSimpleSlice();
    WritableSlice<PsiElement, VariableDescriptor> VARIABLE = Slices.createSimpleSlice();
    WritableSlice<CjParameter, VariableDescriptor> VALUE_PARAMETER = Slices.createSimpleSlice();
    WritableSlice<PsiElement, TypeAliasDescriptor> TYPE_ALIAS = Slices.createSimpleSlice();
    WritableSlice[] DECLARATIONS_TO_DESCRIPTORS = new WritableSlice[]{
            CLASS
            , TYPE_PARAMETER, FUNCTION, /*CONSTRUCTOR,*/ VARIABLE, VALUE_PARAMETER, /*PROPERTY_ACCESSOR, PRIMARY_CONSTRUCTOR_PARAMETER, */
            TYPE_ALIAS
    };


    @SuppressWarnings("unchecked")
    ReadOnlySlice<PsiElement, DeclarationDescriptor> DECLARATION_TO_DESCRIPTOR =
            Slices.<PsiElement, DeclarationDescriptor>sliceBuilder()
                    .setFurtherLookupSlices(DECLARATIONS_TO_DESCRIPTORS)
                    .build();





    @SuppressWarnings("UnusedDeclaration")
    @Deprecated // This field is needed only for the side effects of its initializer
    Void _static_initializer = BasicWritableSlice.initSliceDebugNames(BindingContext.class);

    @NotNull
    Diagnostics getDiagnostics();

    @Nullable <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must be true
    @NotNull
    @ReadOnly
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);

    /**
     * This method should be used only for debug and testing
     */
    @TestOnly
    @NotNull <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice);

    @Nullable
    CangJieType getType(@NotNull CjExpression expression);

    void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics);
}
