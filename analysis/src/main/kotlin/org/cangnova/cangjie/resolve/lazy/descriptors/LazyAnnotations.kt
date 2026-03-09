/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.lazy.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.resolve.AnnotationResolver
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.AbbreviatedType
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.descriptors.annotations.FilteredByPredicateAnnotations
import org.cangnova.cangjie.diagnostics.infos.errors.RECURSIVE_TYPEALIAS_EXPANSION
import org.cangnova.cangjie.resolve.AnnotationResolverImpl
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.lazy.LazyEntity
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.replaceAnnotations

abstract class LazyAnnotationsContext(
    val annotationResolver: AnnotationResolver,
    val storageManager: StorageManager,
    val trace: BindingTrace
) {
    abstract val scope: LexicalScope
}

class LazyAnnotationsContextImpl(
    annotationResolver: AnnotationResolver,
    storageManager: StorageManager,
    trace: BindingTrace,
    override val scope: LexicalScope
) : LazyAnnotationsContext(annotationResolver, storageManager, trace)

class LazyAnnotations(
    val c: LazyAnnotationsContext,
    private val annotationEntries: List<CjAnnotation>
) : Annotations, LazyEntity {
    private val annotation = c.storageManager.createMemoizedFunction { entry: CjAnnotation  ->
        c.trace[BindingContext.ANNOTATION, entry] ?: LazyAnnotationDescriptor(c, entry)
    }

    override fun iterator(): Iterator<AnnotationDescriptor> = annotationEntries.asSequence().map(annotation).iterator()


    override fun isEmpty() = annotationEntries.isEmpty()


    override fun forceResolveAllContents() {

    }
}

class LazyAnnotationDescriptor(
    val c: LazyAnnotationsContext,
    private val annotationEntry: CjAnnotation
) : AnnotationDescriptor, LazyEntity, ValidateableDescriptor {
    private class FileDescriptorForVisibilityChecks(
        override val source: SourceElement,
        private val _containingDeclaration: PackageFragmentDescriptor
    ) : DeclarationDescriptorWithSource, PackageFragmentDescriptor by _containingDeclaration {


        override val annotations: Annotations get() = Annotations.EMPTY

        override val original: DeclarationDescriptorWithSource
            get() = this
        override val name: Name
            get() = Name.special("< file descriptor for annotation resolution >")

        private fun error(): Nothing = error("This method should not be called")
        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R = error()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) = error()

        override fun toString(): String = "${name.asString()} declared in LazyAnnotations.cj"
    }
    override val source = annotationEntry.toSourceElement()

    private val scope = (c.scope.ownerDescriptor as? PackageFragmentDescriptor)?.let {
        LexicalScope.Base(c.scope, FileDescriptorForVisibilityChecks(source, it))
    } ?: c.scope
    override val type by c.storageManager.createLazyValue(
        computable = lazy@{
            val annotationType = c.annotationResolver.resolveAnnotationType(scope, annotationEntry, c.trace)
            if (annotationType is AbbreviatedType) {
                // This is needed to prevent recursion in cases like this: typealias S = @S Ann
                if (annotationType.annotations.any { it == this }) {
                    annotationType.abbreviation.constructor.declarationDescriptor?.let { typeAliasDescriptor ->
                        c.trace.report(RECURSIVE_TYPEALIAS_EXPANSION.on(annotationEntry, typeAliasDescriptor))
                    }
                    return@lazy annotationType.replaceAnnotations(FilteredByPredicateAnnotations(annotationType.annotations) { it != this })
                }
            }
            annotationType
        },
        onRecursiveCall = {
            ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_ANNOTATION_TYPE)
        }
    )

    override val allValueArguments: Map<Name, ConstantValue<*>>
        get() = emptyMap()

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(type)
        allValueArguments
    }
}
