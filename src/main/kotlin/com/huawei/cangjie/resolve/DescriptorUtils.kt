package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.DeferredType
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeRefinement
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.REFINER_CAPABILITY
import com.huawei.cangjie.types.util.contains
import com.huawei.cangjie.utils.DFS

@OptIn(TypeRefinement::class)
fun ModuleDescriptor.isTypeRefinementEnabled(): Boolean =
    getCapability(REFINER_CAPABILITY)?.value?.isEnabled == true

fun ModuleDescriptor.resolveClassByFqName(fqName: FqName, lookupLocation: LookupLocation): ClassDescriptor? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent())
        .memberScope.getContributedClassifier(
            fqName.shortName(),
            lookupLocation
        ) as? ClassDescriptor)?.let { return it }

    return resolveClassByFqName(fqName.parent(), lookupLocation)
        ?.unsubstitutedInnerClassesScope
        ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor
}

val DeclarationDescriptor.builtIns: CangJieBuiltIns
    get() = module.builtIns
val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)
val ClassifierDescriptor?.classId: ClassId?
    get() = this?.containingDeclaration?.let { owner ->
        when (owner) {
            is PackageFragmentDescriptor -> ClassId(owner.fqName, name)
            is ClassifierDescriptorWithTypeParameters -> owner.classId?.createNestedClassId(name)
            else -> null
        }
    }

fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor = this
fun ClassDescriptor.getSuperClassNotAny(): ClassDescriptor? {
    for (supertype in defaultType.constructor.supertypes) {
        if (!CangJieBuiltIns.isAnyOrNullableAny(supertype)) {
            val superClassifier = supertype.constructor.declarationDescriptor
            if (DescriptorUtils.isClassOrEnum(superClassifier)) {
                return superClassifier as ClassDescriptor
            }
        }
    }
    return null
}

val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

fun ClassDescriptor.getSuperClassOrAny(): ClassDescriptor = getSuperClassNotAny() ?: builtIns.any


val ClassDescriptor.classValueDescriptor: ClassDescriptor
    get() = this

fun ValueParameterDescriptor.declaresOrInheritsDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::original) },
        ValueParameterDescriptor::declaresDefaultValue
    )
}

val AnnotationDescriptor.annotationClass: ClassDescriptor?
    get() = type.constructor.declarationDescriptor as? ClassDescriptor

object DescriptorUtils {
    @JvmStatic

    fun isClassOrEnum(descriptor: DeclarationDescriptor?): Boolean {
        return isClass(descriptor) || isEnum(
            descriptor
        )
    }
//
//    fun getContainingSourceFile(descriptor:  DeclarationDescriptor):  SourceFile {
//        var descriptor:  DeclarationDescriptor = descriptor
//        if (descriptor is  PropertySetterDescriptor) {
//            descriptor =
//                (descriptor as  PropertySetterDescriptor).getCorrespondingProperty()
//        }
//
//        if (descriptor is  DeclarationDescriptorWithSource) {
//            return (descriptor as  DeclarationDescriptorWithSource).getSource()
//                .getContainingFile()
//        }
//
//        return  SourceFile.NO_SOURCE_FILE
//    }

    private fun getFqNameUnsafe(descriptor: DeclarationDescriptor): FqNameUnsafe {
        val containingDeclaration =
            checkNotNull(descriptor.containingDeclaration) { "Not package/module descriptor doesn't have containing declaration: $descriptor" }
        return getFqName(containingDeclaration).child(descriptor.name)
    }

    fun getFqNameSafe(descriptor: DeclarationDescriptor): FqName {
        return getFqNameSafeIfPossible(descriptor) ?: getFqNameUnsafe(descriptor)
            .toSafe()
    }

    private fun isDescriptorWithLocalVisibility(current: DeclarationDescriptor): Boolean {
        return current is DeclarationDescriptorWithVisibility &&
                current.getVisibility() === DescriptorVisibilities.LOCAL
    }

