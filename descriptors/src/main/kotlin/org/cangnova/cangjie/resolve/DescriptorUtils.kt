/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames.FqNames.fromByName
import org.cangnova.cangjie.builtins.UnsignedTypes
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.psi.CjDeclarationStub
import org.cangnova.cangjie.psi.CjImportDirectiveItem
import org.cangnova.cangjie.resolve.DescriptorUtils.getContainingModule
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils.isError
import org.cangnova.cangjie.types.StubTypeForBuilderInference
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.REFINER_CAPABILITY
import org.cangnova.cangjie.types.checker.TypeRefinementSupport
import org.cangnova.cangjie.types.contains
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.DFS
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 描述符工具类
 *
 * 提供用于处理和操作仓颉语言描述符的实用工具方法集合。
 * 包含类型检查、层级关系判断、可见性计算、模块关系等常用功能。
 */
object DescriptorUtils {

    /**
     * 从顶层类获取完全限定名
     *
     * @param descriptor 声明描述符
     * @return 完全限定名
     */
    fun getFqNameFromTopLevelClass(descriptor: DeclarationDescriptor): FqName {
        val containingDeclaration =
            descriptor.containingDeclaration
        val name: Name = descriptor.name
        if (containingDeclaration !is ClassDescriptor) {
            return FqName.topLevel(name)
        }
        return getFqNameFromTopLevelClass(containingDeclaration).child(name)
    }

    /**
     * 判断可调用成员是否是覆盖成员
     *
     * @param descriptor 可调用成员描述符
     * @return 如果该成员覆盖了其他成员则返回 true
     */
    fun isOverride(descriptor: CallableMemberDescriptor): Boolean {
        return !descriptor.overriddenDescriptors.isEmpty()
    }

    /**
     * 获取所有被覆盖的声明
     * 递归收集所有被该成员直接或间接覆盖的声明
     *
     * @param memberDescriptor 成员描述符
     * @return 所有被覆盖的声明集合
     */
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

    /**
     * 判断一个描述符是否是另一个描述符的祖先
     *
     * @param ancestor 可能的祖先描述符
     * @param declarationDescriptor 要检查的声明描述符
     * @param strict 是否严格模式(true 时从父级开始检查,false 时包含自身)
     * @return 如果 ancestor 是 declarationDescriptor 的祖先则返回 true
     */
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

    /**
     * 获取直接成员
     * 如果是属性访问器,返回对应的属性;否则返回自身
     *
     * @param descriptor 可调用成员描述符
     * @return 直接成员描述符
     */
    fun getDirectMember(descriptor: CallableMemberDescriptor): CallableMemberDescriptor {
        return if (descriptor is PropertyAccessorDescriptor)
            descriptor.correspondingProperty
        else
            descriptor
    }

    /**
     * 判断是否是静态声明
     * 顶层声明或没有 dispatch receiver 的类成员被认为是静态的
     *
     * @return true 当且仅当这是一个顶层声明或没有预期 "this" 对象的类成员
     */
    fun isStaticDeclaration(descriptor: CallableDescriptor): Boolean {
        if (descriptor is ConstructorDescriptor) return false

        val container: DeclarationDescriptor = descriptor.containingDeclaration
        return container is PackageFragmentDescriptor ||
                (container is ClassDescriptor && descriptor.dispatchReceiverParameter == null)
    }

    /**
     * 判断是否是直接子类
     *
     * @param subClass 子类描述符
     * @param superClass 超类描述符
     * @return 如果 subClass 是 superClass 的直接子类则返回 true
     */
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

    /**
     * 获取非本地类的 ClassId
     *
     * @param descriptor 声明描述符
     * @return 类 ID
     */
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

    /**
     * 判断描述符是否表示类或枚举
     *
     * @param descriptor 声明描述符
     * @return 如果是类或枚举则返回 true
     */
    @JvmStatic
    fun isClassOrEnum(descriptor: DeclarationDescriptor?): Boolean {
        return isClass(descriptor) || isEnum(
            descriptor
        )
    }

