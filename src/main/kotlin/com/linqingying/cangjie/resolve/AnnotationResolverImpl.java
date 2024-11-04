package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.descriptors.ClassDescriptor;
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor;
import com.linqingying.cangjie.descriptors.annotations.AnnotationWithTarget;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.descriptors.annotations.TargetedAnnotations;
import com.linqingying.cangjie.psi.CjAnnotationEntry;
import com.linqingying.cangjie.psi.CjTypeReference;
import com.linqingying.cangjie.resolve.calls.CallResolver;
import com.linqingying.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import com.linqingying.cangjie.storage.StorageManager;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.ErrorUtils;
import com.linqingying.cangjie.types.error.ErrorTypeKind;

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
