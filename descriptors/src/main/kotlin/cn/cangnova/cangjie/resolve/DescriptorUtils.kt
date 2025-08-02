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

package cn.cangnova.cangjie.resolve


import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.builtins.StandardNames.FqNames.fromByName
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotated
import cn.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import cn.cangnova.cangjie.descriptors.enumd.EnumEntryDescriptor
import cn.cangnova.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import cn.cangnova.cangjie.descriptors.impl.PackageFragmentDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import cn.cangnova.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import cn.cangnova.cangjie.lexer.CjModifierKeywordToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.*
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.CjNamedFunctionForExtend
import cn.cangnova.cangjie.resolve.DescriptorUtils.getContainingModule
import cn.cangnova.cangjie.resolve.scopes.*
import cn.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils.isError
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.TypeRefinement
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.checker.REFINER_CAPABILITY
import cn.cangnova.cangjie.types.checker.TypeRefinementSupport
import cn.cangnova.cangjie.types.isError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


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
        return !descriptor.overriddenDescriptors.isEmpty()
    }

    fun <D : CallableMemberDescriptor?> getAllOverriddenDeclarations(memberDescriptor: D): Set<D> {
        val result: MutableSet<D> = HashSet()
        for (overriddenDeclaration in memberDescriptor?.overriddenDescriptors ?: emptyList()) {
            val kind: CallableMemberDescriptor.Kind = overriddenDeclaration.kind
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
                (container is ClassDescriptor && descriptor.dispatchReceiverParameter == null)
    }

    @JvmStatic
    fun isDirectSubclass(
        subClass: ClassDescriptor,
        superClass: ClassDescriptor
    ): Boolean {
        for (superType in subClass.typeConstructor.supertypes) {
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
        val classKind: ClassKind = classDescriptor.kind
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
        while (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            val overridden: Collection<CallableMemberDescriptor?> =
                descriptor.overriddenDescriptors
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
        )) && (descriptor as ClassDescriptor).modality == Modality.SEALED
    }

    fun classCanHaveAbstractDeclaration(classDescriptor: ClassDescriptor): Boolean {
        return classDescriptor.modality== Modality.ABSTRACT || isSealedClass(
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
            return descriptor.source
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
        is EnumClassCallableDescriptor -> isStatic
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
fun CallableDescriptor.hasDynamicExtensionAnnotation(): Boolean = false
val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)
val DeclarationDescriptor.parents: Sequence<DeclarationDescriptor>
    get() = parentsWithSelf.drop(1)

val DeclarationDescriptor.parentsWithSelf: Sequence<DeclarationDescriptor>
    get() = generateSequence(this, { it.containingDeclaration })
val AnnotationDescriptor.annotationClass: ClassDescriptor?
    get() = type.constructor.declarationDescriptor as? ClassDescriptor
@TypeRefinement
fun ModuleDescriptor.getCangJieTypeRefiner(): CangJieTypeRefiner =
    when (val refinerCapability = getCapability(REFINER_CAPABILITY)?.value) {
        is TypeRefinementSupport.Enabled -> refinerCapability.typeRefiner
        else -> CangJieTypeRefiner.Default
    }