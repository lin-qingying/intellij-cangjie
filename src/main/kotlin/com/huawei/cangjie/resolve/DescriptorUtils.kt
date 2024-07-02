package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.fqNameUnsafe
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.TypeRefinement
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.REFINER_CAPABILITY

val DeclarationDescriptor.builtIns: CangJieBuiltIns
    get() = module.builtIns
val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

object DescriptorUtils {

    @JvmStatic
    fun getDispatchReceiverParameterIfNeeded(containingDeclaration: DeclarationDescriptor): ReceiverParameterDescriptor? {
        if (containingDeclaration is ClassDescriptor) {
            return containingDeclaration.thisAsReceiverParameter
        }
        return null
    }

    @JvmStatic
    fun isEnumEntry(descriptor: DeclarationDescriptor): Boolean {
        return isKindOf(descriptor, ClassKind.ENUM_ENTRY)
    }

    @JvmStatic
    private fun isKindOf(descriptor: DeclarationDescriptor?, classKind: ClassKind): Boolean {
        return descriptor is ClassDescriptor && descriptor.kind == classKind
    }

    @JvmStatic
    fun getAllDescriptors(scope: MemberScope): Collection<DeclarationDescriptor> {
        return scope.getContributedDescriptors(DescriptorKindFilter.ALL, ALL_NAME_FILTER)
    }

    @JvmStatic
    fun isInterface(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(descriptor, ClassKind.INTERFACE)
    }

    @JvmStatic
    private fun getFqNameSafeIfPossible(descriptor: DeclarationDescriptor): FqName? {
        if ( /*descriptor instanceof ModuleDescriptor || */isError(descriptor)) {
            return FqName.ROOT
        }

        if (descriptor is PackageViewDescriptor) {
            return descriptor.fqName
        } else if (descriptor is PackageFragmentDescriptor) {
            return descriptor.fqName
        }

        return null
    }

    @JvmStatic
    fun getFqName(descriptor: DeclarationDescriptor): FqNameUnsafe {
        val safe = getFqNameSafeIfPossible(descriptor)
        return safe?.toUnsafe() ?: descriptor.fqNameUnsafe
    }

    @JvmStatic
    fun getContainingModule(descriptor: DeclarationDescriptor): ModuleDescriptor {
        val module =
            getContainingModuleOrNull(descriptor)
                ?: error("Descriptor without a containing module: $descriptor")
        return module
    }

    @JvmStatic
    fun getContainingModuleOrNull(descriptor: DeclarationDescriptor?): ModuleDescriptor? {
        var descriptor = descriptor
        while (descriptor != null) {
            if (descriptor is ModuleDescriptor) {
                return descriptor
            }
            if (descriptor is PackageViewDescriptor) {
                return descriptor.module
            }
            descriptor = descriptor.containingDeclaration
        }
        return null
    }

}


@TypeRefinement
fun ModuleDescriptor.getCangJieTypeRefiner(): CangJieTypeRefiner =
    when (val refinerCapability = getCapability(REFINER_CAPABILITY)?.value) {
//        is TypeRefinementSupport.Enabled -> refinerCapability.typeRefiner
        else -> CangJieTypeRefiner.Default
    }