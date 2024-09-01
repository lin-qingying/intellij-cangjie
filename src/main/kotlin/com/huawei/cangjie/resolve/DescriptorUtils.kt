package com.huawei.cangjie.resolve


import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames.FqNames.fromByName
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BuiltInTypeDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils


object DescriptorUtils {
@JvmStatic
    fun isDirectSubclass(
        subClass:  ClassDescriptor,
        superClass:  ClassDescriptor
    ): Boolean {
        for (superType in subClass.getTypeConstructor().getSupertypes()) {
            if ( isSameClass(superType, superClass.original)) {
                return true
            }
        }
        return false
    }
    @JvmStatic

    fun isClassOrEnum(descriptor: DeclarationDescriptor?): Boolean {
        return isClass(descriptor) || isEnum(
            descriptor
        )
    }

    fun canHaveDeclaredConstructors(classDescriptor: ClassDescriptor): Boolean {
        return !isInterface(
            classDescriptor
        )
    }

    @JvmStatic
    fun getDefaultConstructorVisibility(
        classDescriptor: ClassDescriptor,
        freedomForSealedInterfacesSupported: Boolean
    ): DescriptorVisibility {
        val classKind: ClassKind = classDescriptor.getKind()
        if (classKind == ClassKind.ENUM || classKind.isStruct) {
            return DescriptorVisibilities.PRIVATE
        }
        if (isSealedClass(classDescriptor)) {
            return if (freedomForSealedInterfacesSupported) {
                DescriptorVisibilities.PROTECTED
            } else {
                DescriptorVisibilities.PRIVATE
            }
        }
//        if (  isAnonymousObject(classDescriptor)) {
//            return  DescriptorVisibilities.DEFAULT_VISIBILITY
//        }
        assert(classKind == ClassKind.CLASS || classKind == ClassKind.STRUCT || classKind == ClassKind.INTERFACE || classKind == ClassKind.ANNOTATION_CLASS) {
            "Unexpected class kind: $classKind"
        }
        return DescriptorVisibilities.PUBLIC
    }

    /**
     * Given a fake override, finds any declaration of it in the overridden descriptors. Keep in mind that there may be many declarations
     * of the fake override in the supertypes, this method finds just only one of them.
     * TODO: probably some call-sites of this method are wrong, they should handle all super-declarations
     */
    fun <D : CallableMemberDescriptor> unwrapFakeOverride(descriptor: D): D {
        var descriptor = descriptor
        while (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            val overridden: Collection<CallableMemberDescriptor?> =
                descriptor.getOverriddenDescriptors()
            check(!overridden.isEmpty()) { "Fake override should have at least one overridden descriptor: $descriptor" }
            descriptor = overridden.iterator().next() as D
        }
        return descriptor
    }

    @JvmStatic
// WARNING! Don't use this method in JVM backend, use JvmCodegenUtil.isCallInsideSameModuleAsDeclared() instead.
// The latter handles compilation against compiled part of our module correctly.
    fun areInSameModule(
        first: DeclarationDescriptor,
        second: DeclarationDescriptor
    ): Boolean {
        return getContainingModule(first) == getContainingModule(
            second
        )
    }

    fun getContainingClass(descriptor: DeclarationDescriptor): ClassDescriptor? {
        var containing = descriptor.containingDeclaration
        while (containing != null) {
            if (containing is ClassDescriptor
            ) {
                return containing
            }
            containing = containing.containingDeclaration
        }
        return null
    }

    fun classCanHaveAbstractFakeOverride(classDescriptor: ClassDescriptor): Boolean {
        return classCanHaveAbstractDeclaration(classDescriptor) /*|| classDescriptor.isExpect()*/
    }

    @JvmStatic
    fun isSealedClass(descriptor: DeclarationDescriptor?): Boolean {
        return (isKindOf(
            descriptor,
            ClassKind.CLASS
        ) || isKindOf(
            descriptor,
            ClassKind.INTERFACE
        )) && (descriptor as ClassDescriptor).getModality() == Modality.SEALED
    }

