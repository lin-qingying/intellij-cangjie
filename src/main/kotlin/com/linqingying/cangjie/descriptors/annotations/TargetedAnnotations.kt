package com.linqingying.cangjie.descriptors.annotations


class TargetedAnnotations(
    private val standardAnnotations: List<AnnotationDescriptor>,
    private val targetedAnnotations: List<AnnotationWithTarget>
) : Annotations {
    override fun isEmpty(): Boolean = standardAnnotations.isEmpty() && targetedAnnotations.isEmpty()

    @Deprecated("This method should only be used in frontend where we split annotations according to their use-site targets.")
    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = targetedAnnotations

    override fun iterator(): Iterator<AnnotationDescriptor> = standardAnnotations.iterator()

    override fun toString(): String = (standardAnnotations + targetedAnnotations).toString()
}
