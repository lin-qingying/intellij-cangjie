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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.Visibilities.Inherited.customEffectiveVisibility
import org.cangnova.cangjie.resolve.DescriptorUtils
import  org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext
import org.cangnova.cangjie.types.CangJieType

fun DeclarationDescriptorWithVisibility.effectiveVisibility(
    visibility: DescriptorVisibility = this.visibility,
    checkPublishedApi: Boolean = false
): EffectiveVisibility =
    lowerBound(
        visibility.effectiveVisibility(this, checkPublishedApi),
        (this.containingDeclaration as? ClassDescriptor)?.effectiveVisibility(checkPublishedApi)
            ?: EffectiveVisibility.Public
    )


data class DescriptorWithRelation(val descriptor: ClassifierDescriptor, private val relation: RelationToType) {
    fun effectiveVisibility() =
        (descriptor as? ClassDescriptor)?.visibility?.effectiveVisibility(descriptor, false) ?: EffectiveVisibility.Public

    override fun toString() = "$relation ${descriptor.name}"
}

private fun ClassDescriptor.effectiveVisibility(classes: Set<ClassDescriptor>, checkPublishedApi: Boolean): EffectiveVisibility =
    if (this in classes) EffectiveVisibility.Public
    else with(this.containingDeclaration as? ClassDescriptor) {
        lowerBound(
            visibility.effectiveVisibility(this@effectiveVisibility, checkPublishedApi),
            this?.effectiveVisibility(classes + this@effectiveVisibility, checkPublishedApi) ?: EffectiveVisibility.Public
        )
    }
fun DescriptorVisibility.effectiveVisibility(
    descriptor: DeclarationDescriptor,
    checkPublishedApi: Boolean = false
): EffectiveVisibility {
    return customEffectiveVisibility() ?: normalize().forVisibility(descriptor, checkPublishedApi)
}

private fun lowerBound(first: EffectiveVisibility, second: EffectiveVisibility): EffectiveVisibility {
    return first.lowerBound(second, SimpleClassicTypeSystemContext)
}

fun ClassDescriptor.effectiveVisibility(checkPublishedApi: Boolean = false) =
    effectiveVisibility(emptySet(), checkPublishedApi)

private fun DescriptorVisibility.forVisibility(
    descriptor: DeclarationDescriptor,
    checkPublishedApi: Boolean = false
): EffectiveVisibility =
    when (this) {
        DescriptorVisibilities.PRIVATE_TO_THIS, DescriptorVisibilities.INVISIBLE_FAKE -> EffectiveVisibility.PrivateInClass
        DescriptorVisibilities.PRIVATE -> if (descriptor is ClassDescriptor &&
            descriptor.containingDeclaration is PackageFragmentDescriptor
        ) EffectiveVisibility.PrivateInFile else EffectiveVisibility.PrivateInClass

        DescriptorVisibilities.PROTECTED -> EffectiveVisibility.Protected(
            (descriptor.containingDeclaration as? ClassDescriptor)?.defaultType?.constructor
        )

        DescriptorVisibilities.INTERNAL -> if (!checkPublishedApi ||
            !descriptor.isPublishedApi()
        ) EffectiveVisibility.Internal else EffectiveVisibility.Public

        DescriptorVisibilities.PUBLIC -> EffectiveVisibility.Public
        DescriptorVisibilities.LOCAL -> EffectiveVisibility.Local
        // NB: visibility must be already normalized here, so e.g. no JavaVisibilities are possible at this point
        else -> throw AssertionError("Visibility $name is not allowed in forVisibility")
    }
fun CangJieType.leastPermissiveDescriptor(base: EffectiveVisibility) = dependentDescriptors().leastPermissive(base)
// Should collect all dependent classifier descriptors, to get verbose diagnostic
private fun CangJieType.dependentDescriptors() = dependentDescriptors(emptySet(), RelationToType.CONSTRUCTOR)
private fun CangJieType.dependentDescriptors(types: Set<CangJieType>, ownRelation: RelationToType): Set<DescriptorWithRelation> {
    if (this in types) return emptySet()
    val ownDependent = constructor.declarationDescriptor?.dependentDescriptors(ownRelation) ?: emptySet()
    val argumentDependent = arguments.map { it.type.dependentDescriptors(types + this, RelationToType.ARGUMENT) }.flatten()
    return ownDependent + argumentDependent
}
private fun ClassifierDescriptor.dependentDescriptors(ownRelation: RelationToType): Set<DescriptorWithRelation> =
    setOf(DescriptorWithRelation(this, ownRelation)) +
            ((this.containingDeclaration as? ClassifierDescriptor)?.dependentDescriptors(ownRelation.containerRelation()) ?: emptySet())
private fun Set<DescriptorWithRelation>.leastPermissive(base: EffectiveVisibility): DescriptorWithRelation? {
    for (descriptorWithRelation in this) {
        val currentVisibility = descriptorWithRelation.effectiveVisibility()
        when (currentVisibility.relation(base, SimpleClassicTypeSystemContext)) {
            EffectiveVisibility.Permissiveness.LESS, EffectiveVisibility.Permissiveness.UNKNOWN -> {
                return descriptorWithRelation
            }
            else -> {
            }
        }
    }
    return null
}




//===========================================================


fun DeclarationDescriptor.isPublishedApi(): Boolean {
    val descriptor = if (this is CallableMemberDescriptor) DescriptorUtils.getDirectMember(this) else this
    return descriptor.annotations.hasAnnotation(StandardNames.FqNames.publishedApi)
}