package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor;
import com.huawei.cangjie.descriptors.annotations.AnnotationWithTarget;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.annotations.TargetedAnnotations;
import com.huawei.cangjie.psi.CjAnnotationEntry;
import com.huawei.cangjie.psi.CjTypeReference;
import com.huawei.cangjie.resolve.calls.CallResolver;
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.ErrorUtils;
import com.huawei.cangjie.types.error.ErrorTypeKind;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.JvmSuppressWildcards;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AnnotationResolverImpl extends AnnotationResolver{
    @NotNull private final CallResolver callResolver;
    @NotNull private final StorageManager storageManager;
    @NotNull private TypeResolver typeResolver;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;

    public AnnotationResolverImpl(
            @NotNull CallResolver callResolver,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull StorageManager storageManager
    ) {
        this.callResolver = callResolver;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.storageManager = storageManager;
    }
    @NotNull
    @Override
    protected Annotations resolveAnnotationEntries(@NotNull LexicalScope scope, @NotNull List<CjAnnotationEntry> annotationEntryElements, @NotNull BindingTrace trace, boolean shouldResolveArguments) {

        if (annotationEntryElements.isEmpty()) return Annotations.EMPTY;

        List<AnnotationDescriptor> standard = new ArrayList<>();
        List<AnnotationWithTarget> targeted = new ArrayList<>();

//        for (CjAnnotationEntry entryElement : annotationEntryElements) {
//            AnnotationDescriptor descriptor = trace.get(BindingContext.ANNOTATION, entryElement);
//            if (descriptor == null) {
//                LazyAnnotationDescriptor lazyAnnotationDescriptor =
//                        new LazyAnnotationDescriptor(new LazyAnnotationsContextImpl(this, storageManager, trace, scope), entryElement);
//                descriptor = lazyAnnotationDescriptor;
//            }
//            if (shouldResolveArguments) {
//                ForceResolveUtil.forceResolveAllContents(descriptor);
//            }
//
//            CjAnnotationUseSiteTarget target = entryElement.getUseSiteTarget();
//            if (target != null) {
//                targeted.add(new AnnotationWithTarget(descriptor, target.getAnnotationUseSiteTarget()));
//            }
//            else {
//                standard.add(descriptor);
//            }
//        }
        return new TargetedAnnotations(CollectionsKt.toList(standard), CollectionsKt.toList(targeted));

    }

    @NotNull
    @Override
    public CangJieType resolveAnnotationType(@NotNull LexicalScope scope, @NotNull CjAnnotationEntry entryElement, @NotNull BindingTrace trace) {
        CjTypeReference typeReference = entryElement.getTypeReference();
        if (typeReference == null) {
            return ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, entryElement.getText());
        }

        CangJieType type = typeResolver.resolveType(scope, typeReference, trace, true);
        if (!(type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            return ErrorUtils.createErrorType(ErrorTypeKind.NOT_ANNOTATION_TYPE_IN_ANNOTATION_CONTEXT, type.toString());
        }
        return type;
    }
}
