package com.huawei.cangjie.resolve


import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.builtins.StandardNames.FqNames.fromByName
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.huawei.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import com.huawei.cangjie.descriptors.impl.PropertyAccessorDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.ide.IdeDescriptorRenderers
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.*
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.DescriptorUtils.getContainingModule
import com.huawei.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl
import com.huawei.cangjie.resolve.lazy.descriptors.LazyEnumEntryDescriptor
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.DFS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun DeclarationDescriptor.unwrapIfTypeAlias(): DeclarationDescriptor? =
    when (this) {
        is TypeAliasDescriptor -> this.classDescriptor?.unwrapIfTypeAlias()
        else -> this
    }

fun DeclarationDescriptor.isPublishedApi(): Boolean {
    val descriptor = if (this is CallableMemberDescriptor) DescriptorUtils.getDirectMember(this) else this
    return descriptor.annotations.hasAnnotation(StandardNames.FqNames.publishedApi)
}

val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && extensionReceiverParameter != null

fun <D : CallableMemberDescriptor> D.getDirectlyOverriddenDeclarations(): Collection<D> {
    val result = java.util.LinkedHashSet<D>()
    for (overriddenDescriptor in overriddenDescriptors) {
        @Suppress("UNCHECKED_CAST")
        when (overriddenDescriptor.kind) {
            CallableMemberDescriptor.Kind.DECLARATION -> result.add(overriddenDescriptor as D)
            CallableMemberDescriptor.Kind.FAKE_OVERRIDE, CallableMemberDescriptor.Kind.DELEGATION -> result.addAll((overriddenDescriptor as D).getDirectlyOverriddenDeclarations())
            CallableMemberDescriptor.Kind.SYNTHESIZED -> {
                //do nothing
            }

            else -> throw AssertionError("Unexpected callable kind ${overriddenDescriptor.kind}: $overriddenDescriptor")
        }
    }
    return OverridingUtil.filterOutOverridden(result)
}

fun DeclarationDescriptorWithVisibility.isVisible(
    context: PsiElement,
    receiverExpression: CjExpression?,
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade
): Boolean {
    val resolutionScope = context.getResolutionScope(bindingContext, resolutionFacade)
    val from = resolutionScope.ownerDescriptor
    return isVisible(
        from,
        receiverExpression,
        bindingContext,
        resolutionScope,
        resolutionFacade.languageVersionSettings
    )
}

private fun DeclarationDescriptorWithVisibility.isVisible(
    from: DeclarationDescriptor,
    receiverExpression: CjExpression?,
    bindingContext: BindingContext? = null,
    resolutionScope: LexicalScope? = null,
    languageVersionSettings: LanguageVersionSettings
): Boolean {
    if (DescriptorVisibilityUtils.isVisibleWithAnyReceiver(this, from, languageVersionSettings)) return true

    if (bindingContext == null || resolutionScope == null) return false

    // for extension, it makes no sense to check explicit receiver because we need dispatch receiver which is implicit in this case
    if (receiverExpression != null && !isExtension) {
        val receiverType = bindingContext.getType(receiverExpression) ?: return false
        val explicitReceiver = ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
        return DescriptorVisibilityUtils.isVisible(explicitReceiver, this, from, languageVersionSettings)
    } else {
        return resolutionScope.getImplicitReceiversHierarchy().any {
            DescriptorVisibilityUtils.isVisible(it.value, this, from, languageVersionSettings)
        }
    }
}

val ClassDescriptor.hasClassValueDescriptor: Boolean get() = classValueDescriptor != null
val ClassDescriptor.isEnumEntry:Boolean get() = when(this){
    is EnumEntryDescriptor -> true
    is LazySubstitutingClassDescriptor -> {
        original is EnumEntryDescriptor
    }
    else -> false
}
val ClassDescriptor.classValueDescriptor: ClassDescriptor?
    get() =
        if(kind.isSingleton && isEnumEntry){
            this
        }else{
            null
        }
//        if (kind.isSingleton && ((this is LazyEnumEntryDescriptor && this.hasUnsubstitutedPrimaryConstructor()) || (this is LazySubstitutingClassDescriptor && original is LazyEnumEntryDescriptor && original.hasUnsubstitutedPrimaryConstructor())))
//            this
//        else
//            null

