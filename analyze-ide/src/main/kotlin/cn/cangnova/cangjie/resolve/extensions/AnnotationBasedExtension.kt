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

package cn.cangnova.cangjie.resolve.extensions

import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import cn.cangnova.cangjie.psi.CjModifierListOwner
import cn.cangnova.cangjie.resolve.descriptorUtil.annotationClass
import cn.cangnova.cangjie.types.util.TypeUtils


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
