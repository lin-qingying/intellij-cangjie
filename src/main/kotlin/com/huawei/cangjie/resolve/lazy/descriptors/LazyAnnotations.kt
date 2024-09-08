package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjAnnotationEntry
import com.huawei.cangjie.resolve.AnnotationResolver
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.resolve.lazy.LazyEntity
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.AbbreviatedType
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.storage.getValue
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.util.replaceAnnotations
import com.huawei.cangjie.descriptors.annotations.FilteredByPredicateAnnotations
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
    val annotationEntries: List<CjAnnotationEntry>
) : Annotations, LazyEntity {
    private val annotation = c.storageManager.createMemoizedFunction { entry: CjAnnotationEntry ->
        c.trace.get(BindingContext.ANNOTATION, entry) ?: LazyAnnotationDescriptor(c, entry)
    }
    override fun iterator(): Iterator<AnnotationDescriptor> = annotationEntries.asSequence().map(annotation).iterator()


    override fun isEmpty() = annotationEntries.isEmpty()


    override fun forceResolveAllContents() {

    }
}

class LazyAnnotationDescriptor(
    val c: LazyAnnotationsContext,
    val annotationEntry: CjAnnotationEntry
) : AnnotationDescriptor, LazyEntity, ValidateableDescriptor {
    private class FileDescriptorForVisibilityChecks(
        private val source: SourceElement,
        private     val _containingDeclaration: PackageFragmentDescriptor
    ) : DeclarationDescriptorWithSource, PackageFragmentDescriptor by _containingDeclaration     {


        override val annotations: Annotations get() = Annotations.EMPTY
        override fun getSource() = source

        override val original: DeclarationDescriptorWithSource
            get() = this
        override val name: Name
            get() = Name.special("< file descriptor for annotation resolution >")

        private fun error(): Nothing = error("This method should not be called")
        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R = error()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void> ) = error()

        override fun toString(): String = "${name.asString()} declared in LazyAnnotations.kt"
    }
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
                        c.trace.report(Errors.RECURSIVE_TYPEALIAS_EXPANSION.on(annotationEntry, typeAliasDescriptor))
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
        get() = TODO("Not yet implemented")
    override val source: SourceElement
        get() = TODO("Not yet implemented")

    override fun forceResolveAllContents() {
        TODO("Not yet implemented")
    }
}