/**
 * Returns containing declaration of dispatch receiver for callable adjusted to fake-overridden cases
 *
 * open class A {
 *   fun foo() = 1
 * }
 * class B : A()
 *
 * for A.foo -> returns A (dispatch receiver parameter is A)
 * for B.foo -> returns B (dispatch receiver parameter is still A, but it's fake-overridden in B, so it's containing declaration is B)
 *
 * class Outer {
 *   inner class Inner()
 * }
 *
 * for constructor of Outer.Inner -> returns Outer (dispatch receiver parameter is Outer, but it's containing declaration is Inner)
 *
 */
fun CallableDescriptor.getOwnerForEffectiveDispatchReceiverParameter(): DeclarationDescriptor? {
    if (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return containingDeclaration
    }
    return dispatchReceiverParameter?.containingDeclaration
}

fun CallableMemberDescriptor.firstOverridden(
    useOriginal: Boolean = false,
    predicate: (CallableMemberDescriptor) -> Boolean
): CallableMemberDescriptor? {
    var result: CallableMemberDescriptor? = null
    return DFS.dfs(listOf(this),
        { current ->
            val descriptor = if (useOriginal) current?.original else current
            (descriptor?.overriddenDescriptors ?: emptyList()) as MutableIterable<CallableMemberDescriptor>
        },
        object : DFS.AbstractNodeHandler<CallableMemberDescriptor, CallableMemberDescriptor?>() {
            override fun beforeChildren(current: CallableMemberDescriptor) = result == null
            override fun afterChildren(current: CallableMemberDescriptor) {
                if (result == null && predicate(current)) {
                    result = current
                }
            }

            override fun result(): CallableMemberDescriptor? = result
        }
    )
}

val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

fun TypeConstructor.supertypesWithAny(): Collection<CangJieType> {
    val supertypes = supertypes
    val noSuperClass = supertypes.map { it.constructor.declarationDescriptor as? ClassDescriptor }.all {
        it == null || it.kind == ClassKind.INTERFACE
    }
    return if (noSuperClass) supertypes + builtIns.anyType else supertypes
}

fun <D : CallableDescriptor> D.overriddenTreeUniqueAsSequence(useOriginal: Boolean): Sequence<D> {
    val set = hashSetOf<D>()

    @Suppress("UNCHECKED_CAST")
    fun D.doBuildOverriddenTreeAsSequence(): Sequence<D> {
        return with(if (useOriginal) original as D else this) {
            if (original in set)
                emptySequence()
            else {
                set += original as D
                sequenceOf(this) + (overriddenDescriptors as Collection<D>).asSequence()
                    .flatMap { it.doBuildOverriddenTreeAsSequence() }
            }
        }
    }

    return doBuildOverriddenTreeAsSequence()
}


val DeclarationDescriptorWithVisibility.isEffectivelyPublicApi: Boolean
    get() = effectiveVisibility().publicApi

val ClassifierDescriptorWithTypeParameters.constructors: Collection<ConstructorDescriptor>
    get() = when (this) {
        is TypeAliasDescriptor -> this.constructors
        is ClassDescriptor -> this.constructors
        else -> emptyList()
    }
val ClassifierDescriptorWithTypeParameters.kind: ClassKind?
    get() = when (this) {
        is TypeAliasDescriptor -> classDescriptor?.kind
        is ClassDescriptor -> kind
        else -> null
    }


fun DescriptorVisibility.toKeywordToken(): CjModifierKeywordToken = when (val normalized = normalize()) {
    DescriptorVisibilities.PUBLIC -> CjTokens.PUBLIC_KEYWORD
    DescriptorVisibilities.PROTECTED -> CjTokens.PROTECTED_KEYWORD
    DescriptorVisibilities.INTERNAL -> CjTokens.INTERNAL_KEYWORD
    else -> {
        if (DescriptorVisibilities.isPrivate(normalized)) {
            CjTokens.PRIVATE_KEYWORD
        } else {
            error("Unexpected visibility '$normalized'")
        }
    }
}