    /**
     * 判断是否是匿名对象
     *
     * @param descriptor 声明描述符
     * @return 如果是匿名对象则返回 true
     */
    @JvmStatic
    fun isAnonymousObject(descriptor: DeclarationDescriptor): Boolean {
        return isClass(descriptor) && descriptor.name == SpecialNames.NO_NAME_PROVIDED
    }

    /**
     * 判断类是否可以有声明的构造函数
     * 接口不能有构造函数
     *
     * @param classDescriptor 类描述符
     * @return 如果可以有构造函数则返回 true
     */
    fun canHaveDeclaredConstructors(classDescriptor: ClassDescriptor): Boolean {
        return !isInterface(
            classDescriptor
        )
    }

    /**
     * 获取默认构造函数的可见性
     * 不同类型的类有不同的默认构造函数可见性:
     * - 枚举和对象: PRIVATE
     * - 密封类: 根据是否支持密封接口自由度返回 PROTECTED 或 PRIVATE
     * - 其他(普通类、扩展类、结构体、接口): PUBLIC
     *
     * @param classDescriptor 类描述符
     * @param freedomForSealedInterfacesSupported 是否支持密封接口的自由度
     * @return 默认构造函数的可见性
     */
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
        assert(classKind == ClassKind.CLASS || classKind == ClassKind.EXTEND || classKind == ClassKind.STRUCT || classKind == ClassKind.INTERFACE) {
            "Unexpected class kind: $classKind"
        }
        return DescriptorVisibilities.PUBLIC
    }

    /**
     * 给定一个虚假覆盖（fake override），在其被覆盖的描述符中查找任意一个声明。
     * 请注意在超类型中可能存在多个该虚假覆盖的声明，此方法仅返回其中的一个。
     * TODO: 可能此方法的某些调用站点存在问题，应处理所有超声明。
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

    /**
     * 警告:不要在 JVM 后端使用此方法,应该使用 JvmCodegenUtil.isCallInsideSameModuleAsDeclared()
     * 后者可以正确处理针对已编译部分模块的编译情况
     *
     * 判断两个描述符是否在同一模块中
     *
     * @param first 第一个声明描述符
     * @param second 第二个声明描述符
     * @return 如果在同一模块中则返回 true
     */
    @JvmStatic
    fun areInSameModule(
        first: DeclarationDescriptor,
        second: DeclarationDescriptor
    ): Boolean {
        return getContainingModule(first) == getContainingModule(
            second
        )
    }

    /**
     * 获取包含此描述符的类
     * 向上遍历声明链,找到第一个类描述符
     *
     * @param descriptor 声明描述符
     * @return 包含的类描述符,如果不在类中则返回 null
     */
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

    /**
     * 判断类是否可以有抽象的虚假覆盖
     *
     * @param classDescriptor 类描述符
     * @return 如果可以有抽象虚假覆盖则返回 true
     */
    fun classCanHaveAbstractFakeOverride(classDescriptor: ClassDescriptor): Boolean {
        return classCanHaveAbstractDeclaration(classDescriptor)
    }

    /**
     * 判断是否是密封类
     *
     * @param descriptor 声明描述符
     * @return 如果是密封类或密封接口则返回 true
     */
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

    /**
     * 判断类是否可以有抽象声明
     * 抽象类、密封类可以有抽象声明
     *
     * @param classDescriptor 类描述符
     * @return 如果可以有抽象声明则返回 true
     */
    fun classCanHaveAbstractDeclaration(classDescriptor: ClassDescriptor): Boolean {
        return classDescriptor.modality == Modality.ABSTRACT || isSealedClass(classDescriptor)
    }

    /**
     * 判断是否应该记录属性的初始化器
     * 对于某些类型(如原始类型、Any、无符号类型等),需要记录其初始化值
     *
     * @param variable 变量描述符
     * @param type 变量类型
     * @return 如果应该记录初始化器则返回 true
     */
    fun shouldRecordInitializerForProperty(
        variable: VariableDescriptor,
        type: CangJieType
    ): Boolean {
        if (variable.isVar || type.isError) return false

        if (TypeUtils.acceptsOption(type)) return true

        val builtIns: CangJieBuiltIns = variable.builtIns
        return CangJieBuiltIns.isPrimitiveType(type) ||
                CangJieTypeChecker.DEFAULT.equalTypes(builtIns.stdlibTypes.anyType, type) ||
                UnsignedTypes.isUnsignedType(type)
    }

    /**
     * 获取包含声明的源文件
     *
     * @param descriptor 声明描述符
     * @return 包含该声明的源文件,如果无法确定则返回 NO_SOURCE_FILE
     */
    @JvmStatic
    fun getContainingSourceFile(descriptor: DeclarationDescriptor): SourceFile {
        if (descriptor is DeclarationDescriptorWithSource) {
            return descriptor.source.containingFile
        }
        return SourceFile.NO_SOURCE_FILE
    }

    private fun getFqNameUnsafe(descriptor: DeclarationDescriptor): FqNameUnsafe {
        val containingDeclaration =
            checkNotNull(descriptor.containingDeclaration) { "Not package/module descriptor doesn't have containing declaration: $descriptor" }
        return getFqName(containingDeclaration).child(descriptor.name)
    }

    /**
     * 安全地获取完全限定名
     *
     * @param descriptor 声明描述符
     * @return 安全的完全限定名
     */
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
        for (callableDescriptor in current.original.overriddenDescriptors) {
            val descriptor = callableDescriptor.original as D
            collectAllOverriddenDescriptors(descriptor, result)
            result.add(descriptor)
        }
    }

    /**
     * 获取所有被覆盖的描述符
     *
     * @return 原始的(未替换的)描述符集合,不包含重复项
     */
    @JvmStatic
    fun <D : CallableDescriptor> getAllOverriddenDescriptors(f: D): MutableSet<D> {
        val result: MutableSet<D> = LinkedHashSet()
        collectAllOverriddenDescriptors<D>(f.original as D, result)
        return result
    }

    /**
     * 判断描述符是否是本地的
     * 描述符本身可能是本地的,或者有一个本地的祖先
     *
     * @param descriptor 声明描述符
     * @return 如果是本地的或有本地祖先则返回 true
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

    /**
     * 判断是否是顶层声明
     * 顶层声明的包含声明是 PackageFragmentDescriptor
     *
     * @param descriptor 声明描述符
     * @return 如果是顶层声明则返回 true
     */
    @JvmStatic
    fun isTopLevelDeclaration(descriptor: DeclarationDescriptor?): Boolean {
        return descriptor != null && descriptor.containingDeclaration is PackageFragmentDescriptor
    }

    /**
     * 获取指定类型的父描述符
     * 向上遍历层次结构,查找指定类型的父级
     *
     * @param descriptor 起始描述符
     * @param aClass 要查找的父类型
     * @return 指定类型的父描述符,如果未找到则返回 null
     */
    @JvmStatic
    fun <D : DeclarationDescriptor?> getParentOfType(
        descriptor: DeclarationDescriptor?,
        aClass: Class<D>
    ): D? {
        return getParentOfType(descriptor, aClass, true)
    }

    /**
     * 获取指定类型的父描述符
     *
     * @param descriptor 起始描述符
     * @param aClass 要查找的父类型
     * @param strict 是否严格模式(true 时从父级开始,false 时包含自身)
     * @return 指定类型的父描述符,如果未找到则返回 null
     */
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

    /**
     * 判断类型是否是指定类的子类型
     * 递归检查类型及其所有超类型
     *
     * @param type 要检查的类型
     * @param superClass 超类声明描述符
     * @return 如果type是superClass的子类型则返回true
     */
    fun isSubtypeOfClass(
        type: CangJieType,
        superClass: DeclarationDescriptor
    ): Boolean {
        if (isSameClass(type, superClass)) return true
        for (superType in type.constructor.supertypes) {
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
            type.constructor.declarationDescriptor
        if (descriptor != null) {
            val originalDescriptor: DeclarationDescriptor = descriptor.original
            if ((originalDescriptor is ClassifierDescriptor
                        && other is ClassifierDescriptor) && other.typeConstructor == originalDescriptor.typeConstructor
            ) {
                return true
            }
        }
        return false
    }

    /**
     * 判断一个类是否是另一个类的子类
     *
     * @param subClass 子类描述符
     * @param superClass 超类描述符
     * @return 如果subClass是superClass的子类则返回true
     */
    @JvmStatic
    fun isSubclass(
        subClass: ClassDescriptor,
        superClass: ClassDescriptor
    ): Boolean {
        return isSubtypeOfClass(
            subClass.defaultType,
            superClass.original
        )
    }


    /**
     * 判断描述符是否表示一个类
     *
     * @param descriptor 声明描述符
     * @return 如果是类则返回true
     */
    @JvmStatic
    fun isClass(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(
            descriptor,
            ClassKind.CLASS
        )
    }

    /**
     * 从类型构造器获取类描述符
     *
     * @param typeConstructor 类型构造器
     * @return 类描述符
     * @throws AssertionError 如果类型构造器的声明描述符不是ClassDescriptor
     */
    fun getClassDescriptorForTypeConstructor(typeConstructor: TypeConstructor): ClassDescriptor {
        val descriptor =
            typeConstructor.declarationDescriptor
        assert(
            descriptor is ClassDescriptor
        ) { "Classifier descriptor of a type should be of type ClassDescriptor: $typeConstructor" }
        return descriptor as ClassDescriptor
    }

    /**
     * 从类型获取类描述符
     *
     * @param type 仓颉类型
     * @return 类描述符
     */
    fun getClassDescriptorForType(type: CangJieType): ClassDescriptor {
        return getClassDescriptorForTypeConstructor(type.constructor)
    }

    /**
     * 获取超类类型
     * 如果没有非接口的超类型,返回Any类型
     *
     * @param classDescriptor 类描述符
     * @return 超类类型
     */
    @JvmStatic
    fun getSuperClassType(classDescriptor: ClassDescriptor): CangJieType {
        val superclassTypes: Collection<CangJieType> =
            classDescriptor.typeConstructor.supertypes
        for (type in superclassTypes) {
            val superClassDescriptor: ClassDescriptor =
                getClassDescriptorForType(type)
            if (superClassDescriptor.kind != ClassKind.INTERFACE) {
                return type
            }
        }
        return classDescriptor.builtIns.stdlibTypes.anyType
    }

    /**
     * 判断描述符是否表示一个元组
     *
     * @param descriptor 声明描述符
     * @return 如果是元组则返回true
     */
    @JvmStatic
    fun isTuple(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(
            descriptor,
            ClassKind.TUPLE
        )
    }

    /**
     * 判断描述符是否表示一个枚举
     *
     * @param descriptor 声明描述符
     * @return 如果是枚举则返回true
     */
    @JvmStatic
    fun isEnum(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(
            descriptor,
            ClassKind.ENUM
        )
    }

    /**
     * 获取调度接收者参数(如果需要)
     * 对于类成员,返回类的this接收者参数
     *
     * @param containingDeclaration 包含声明
     * @return 接收者参数描述符,如果不需要则返回null
     */
    @JvmStatic
    fun getDispatchReceiverParameterIfNeeded(containingDeclaration: DeclarationDescriptor): ReceiverParameterDescriptor? {
        if (containingDeclaration is ClassDescriptor) {
            return containingDeclaration.thisAsReceiverParameter
        }
        return null
    }

    /**
     * 判断描述符是否表示一个枚举项
     *
     * @param descriptor 声明描述符
     * @return 如果是枚举项则返回true
     */
    @JvmStatic
    fun isEnumEntry(descriptor: DeclarationDescriptor): Boolean {
        return isKindOf(descriptor, ClassKind.ENUM_ENTRY)
    }

    @JvmStatic
    private fun isKindOf(descriptor: DeclarationDescriptor?, classKind: ClassKind): Boolean {
        return when (descriptor) {
            is ClassDescriptor -> descriptor.kind == classKind
            else -> false
        }
    }

    /**
     * 获取作用域中的所有描述符
     *
     * @param scope 成员作用域
     * @return 所有贡献的声明描述符集合
     */
    @JvmStatic
    fun getAllDescriptors(scope: MemberScope): Collection<DeclarationDescriptor> {
        return scope.getContributedDescriptors(DescriptorKindFilter.ALL, ALL_NAME_FILTER)
    }

    /**
     * 判断描述符是否表示一个接口
     *
     * @param descriptor 声明描述符
     * @return 如果是接口则返回true
     */
    @JvmStatic
    fun isInterface(descriptor: DeclarationDescriptor?): Boolean {
        return isKindOf(descriptor, ClassKind.INTERFACE)
    }

    @JvmStatic
    private fun getFqNameSafeIfPossible(descriptor: DeclarationDescriptor): FqName? {
        if (descriptor is ModuleDescriptor || isError(descriptor)) {
            return FqName.ROOT
        }
        if (descriptor is PrimitiveClassDescriptor) {

            return fromByName(descriptor.name)
        }


        if (descriptor is PackageViewDescriptor) {
            return descriptor.fqName
        } else if (descriptor is PackageFragmentDescriptor) {
            return descriptor.fqName
        }

        return null
    }

    /**
     * 获取完全限定名(可能不安全)
     *
     * @param descriptor 声明描述符
     * @return 不安全的完全限定名
     */
    @JvmStatic
    fun getFqName(descriptor: DeclarationDescriptor): FqNameUnsafe {
        val safe = getFqNameSafeIfPossible(descriptor)
        return safe?.toUnsafe() ?: getFqNameUnsafe(descriptor)
    }

    /**
     * 获取包声明描述符
     * 递归向上查找包级别的描述符
     *
     * @param descriptor 声明描述符
     * @return 包数据描述符
     */
    @JvmStatic
    fun getPackageDeclarationDescriptor(descriptor: DeclarationDescriptor): PackageData {
        return when (descriptor) {
            is PackageFragmentDescriptor -> descriptor
            is PackageViewDescriptor -> descriptor
            is ClassDescriptor -> getPackageDeclarationDescriptor(descriptor.containingDeclaration)
            else -> getPackageDeclarationDescriptor(descriptor.containingDeclaration!!)
        }
    }

    /**
     * 获取包含此描述符的模块
     *
     * @param descriptor 声明描述符
     * @return 模块描述符
     * @throws IllegalStateException 如果描述符没有包含模块
     */
    @JvmStatic
    fun getContainingModule(descriptor: DeclarationDescriptor): ModuleDescriptor {
        val module =
            getContainingModuleOrNull(descriptor)
                ?: error("Descriptor without a containing module: $descriptor")
        return module
    }

    /**
     * 获取包含此描述符的模块(可能为null)
     * 向上遍历声明链查找模块描述符
     *
     * @param descriptor 声明描述符
     * @return 模块描述符,如果未找到则返回null
     */
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

    /**
     * 从类型获取包含的模块(可能为null)
     *
     * @param cangjieType 仓颉类型
     * @return 模块描述符,如果未找到则返回null
     */
    fun getContainingModuleOrNull(cangjieType: CangJieType): ModuleDescriptor? {
        val descriptor: ClassifierDescriptor =
            cangjieType.constructor.declarationDescriptor
                ?: return null

        return getContainingModuleOrNull(descriptor)
    }
}