    fun classCanHaveAbstractDeclaration(classDescriptor: ClassDescriptor): Boolean {
        return classDescriptor.getModality() == Modality.ABSTRACT || isSealedClass(
            classDescriptor
        ) /*|| classDescriptor.getKind() == ClassKind.ENUM*/
    }

    fun shouldRecordInitializerForProperty(
        variable: VariableDescriptorBase,
        type: CangJieType
    ): Boolean {
        if (variable.isVar || type.isError) return false

        if (TypeUtils.acceptsNullable(type)) return true

        val builtIns: CangJieBuiltIns = variable.builtIns
        return CangJieBuiltIns.isPrimitiveType(type) ||
//                CangJieTypeChecker.DEFAULT.equalTypes(
//                    builtIns.getStringType(),
//                    type
//                ) ||
//                CangJieTypeChecker.DEFAULT.equalTypes(
//                    builtIns.getNumber().getDefaultType(), type
//                ) ||
                CangJieTypeChecker.DEFAULT.equalTypes(builtIns.anyType, type) ||
                UnsignedTypes.isUnsignedType(type)
    }

    @JvmStatic
    fun getContainingSourceFile(descriptor: DeclarationDescriptor): SourceFile {
        var descriptor: DeclarationDescriptor = descriptor
//        if (descriptor is  PropertySetterDescriptor) {
//            descriptor =
//                (descriptor as  PropertySetterDescriptor).getCorrespondingProperty()
//        }

        if (descriptor is DeclarationDescriptorWithSource) {
            return descriptor.getSource()
                .getContainingFile()
        }

        return SourceFile.NO_SOURCE_FILE
    }

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
                current.visibility === DescriptorVisibilities.LOCAL
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

    @JvmStatic
    fun <D : DeclarationDescriptor?> getParentOfType(
        descriptor: DeclarationDescriptor?,
        aClass: Class<D>,
        strict: Boolean
    ): D? {
//        if(descriptor is  BasicTypeDescriptor) return null
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

    fun isSubtypeOfClass(
        type: CangJieType,
        superClass: DeclarationDescriptor
    ): Boolean {
        if (isSameClass(type, superClass)) return true
        for (superType in type.constructor.getSupertypes()) {
            if (isSubtypeOfClass(superType, superClass)) {
                return true
            }
        }
        return false
    }

    private fun isSameClass(
        type: CangJieType,
        other: DeclarationDescriptor
    ): Boolean {
        val descriptor =
            type.constructor.getDeclarationDescriptor()
        if (descriptor != null) {
            val originalDescriptor: DeclarationDescriptor = descriptor.original
            if ((originalDescriptor is ClassifierDescriptor
                        && other is ClassifierDescriptor) && other.getTypeConstructor() == originalDescriptor.getTypeConstructor()
            ) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isSubclass(
        subClass: ClassDescriptor,
        superClass: ClassDescriptor
    ): Boolean {
        return isSubtypeOfClass(
            subClass.getDefaultType(),
            superClass.original
        )
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
        if (descriptor is ModuleDescriptor || isError(descriptor)) {
            return FqName.ROOT
        }
        if (descriptor is BasicTypeDescriptor) {

            return fromByName(descriptor.name)
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
    fun getPackageDeclarationDescriptor(descriptor: DeclarationDescriptor): PackageData {
        return when (descriptor) {
            is PackageFragmentDescriptor -> descriptor
            is PackageViewDescriptor -> descriptor
            is ClassDescriptor -> getPackageDeclarationDescriptor(descriptor.containingDeclaration)

            else -> getPackageDeclarationDescriptor(descriptor.containingDeclaration!!)
        }


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

object DeserializedDeclarationsFromSupertypeConflictDataKey : CallableDescriptor.UserDataKey<CallableMemberDescriptor>