object DescriptorUtils {
    fun getFqNameFromTopLevelClass(descriptor: DeclarationDescriptor): FqName {
        val containingDeclaration =
            descriptor.containingDeclaration
        val name: Name = descriptor.name
        if (containingDeclaration !is ClassDescriptor) {
            return FqName.topLevel(name)
        }
        return getFqNameFromTopLevelClass(containingDeclaration).child(name)
    }

    fun isOverride(descriptor: CallableMemberDescriptor): Boolean {
        return !descriptor.getOverriddenDescriptors().isEmpty()
    }

    fun <D : CallableMemberDescriptor?> getAllOverriddenDeclarations(memberDescriptor: D): Set<D> {
        val result: MutableSet<D> = HashSet()
        for (overriddenDeclaration in memberDescriptor?.getOverriddenDescriptors() ?: emptyList()) {
            val kind: CallableMemberDescriptor.Kind = overriddenDeclaration.getKind()
            if (kind == CallableMemberDescriptor.Kind.DECLARATION) {
                result.add(overriddenDeclaration as D)
            } else if (kind == CallableMemberDescriptor.Kind.DELEGATION || kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE || kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
                //do nothing
            } else {
                throw java.lang.AssertionError("Unexpected callable kind $kind")
            }
            result.addAll(getAllOverriddenDeclarations(overriddenDeclaration as D))
        }
        return result
    }

    fun isAncestor(
        ancestor: DeclarationDescriptor?,
        declarationDescriptor: DeclarationDescriptor,
        strict: Boolean
    ): Boolean {
        if (ancestor == null) return false
        var descriptor =
            if (strict) declarationDescriptor.containingDeclaration else declarationDescriptor
        while (descriptor != null) {
            if (ancestor === descriptor) return true
            descriptor = descriptor.containingDeclaration
        }
        return false
    }

    fun getDirectMember(descriptor: CallableMemberDescriptor): CallableMemberDescriptor {
        return if (descriptor is PropertyAccessorDescriptor)
            descriptor.correspondingProperty
        else
            descriptor
    }

    /**
     * @return true iff this is a top-level declaration or a class member with no expected "this" object
     */
    fun isStaticDeclaration(descriptor: CallableDescriptor): Boolean {
        if (descriptor is ConstructorDescriptor) return false

        val container: DeclarationDescriptor = descriptor.containingDeclaration
        return container is PackageFragmentDescriptor ||
                (container is ClassDescriptor && descriptor.getDispatchReceiverParameter() == null)
    }

    @JvmStatic
    fun isDirectSubclass(
        subClass: ClassDescriptor,
        superClass: ClassDescriptor
    ): Boolean {
        for (superType in subClass.getTypeConstructor().getSupertypes()) {
            if (isSameClass(superType, superClass.original)) {
                return true
            }
        }
        return false
    }

    fun getClassIdForNonLocalClass(descriptor: DeclarationDescriptor): ClassId {
        val containingDeclaration =
            descriptor.containingDeclaration
        val name: Name = descriptor.name
        if (containingDeclaration is PackageFragmentDescriptorImpl) {
            val packageFqName: FqName =
                containingDeclaration.fqName
            return ClassId(packageFqName, name)
        }
        if (containingDeclaration !is ClassDescriptor) {
            return ClassId(FqName.ROOT, name)
        }
        return getClassIdForNonLocalClass(containingDeclaration).createNestedClassId(name)
    }

    @JvmStatic

    fun isClassOrEnum(descriptor: DeclarationDescriptor?): Boolean {
        return isClass(descriptor) || isEnum(
            descriptor
        )
    }