/**
 * 反序列化声明的超类型冲突数据键
 * 用于存储从超类型冲突中反序列化的声明信息
 */
object DeserializedDeclarationsFromSupertypeConflictDataKey : CallableDescriptor.UserDataKey<CallableMemberDescriptor>

/**
 * 设置单个覆盖描述符
 * 为可调用成员设置唯一的被覆盖描述符
 *
 * @param overridden 被覆盖的可调用成员描述符
 */
fun CallableMemberDescriptor.setSingleOverridden(overridden: CallableMemberDescriptor) {
    setOverriddenDescriptors(listOf(overridden))
}


/**
 * 判断两个声明是否来自同一模块
 */
fun DeclarationDescriptor.isSameModule(other: DeclarationDescriptor): Boolean {
    val whatModule = getContainingModule(this)
    val fromModule = getContainingModule(other)
    return fromModule.shouldProtectedsOf(whatModule)
}

/**
 * 判断声明是否在接口内部
 * 检查父声明是否是接口类型
 */
val DeclarationDescriptor.isInsideInterface: Boolean
    get() {
        val parent = containingDeclaration as? ClassDescriptor
        return parent != null && parent.kind.isInterface
    }

/**
 * 判断声明是否是密封类或密封接口
 * 使用Kotlin契约确保返回true时描述符一定是ClassDescriptor
 */
