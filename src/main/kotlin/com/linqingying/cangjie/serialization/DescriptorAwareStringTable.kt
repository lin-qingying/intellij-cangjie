package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.metadata.serialization.StringTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.resolve.descriptorUtil.classId
import com.linqingying.cangjie.types.ErrorUtils


interface DescriptorAwareStringTable : StringTable {
    fun getQualifiedClassNameIndex(classId: ClassId): Int =
        getQualifiedClassNameIndex(classId.asString(), classId.isLocal)

    fun getFqNameIndex(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        if (ErrorUtils.isError(descriptor)) {
            throw IllegalStateException("Cannot get FQ name of error class: ${renderDescriptor(descriptor)}")
        }

        val classId = descriptor.classId
            ?: getLocalClassIdReplacement(descriptor)
            ?: throw IllegalStateException("Cannot get FQ name of local class: ${renderDescriptor(descriptor)}")

        return getQualifiedClassNameIndex(classId)
    }

    fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? = null

    /**
     * true if this [StringTable] replaces absent [ClassId] of a local class descriptor with a semantic equivalent that
     * still expects original type arguments when used in a type
     * false otherwise
     */
    val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = false

    private fun renderDescriptor(descriptor: ClassifierDescriptorWithTypeParameters): String =
        DescriptorRenderer.COMPACT.render(descriptor) + " defined in " +
                DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor.containingDeclaration)
}
