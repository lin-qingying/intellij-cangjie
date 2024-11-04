package com.linqingying.cangjie.descriptors.annotations


/**
 * Use [Annotations.create] to create an instance of this class if necessary.
 */
internal class AnnotationsImpl(private val annotations: List<AnnotationDescriptor>) : Annotations {
    override fun isEmpty(): Boolean = annotations.isEmpty()

    override fun iterator(): Iterator<AnnotationDescriptor> = annotations.iterator()

    override fun toString(): String = annotations.toString()
}