@OptIn(ExperimentalContracts::class)
fun DeclarationDescriptor.isSealed(): Boolean {
    contract {
        returns(true) implies (this@isSealed is ClassDescriptor)
    }
    return DescriptorUtils.isSealedClass(this)
}

/**
 * 判断声明是否是静态的
 * 根据描述符类型判断是否为静态成员
 */
fun DeclarationDescriptor.isStatic(): Boolean {
    return when (this) {
        is FunctionDescriptor -> isStatic
        is VariableDescriptor -> isStatic
        else -> false
    }
}

/**
 * 判断当前描述符是否是指定描述符的祖先
 *
 * @param descriptor 要检查的描述符
 * @param strict 是否严格模式
 * @return 如果是祖先则返回true
 */
fun DeclarationDescriptor.isAncestorOf(descriptor: DeclarationDescriptor, strict: Boolean): Boolean =
    DescriptorUtils.isAncestor(this, descriptor, strict)

/**
 * 可调用描述符是否有动态扩展注解
 * 当前实现总是返回false
 */
fun CallableDescriptor.hasDynamicExtensionAnnotation(): Boolean = false

/**
 * 获取描述符所在的模块
 */
val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

/**
 * 获取描述符的所有父级(不包含自身)
 */
val DeclarationDescriptor.parents: Sequence<DeclarationDescriptor>
    get() = parentsWithSelf.drop(1)

