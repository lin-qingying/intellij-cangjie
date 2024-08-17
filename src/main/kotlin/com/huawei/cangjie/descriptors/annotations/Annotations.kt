package com.huawei.cangjie.descriptors.annotations

import com.huawei.cangjie.name.FqName

interface Annotated {
    val annotations: Annotations
}

class CompositeAnnotations(
    private val delegates: List<Annotations>
) : Annotations {
    constructor(vararg delegates: Annotations) : this(delegates.toList())

    override fun isEmpty() = delegates.all { it.isEmpty() }

    override fun hasAnnotation(fqName: FqName) = delegates.asSequence().any { it.hasAnnotation(fqName) }

    override fun findAnnotation(fqName: FqName) =
        delegates.asSequence().mapNotNull { it.findAnnotation(fqName) }.firstOrNull()

    @Suppress("DEPRECATION", "OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun getUseSiteTargetedAnnotations() = delegates.flatMap { it.getUseSiteTargetedAnnotations() }

    override fun iterator() = delegates.asSequence().flatMap { it.asSequence() }.iterator()
}



interface Annotations : Iterable<AnnotationDescriptor> {
    fun isEmpty(): Boolean

    fun findAnnotation(fqName: FqName): AnnotationDescriptor? = firstOrNull { it.fqName == fqName }

    fun hasAnnotation(fqName: FqName): Boolean = findAnnotation(fqName) != null

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This method should only be used in frontend where we split annotations according to their use-site targets.")
    fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = emptyList()

    companion object {
        @JvmField
        val EMPTY: Annotations = object : Annotations {
            override fun isEmpty() = true

            override fun findAnnotation(fqName: FqName) = null

            override fun iterator() = emptyList<AnnotationDescriptor>().iterator()

            override fun toString() = "EMPTY"
        }

        fun create(annotations: List<AnnotationDescriptor>): Annotations =
            if (annotations.isEmpty()) EMPTY else AnnotationsImpl(annotations)
    }
}

class FilteredAnnotations(
    private val delegate: Annotations,
    private val isDefinitelyNewInference: Boolean,
    private val fqNameFilter: (FqName) -> Boolean
) : Annotations {

    constructor(delegate: Annotations, fqNameFilter: (FqName) -> Boolean) : this(delegate, false, fqNameFilter)

    override fun hasAnnotation(fqName: FqName) =
        if (fqNameFilter(fqName)) delegate.hasAnnotation(fqName)
        else false

    override fun findAnnotation(fqName: FqName) =
        if (fqNameFilter(fqName)) delegate.findAnnotation(fqName)
        else null

    override fun iterator() = delegate.filter(this::shouldBeReturned).iterator()

    override fun isEmpty(): Boolean {
        val condition = delegate.any(this::shouldBeReturned)
        // fixing KT-32189 && KT-32138 for the new inference only
        return if (isDefinitelyNewInference) !condition else condition
    }

    private fun shouldBeReturned(annotation: AnnotationDescriptor): Boolean =
        annotation.fqName.let { fqName ->
            fqName != null && fqNameFilter(fqName)
        }
}

fun composeAnnotations(first: Annotations, second: Annotations) =
    when {
        first.isEmpty() -> second
        second.isEmpty() -> first
        else -> CompositeAnnotations(first, second)
    }
