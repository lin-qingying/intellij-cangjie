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
