package com.huawei.cangjie.resolve.descriptorUtil

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.ClassKind.ENUM_ENTRY
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.descriptors.impl.DescriptorDerivedFromTypeAlias
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.references.mainReference
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.psi.doNotAnalyze
import com.huawei.cangjie.psi.psiUtil.getQualifiedElementSelector
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.DescriptorUtils.getContainingClass
import com.huawei.cangjie.resolve.OverridingUtil
import com.huawei.cangjie.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT
import com.huawei.cangjie.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.*
import com.huawei.cangjie.types.util.contains
import com.huawei.cangjie.types.util.equalTypesOrNulls
import com.huawei.cangjie.utils.DFS
import com.intellij.util.SmartList

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

fun ClassDescriptor.getAllSuperclassesWithoutAny() =
    generateSequence(
        getSuperClassNotAny(),
        ClassDescriptor::getSuperClassNotAny
    ).toCollection(SmartList<ClassDescriptor>())

@TypeRefinement
fun ModuleDescriptor.getCangJieTypeRefiner(): CangJieTypeRefiner =
    when (val refinerCapability = getCapability(REFINER_CAPABILITY)?.value) {
        is TypeRefinementSupport.Enabled -> refinerCapability.typeRefiner
        else -> CangJieTypeRefiner.Default
    }

val VariableDescriptor.isUnderscoreNamed
    get() = !name.isSpecial && name.identifier == "_"

val ClassifierDescriptorWithTypeParameters.denotedClassDescriptor: ClassDescriptor?
    get() = when (this) {
        is ClassDescriptor -> this
        is TypeAliasDescriptor -> classDescriptor
        else -> throw UnsupportedOperationException("Unexpected descriptor kind: $this")
    }

private fun <D : CallableDescriptor> D.containsStubTypes() =
    valueParameters.any { parameter -> parameter.type.contains { it is StubTypeForBuilderInference } }
            || returnType?.contains { it is StubTypeForBuilderInference } == true
            || dispatchReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true
            || extensionReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true

fun <D : CallableDescriptor> D.shouldBeSubstituteWithStubTypes() =
    valueParameters.none { it.type.isError }
            && returnType?.isError != true
            && dispatchReceiverParameter?.type?.isError != true
            && extensionReceiverParameter?.type?.isError != true
            && containsStubTypes()

val ClassifierDescriptorWithTypeParameters.classValueTypeDescriptor: ClassDescriptor?
    get() = denotedClassDescriptor?.let {
        when (it.kind) {
//            OBJECT -> it
            ENUM_ENTRY -> {
                // enum entry has the type of enum class
                val container = this.containingDeclaration
                assert(container is ClassDescriptor /*&& container.kind == ENUM_CLASS*/)
                container as ClassDescriptor
            }

            else -> it
//            else -> it.companionObjectDescriptor
        }
    }


/** If a literal of this class can be used as a value, returns the type of this value */
val ClassDescriptor.classValueType: CangJieType?
    get() = classValueTypeDescriptor?.defaultType


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