    //    匿名对象
    @JvmStatic
    fun isAnonymousObject(descriptor: DeclarationDescriptor): Boolean {
        return isClass(descriptor) && descriptor.name == SpecialNames.NO_NAME_PROVIDED
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
        if (classKind == ClassKind.ENUM || classKind.isObject) {
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
        assert(classKind == ClassKind.CLASS || classKind == ClassKind.EXTEND || classKind == ClassKind.STRUCT || classKind == ClassKind.INTERFACE || classKind == ClassKind.ANNOTATION_CLASS) {
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
        variable: VariableDescriptor,
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
//        val descriptor: DeclarationDescriptor = descriptor
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
            if (isAnonymousObject(current) || isDescriptorWithLocalVisibility(
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
        return getParentOfType(descriptor, aClass, true)
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

    fun isTuple(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(
            descriptor,
            ClassKind.TUPLE
        )
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
        return when (descriptor) {
            is ClassDescriptor -> descriptor.kind == classKind
            is EnumClassCallableDescriptor ->  isKindOf(descriptor.type,classKind)
            else -> false
        }

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

    fun getContainingModuleOrNull(cangjieType: CangJieType): ModuleDescriptor? {
        val descriptor: ClassifierDescriptor =
            cangjieType.constructor.getDeclarationDescriptor()
                ?: return null

        return getContainingModuleOrNull(descriptor)
    }
}

object DeserializedDeclarationsFromSupertypeConflictDataKey : CallableDescriptor.UserDataKey<CallableMemberDescriptor>

fun CallableMemberDescriptor.setSingleOverridden(overridden: CallableMemberDescriptor) {
    overriddenDescriptors = listOf(overridden)
}


/**
 * 判断两个声明是否来自同一模块
 */
fun DeclarationDescriptor.isSameModule(other: DeclarationDescriptor): Boolean {
    val whatModule = getContainingModule(this)
    val fromModule = getContainingModule(other)


    //            PackageData whatModule = DescriptorUtils.getPackageDeclarationDescriptor(what);
//            PackageData fromModule = DescriptorUtils.getPackageDeclarationDescriptor(from);
    return fromModule.shouldProtectedsOf(whatModule)

}

val DeclarationDescriptor.isInsideInterface: Boolean
    get() {
        val parent = containingDeclaration as? ClassDescriptor
        return parent != null && parent.kind.isInterface
    }

@OptIn(ExperimentalContracts::class)
fun DeclarationDescriptor.isSealed(): Boolean {
    contract {
        returns(true) implies (this@isSealed is ClassDescriptor)
    }
    return DescriptorUtils.isSealedClass(this)
}

fun DeclarationDescriptor.isStatic(): Boolean {
    return when (this) {

        is FunctionDescriptor -> isStatic
        is VariableDescriptor -> isStatic

        else -> false
    }

}

fun DeclarationDescriptor.isAncestorOf(descriptor: DeclarationDescriptor, strict: Boolean): Boolean =
    DescriptorUtils.isAncestor(this, descriptor, strict)

fun compareDescriptors(
    project: Project,
    currentDescriptor: DeclarationDescriptor?,
    originalDescriptor: DeclarationDescriptor?
): Boolean {
    if (currentDescriptor == originalDescriptor) return true
    if (currentDescriptor == null || originalDescriptor == null) return false

    if (currentDescriptor.name != originalDescriptor.name) return false


    if (compareDescriptorsText(project, currentDescriptor, originalDescriptor)) return true

    if (originalDescriptor is CallableDescriptor && currentDescriptor is CallableDescriptor) {
        val overriddenOriginalDescriptor = originalDescriptor.findOriginalTopMostOverriddenDescriptors()
        val overriddenCurrentDescriptor = currentDescriptor.findOriginalTopMostOverriddenDescriptors()

        if (overriddenOriginalDescriptor.size != overriddenCurrentDescriptor.size) return false
        return overriddenCurrentDescriptor.zip(overriddenOriginalDescriptor).all {
            compareDescriptorsText(project, it.first, it.second)
        }
    }

    return false
}

private fun compareDescriptorsText(project: Project, d1: DeclarationDescriptor, d2: DeclarationDescriptor): Boolean {
    if (d1 == d2) return true
    if (d1.name != d2.name) return false

    val renderedD1 = IdeDescriptorRenderers.SOURCE_CODE.render(d1)
    val renderedD2 = IdeDescriptorRenderers.SOURCE_CODE.render(d2)
    if (renderedD1 == renderedD2) return true

    val declarations1 = DescriptorToSourceUtilsIde.getAllDeclarations(project, d1)
    val declarations2 = DescriptorToSourceUtilsIde.getAllDeclarations(project, d2)
    return declarations1 == declarations2 && declarations1.isNotEmpty()
}