/**
 * 获取描述符的所有父级(包含自身)
 * 生成从当前描述符到根的序列
 */
val DeclarationDescriptor.parentsWithSelf: Sequence<DeclarationDescriptor>
    get() = generateSequence(this, { it.containingDeclaration })

/**
 * 获取注解的注解类
 */
val AnnotationDescriptor.annotationClass: ClassDescriptor?
    get() = type.constructor.declarationDescriptor as? ClassDescriptor


/**
 * 获取模块的仓颉类型精化器
 * 根据模块的类型精化支持能力返回相应的精化器
 */
fun ModuleDescriptor.getCangJieTypeRefiner(): CangJieTypeRefiner =
    when (val refinerCapability = getCapability(REFINER_CAPABILITY)?.value) {
        is TypeRefinementSupport.Enabled -> refinerCapability.typeRefiner
        else -> CangJieTypeRefiner.Default
    }

/**
 * 通过完全限定名解析类
 * 在模块的包和类层次结构中查找类描述符
 *
 * @param fqName 完全限定名
 * @param lookupLocation 查找位置
 * @return 类描述符,如果未找到则返回null
 */
fun ModuleDescriptor.resolveClassByFqName(fqName: FqName, lookupLocation: LookupLocation): ClassDescriptor? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent())
        .memberScope.getContributedClassifier(
            fqName.shortName(),
            lookupLocation
        ) as? ClassDescriptor)?.let { return it }

    return resolveClassByFqName(fqName.parent(), lookupLocation)
        ?.unsubstitutedMemberScope
        ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor
}

