package com.huawei.cangjie.resolve


import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames.FqNames.fromByName
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.descriptors.impl.DescriptorDerivedFromTypeAlias
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.ide.references.mainReference
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.psi.doNotAnalyze
import com.huawei.cangjie.psi.psiUtil.getQualifiedElementSelector
import com.huawei.cangjie.resolve.DescriptorUtils.getContainingClass
import com.huawei.cangjie.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.checker.CangJieTypeCheckerImpl
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.REFINER_CAPABILITY
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.contains
import com.huawei.cangjie.types.util.equalTypesOrNulls
import com.huawei.cangjie.utils.DFS


fun <T : DeclarationDescriptor> T.unwrapIfFakeOverride(): T {
    return if (this is CallableMemberDescriptor) DescriptorUtils.unwrapFakeOverride(this) else this
}

inline fun <reified T : CjDeclaration> reportOnDeclarationAs(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (T) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        (psiElement as? T)?.let {
            trace.report(what(it))
        }
            ?: throw AssertionError("Declaration for $descriptor is expected to be ${T::class.simpleName}, actual declaration: $psiElement")
    } ?: throw AssertionError("No declaration for $descriptor")
}


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
        if (!CangJieBuiltIns.isAny(supertype)) {
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
fun MemberDescriptor.isEffectivelyExternal(): Boolean {
//    if (isExternal) return true
//
//    if (this is PropertyAccessorDescriptor) {
//        val variableDescriptor = correspondingProperty
//        if (variableDescriptor.isEffectivelyExternal()) return true
//    }

//    if (this is PropertyDescriptor) {
//        if (getter?.isExternal == true &&
//            (!isVar || setter?.isExternal == true)
//        ) return true
//    }

    val containingClass = getContainingClass(this)
    return containingClass != null && containingClass.isEffectivelyExternal()
}

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
val DeclarationDescriptor.parents: Sequence<DeclarationDescriptor>
    get() = parentsWithSelf.drop(1)

val DeclarationDescriptor.parentsWithSelf: Sequence<DeclarationDescriptor>
    get() = generateSequence(this, { it.containingDeclaration })

fun ClassDescriptor.findCallableMemberBySignature(
    signature: CallableMemberDescriptor,
    allowOverridabilityConflicts: Boolean = false
): CallableMemberDescriptor? {
    val descriptorKind =
        if (signature is FunctionDescriptor) DescriptorKindFilter.FUNCTIONS else DescriptorKindFilter.VARIABLES
    return defaultType.memberScope
        .getContributedDescriptors(descriptorKind)
        .filterIsInstance<CallableMemberDescriptor>()
        .firstOrNull {
            if (it.containingDeclaration != this) return@firstOrNull false
            val overridability =
                OverridingUtil.DEFAULT.isOverridableBy(it as CallableDescriptor, signature, null).result
            overridability == OVERRIDABLE || (allowOverridabilityConflicts && overridability == CONFLICT)
        }
}

fun CjImportDirective.targetDescriptors(resolutionFacade: ResolutionFacade = this.getResolutionFacade()): Collection<DeclarationDescriptor> {
    // For codeFragments imports are created in dummy file
    if (this.getContainingCjFile().doNotAnalyze != null) return emptyList()
    val nameExpression =
        importedReference?.getQualifiedElementSelector() as? CjSimpleNameExpression ?: return emptyList()
    return nameExpression.mainReference.resolveToDescriptors(resolutionFacade.analyze(nameExpression))
}

fun descriptorsEqualWithSubstitution(
    descriptor1: DeclarationDescriptor?,
    descriptor2: DeclarationDescriptor?,
    checkOriginals: Boolean = true
): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (checkOriginals && descriptor1.original != descriptor2.original) return false
    if (descriptor1 !is CallableDescriptor) return true
    descriptor2 as CallableDescriptor

    val typeChecker = CangJieTypeCheckerImpl.withAxioms(object : CangJieTypeChecker.TypeConstructorEquality {
        override fun equals(a: TypeConstructor, b: TypeConstructor): Boolean {
            val typeParam1 = a.declarationDescriptor as? TypeParameterDescriptor
            val typeParam2 = b.declarationDescriptor as? TypeParameterDescriptor
            if (typeParam1 != null
                && typeParam2 != null
                && typeParam1.containingDeclaration == descriptor1
                && typeParam2.containingDeclaration == descriptor2
            ) {
                return typeParam1.index == typeParam2.index
            }

            return a == b
        }
    })

    if (!typeChecker.equalTypesOrNulls(descriptor1.returnType, descriptor2.returnType)) return false

    val parameters1 = descriptor1.valueParameters
    val parameters2 = descriptor2.valueParameters
    if (parameters1.size != parameters2.size) return false
    for ((param1, param2) in parameters1.zip(parameters2)) {
        if (!typeChecker.equalTypes(param1.type, param2.type)) return false
    }
    return true
}

fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor =
    when (this) {
        is DescriptorDerivedFromTypeAlias -> typeAliasDescriptor
        is ConstructorDescriptor -> containingDeclaration
//        is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }

object DescriptorUtils {
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
        if (classKind == ClassKind.ENUM || classKind.isSingleton) {
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
        ) || classDescriptor.getKind() == ClassKind.ENUM
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

/**
 * When `Inner` is used as type outside of `Outer` class all type arguments should be specified, e.g. `Outer<String, Int>.Inner<Double>`
 * However, it's not necessary inside Outer's members, only the last one should be specified there.
 * So this function return a list of arguments that should be used if relevant arguments weren't specified explicitly inside the [scopeOwner].
 *
 * Examples:
 * for `Outer` class the map will contain: Outer -> (X, Y) (i.e. defaultType mapping)
 * for `Derived` class the map will contain: Derived -> (E), Outer -> (E, String)
 * for `A.B` class the map will contain: B -> (), Outer -> (Int, CharSequence), A -> ()
 *
 * open class Outer<X, Y> {
 *  inner class Inner<Z>
 * }
 *
 * class Derived<E> : Outer<E, String>()
 *
 * class A : Outer<String, Double>() {
 *   inner class B : Outer<Int, CharSequence>()
 * }
 */
//fun findImplicitOuterClassArguments(scopeOwner: ClassDescriptor, outerClass: ClassDescriptor): List<TypeProjection>? {
//    for (current in scopeOwner.classesFromInnerToOuter()) {
//        for (supertype in current.getAllSuperClassesTypesIncludeItself()) {
//            val classDescriptor = supertype.constructor.declarationDescriptor as ClassDescriptor
//            if (classDescriptor == outerClass) return supertype.arguments
//        }
//    }
//
//    return null
//}
//private fun ClassDescriptor.classesFromInnerToOuter() = generateSequence(this) {
//    if (it.isInner)
//        it.containingDeclaration.original as? ClassDescriptor
//    else
//        null
//}
object DeserializedDeclarationsFromSupertypeConflictDataKey : CallableDescriptor.UserDataKey<CallableMemberDescriptor>
