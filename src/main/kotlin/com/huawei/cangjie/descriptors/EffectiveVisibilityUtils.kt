package com.huawei.cangjie.descriptors

import com.huawei.cangjie.descriptors.Visibilities.Inherited.customEffectiveVisibility
import  com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext
import  com.huawei.cangjie.resolve.isPublishedApi
fun DeclarationDescriptorWithVisibility.effectiveVisibility(
    visibility: DescriptorVisibility = this.visibility,
    checkPublishedApi: Boolean = false
): EffectiveVisibility =
    lowerBound(
        visibility.effectiveVisibility(this, checkPublishedApi),
        (this.containingDeclaration as? ClassDescriptor)?.effectiveVisibility(checkPublishedApi)
            ?: EffectiveVisibility.Public
    )



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