/**
 * 获取描述符的内置类型集合
 * 通过描述符所在模块获取仓颉内置类型
 */
val DeclarationDescriptor.builtIns: CangJieBuiltIns
    get() = module.builtIns

/**
 * 获取分类器描述符的类ID
 * 根据不同的描述符类型生成相应的ClassId
 * - 原始类型: 根命名空间下的类ID
 * - 函数类型: 根命名空间下的类ID
 * - 元组类型: 根命名空间下的类ID
 * - 其他: 根据包含声明生成嵌套的类ID
 */
val ClassifierDescriptor?.classId: ClassId?
    get() {
        if (this is PrimitiveClassDescriptor) {
            return ClassId(FqName.ROOT, name)
        } else if (this is FunctionClassDescriptor) {
            return ClassId(FqName.ROOT, name)
        } else if (this is TupleClassDescriptor) {
            return ClassId(FqName.ROOT, name)
        }
        return this?.containingDeclaration?.let { owner ->
            when (owner) {
                is PackageFragmentDescriptor -> ClassId(owner.fqName, name)
                is ClassifierDescriptorWithTypeParameters -> owner.classId?.createNestedClassId(name)
                else -> null
            }
        }
    }

/**
 * 判断值参数是否声明或继承了默认值
 * 使用深度优先搜索检查参数及其所有被覆盖的参数
 *
 * @return 如果参数或其任何被覆盖的参数声明了默认值则返回true
 */
