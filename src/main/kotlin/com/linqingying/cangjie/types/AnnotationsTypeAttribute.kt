package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.annotations.Annotations
import kotlin.reflect.KClass
import com.linqingying.cangjie.descriptors.annotations.composeAnnotations
val TypeAttributes.annotations: Annotations get() = annotationsAttribute?.annotations ?: Annotations.EMPTY
val TypeAttributes.annotationsAttribute: AnnotationsTypeAttribute? by TypeAttributes.attributeAccessor<AnnotationsTypeAttribute>()

class AnnotationsTypeAttribute(val annotations: Annotations) : TypeAttribute<AnnotationsTypeAttribute>() {
    override fun union(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? =
        if (other == this) this else null

    override fun intersect(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? =
        if (other == this) this else null

    override fun add(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute {
        if (other == null) return this
        return AnnotationsTypeAttribute(composeAnnotations(annotations, other.annotations))
    }

    override fun isSubtypeOf(other: AnnotationsTypeAttribute?): Boolean = true

    override val key: KClass<out AnnotationsTypeAttribute>
        get() = AnnotationsTypeAttribute::class

    override fun equals(other: Any?): Boolean {
        if (other !is AnnotationsTypeAttribute) return false
        return other.annotations == this.annotations
    }

    override fun hashCode(): Int = annotations.hashCode()
}