    @JvmStatic
    private fun <D : CallableDescriptor> collectAllOverriddenDescriptors(
        current: D,
        result: MutableSet<D>
    ) {
        if (result.contains(current)) return
        for (callableDescriptor in current.original.getOverriddenDescriptors()) {
            val descriptor = callableDescriptor.original as D
            collectAllOverriddenDescriptors(descriptor, result)
            result.add(descriptor)
        }
    }

    /**
     * @return original (not substituted) descriptors without any duplicates
     */
    @JvmStatic
    fun <D : CallableDescriptor> getAllOverriddenDescriptors(f: D): MutableSet<D> {
        val result: MutableSet<D> = LinkedHashSet()
        collectAllOverriddenDescriptors<D>(f.original as D, result)
        return result
    }

    /**
     * Descriptor may be local itself or have a local ancestor
     */
    @JvmStatic
    fun isLocal(descriptor: DeclarationDescriptor): Boolean {
        var current: DeclarationDescriptor? = descriptor
        while (current != null) {
            if (/*isAnonymousObject(current) || */isDescriptorWithLocalVisibility(
                    current
                )
            ) {
                return true
            }
            current = current.containingDeclaration
        }
        return false
    }

    @JvmStatic
    fun isTopLevelDeclaration(descriptor: DeclarationDescriptor?): Boolean {
        return descriptor != null && descriptor.containingDeclaration is PackageFragmentDescriptor
    }

    @JvmStatic

    fun <D : DeclarationDescriptor?> getParentOfType(
        descriptor: DeclarationDescriptor?,
        aClass: Class<D>
    ): D? {
        return getParentOfType<D>(descriptor, aClass, true)
    }

    fun <D : DeclarationDescriptor?> getParentOfType(
        descriptor: DeclarationDescriptor?,
        aClass: Class<D>,
        strict: Boolean
    ): D? {
        if (descriptor == null) return null
        var descriptor = descriptor
        if (strict) {
            descriptor = descriptor.containingDeclaration
        }
        while (descriptor != null) {
            if (aClass.isInstance(descriptor)) {
                return descriptor as D
            }
            descriptor = descriptor.containingDeclaration
        }
        return null
    }

    @JvmStatic

    fun isClass(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(
            descriptor,
            ClassKind.CLASS
        )
    }

    fun getClassDescriptorForTypeConstructor(typeConstructor: TypeConstructor): ClassDescriptor {
        val descriptor =
            typeConstructor.getDeclarationDescriptor()
        assert(
            descriptor is ClassDescriptor
        ) { "Classifier descriptor of a type should be of type ClassDescriptor: $typeConstructor" }
        return descriptor as ClassDescriptor
    }

    fun getClassDescriptorForType(type: CangJieType): ClassDescriptor {
        return getClassDescriptorForTypeConstructor(type.constructor)
    }

    @JvmStatic
    fun getSuperClassType(classDescriptor: ClassDescriptor): CangJieType {
        val superclassTypes: Collection<CangJieType> =
            classDescriptor.getTypeConstructor().getSupertypes()
        for (type in superclassTypes) {
            val superClassDescriptor: ClassDescriptor =
                getClassDescriptorForType(type)
            if (superClassDescriptor.getKind() != ClassKind.INTERFACE) {
                return type
            }
        }
        return classDescriptor.builtIns.anyType
    }

    @JvmStatic

    fun isEnum(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(
            descriptor,
            ClassKind.ENUM
        )
    }

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
        if ( descriptor is ModuleDescriptor || isError(descriptor)) {
            return FqName.ROOT
        }
        if(descriptor is BasicTypeDescriptor){
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
        return safe?.toUnsafe() ?: getFqNameUnsafe(descriptor)
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

fun FunctionDescriptor.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnType ?: return false
    if (returnType is DeferredType && returnType.isComputing) return false

    if (returnType.constructor != typeParameter.typeConstructor) return false

    fun CangJieType.isBadType() = contains { it.constructor == typeParameter.typeConstructor }

    return !(valueParameters.any { it.type.isBadType() } || extensionReceiverParameter?.type?.isBadType() == true)
}
