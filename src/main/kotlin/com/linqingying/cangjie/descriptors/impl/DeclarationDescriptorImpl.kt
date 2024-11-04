package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptorVisitor
import com.linqingying.cangjie.descriptors.annotations.AnnotatedImpl
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.renderer.DescriptorRenderer

abstract class DeclarationDescriptorImpl(
    annotations: Annotations,
    override val  name: Name
):  AnnotatedImpl(annotations), DeclarationDescriptor {
    override val original: DeclarationDescriptor
        get() = this
    override fun toString(): String {
        return toString(this)
    }

    fun toString(descriptor: DeclarationDescriptor): String {
        return try {
        DescriptorRenderer.  DEBUG_TEXT.render(descriptor) +
                    "[" + descriptor.javaClass.simpleName + "@" + Integer.toHexString(
                System.identityHashCode(
                    descriptor
                )
            ) + "]"
        } catch (e: Throwable) {
            // DescriptionRenderer may throw if this is not yet completely initialized
            // It is very inconvenient while debugging
            descriptor.javaClass.getSimpleName() + " " + descriptor.name
        }
    }
    override fun acceptVoid(visitor:  DeclarationDescriptorVisitor<Void, Void>) {
        accept(visitor, null)
    }

}
