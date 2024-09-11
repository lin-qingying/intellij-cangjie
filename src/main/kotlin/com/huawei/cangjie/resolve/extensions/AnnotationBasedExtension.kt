package com.huawei.cangjie.resolve.extensions

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.psi.CjModifierListOwner
import com.huawei.cangjie.resolve.descriptorUtil.annotationClass
import com.huawei.cangjie.types.util.TypeUtils


interface AnnotationBasedExtension {

    fun getAnnotationFqNames(modifierListOwner: CjModifierListOwner?): List<String>

    fun DeclarationDescriptor.hasSpecialAnnotation(modifierListOwner: CjModifierListOwner?): Boolean {
        val specialAnnotations = getAnnotationFqNames(modifierListOwner).takeIf { it.isNotEmpty() } ?: return false

        if (annotations.any { it.isASpecialAnnotation(specialAnnotations) }) return true

        if (this is ClassDescriptor) {
            for (superType in TypeUtils.getAllSupertypes(defaultType)) {
                val superTypeDescriptor = superType.constructor.declarationDescriptor as? ClassDescriptor ?: continue
                if (superTypeDescriptor.annotations.any { it.isASpecialAnnotation(specialAnnotations) }) return true
            }
        }

        return false
    }

    private fun AnnotationDescriptor.isASpecialAnnotation(
        specialAnnotations: List<String>,
        visitedAnnotations: MutableSet<String> = hashSetOf(),
        allowMetaAnnotations: Boolean = true
    ): Boolean {
        val annotationFqName = fqName?.asString() ?: return false
        if (annotationFqName in visitedAnnotations) return false // Prevent infinite recursion
        if (annotationFqName in specialAnnotations) return true

        visitedAnnotations.add(annotationFqName)

        if (allowMetaAnnotations) {
            val annotationType = annotationClass ?: return false
            for (metaAnnotation in annotationType.annotations) {
                if (metaAnnotation.isASpecialAnnotation(specialAnnotations, visitedAnnotations, allowMetaAnnotations = true)) {
                    return true
                }
            }
        }

        visitedAnnotations.remove(annotationFqName)

        return false
    }
}