fun ValueParameterDescriptor.declaresOrInheritsDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::original) },
        ValueParameterDescriptor::declaresDefaultValue
    )
}

fun DeclarationDescriptor.isAnnotatinoDescriptor(): Boolean {
    TODO("实现是否是注解声明")
}

/**
 * 获取类的辅助构造函数列表
 * 过滤掉主构造函数,只返回辅助构造函数
 */
val ClassDescriptor.secondaryConstructors: List<ClassConstructorDescriptor>
    get() = constructors.filterNot { it.isPrimary }

/**
 * 展开类型别名
 * 如果是类型别名,递归展开到实际的类描述符;否则返回自身
 *
 * @return 展开后的描述符,如果无法展开则返回null
 */
fun DeclarationDescriptor.unwrapIfTypeAlias(): DeclarationDescriptor? =
    when (this) {
        is TypeAliasDescriptor -> this.classDescriptor?.unwrapIfTypeAlias()
        else -> this
    }

/**
 * 获取超类或Any
 * 如果类有非Any的超类则返回该超类,否则返回Any
 */
fun ClassDescriptor.getSuperClassOrAny(): ClassDescriptor = getSuperClassNotAny() ?: builtIns.stdlibTypes.any

/**
 * 获取非Any的超类
 * 在超类型中查找第一个非Any且非接口的类
 *
 * @return 超类描述符,如果没有则返回null
 */
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

/**
 * 安全地获取完全限定名
 * 确保返回的完全限定名不会抛出异常
 */
val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

/**
 * 从声明stub获取修饰符可见性
 * 根据修饰符关键字确定可见性级别
 * - private: PRIVATE
 * - internal: INTERNAL
 * - protected: PROTECTED
 * - public或无修饰符: PUBLIC(默认)
 */
val <T : StubElement<*>>CjDeclarationStub<T>.modifierVisibility: DescriptorVisibility
    get() {

        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE
        if (hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL
        if (hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED
        if (hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC
        return DescriptorVisibilities.PUBLIC
    }

/**
 * 获取导入语句的可见性
 *
 * 仓颉语言导入语句可见性规则：
 * - private（默认）: 仅当前文件内可访问，不重导出
 * - internal: 当前包及子包可访问
 * - protected: 当前模块内可访问
 * - public: 外部可访问（重导出）
 *
 * 注意：与普通声明不同，导入语句默认为 PRIVATE
 */
val CjImportDirectiveItem.importVisibility: DescriptorVisibility
    get() {
        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE
        if (hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL
        if (hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED
        if (hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC
        // 导入语句默认为 PRIVATE（不重导出）
        return DescriptorVisibilities.PRIVATE
    }

/**
 * 判断导入语句是否为重导出
 *
 * 如果导入语句带有 internal/protected/public 修饰符，则为重导出。
 * private 导入（包括默认无修饰符）不是重导出。
 */
val CjImportDirectiveItem.isReexport: Boolean
    get() = importVisibility != DescriptorVisibilities.PRIVATE

/**
 * 获取表示的类描述符
 * 如果是类描述符直接返回,如果是类型别名则返回其指向的类
 *
 * @return 类描述符,如果不支持的描述符类型则抛出异常
 * @throws UnsupportedOperationException 对于不支持的描述符类型
 */
val ClassifierDescriptorWithTypeParameters.denotedClassDescriptor: ClassDescriptor?
    get() = when (this) {
        is ClassDescriptor -> this
        is TypeAliasDescriptor -> classDescriptor
        else -> throw UnsupportedOperationException("Unexpected descriptor kind: $this")
    }

val ClassifierDescriptorWithTypeParameters.classValueTypeDescriptor: ClassDescriptor?
    get() = denotedClassDescriptor


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

fun ModuleDescriptor.isTypeRefinementEnabled(): Boolean =
    getCapability(REFINER_CAPABILITY)?.value?.isEnabled == true

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