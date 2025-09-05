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
package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*

/**
 * 枚举描述符实现
 *
 * 提供枚举描述符的基本实现，支持：
 * - 枚举构造函数管理
 * - 枚举成员函数管理
 * - 类型参数支持（如 Option<T>）
 * - 成员作用域管理
 * - 非穷尽性枚举支持
 * - 类型替换支持
 *
 * 示例：
 * ```kotlin
 * // 简单枚举
 * val colorEnum = EnumDescriptorImpl(
 *     name = "Color",
 *     containingDeclaration = packageDescriptor,
 *     constructors = listOf(redConstructor, greenConstructor, blueConstructor),
 *     members = listOf(getNameFunction),
 *     hasArguments = false,
 *     isNonExhaustive = false
 * )
 *
 * // 泛型枚举
 * val optionEnum = EnumDescriptorImpl(
 *     name = "Option",
 *     containingDeclaration = packageDescriptor,
 *     constructors = listOf(someConstructor, noneConstructor),
 *     members = listOf(mapFunction, flatMapFunction),
 *     hasArguments = true,
 *     isNonExhaustive = false,
 *     declaredTypeParameters = listOf(typeParameterT)
 * )
 * ```
 */
class EnumDescriptorImpl(
    override val name: Name,
    override val containingDeclaration: DeclarationDescriptor,
    private val enumConstructors: Collection<EnumConstructorDescriptor>,
    private val enumMembers: Collection<FunctionDescriptor>,
    override val hasArguments: Boolean,
    override val isNonExhaustive: Boolean,
    override val source: SourceElement,
    override val enumKind: EnumKind = EnumKind.ENUM,
    override val modality: Modality = Modality.FINAL,
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    override val declaredTypeParameters: List<TypeParameterDescriptor> = emptyList(),
    private val memberScope: MemberScope = MemberScope.Empty
) : EnumDescriptor {

    /**
     * 原始描述符（自身）
     */
    override val original: ClassifierDescriptor = this

    /**
     * 默认类型
     */
    override val defaultType: SimpleType
        get() = EnumType(this)

    /**
     * 类型构造函数
     */
    override val typeConstructor: org.cangnova.cangjie.types.TypeConstructor
        get() = EnumTypeConstructor(this)

    /**
     * 枚举构造函数列表
     */
    override val constructors: Collection<EnumConstructorDescriptor> = enumConstructors

    /**
     * 枚举成员函数列表
     */
    override val members: Collection<FunctionDescriptor> = enumMembers

    /**
     * 未替换的成员作用域
     */
    override val unsubstitutedMemberScope: MemberScope = memberScope
    
    /**
     * 是否为Option类型
     */
    override val isOptionType: Boolean
        get() = name.asString() == "Option"

    /**
     * 静态作用域
     */
    override val staticScope: MemberScope = memberScope

    /**
     * 实例作用域
     */
    override val instanceScope: MemberScope = memberScope

    /**
     * 获取成员作用域（带类型参数）
     *
     * 对于泛型枚举（如 Option<T>），需要根据类型参数创建相应的成员作用域。
     *
     * @param typeArguments 类型参数列表
     * @return 成员作用域
     */
    override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope {
        // 如果没有类型参数，返回未替换的成员作用域
        if (typeArguments.isEmpty()) {
            return unsubstitutedMemberScope
        }
        
        // 如果有类型参数，需要创建类型替换
        val typeSubstitution = TypeConstructorSubstitution.createByParametersMap(
            declaredTypeParameters.zip(typeArguments).toMap()
        )
        
        return getMemberScope(typeSubstitution)
    }

    /**
     * 获取成员作用域（带类型替换）
     *
     * 根据类型替换创建成员作用域，支持泛型枚举的类型参数替换。
     *
     * @param typeSubstitution 类型替换
     * @return 成员作用域
     */
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        // 如果没有类型替换，返回未替换的成员作用域
        if (typeSubstitution.isEmpty()) {
            return unsubstitutedMemberScope
        }
        
        // 创建替换后的成员作用域
        // 这里可以扩展为更复杂的实现，比如替换构造函数和成员函数的类型
        return memberScope
    }

    /**
     * 获取所有枚举构造函数
     *
     * @return 所有构造函数（包括继承的）
     */
    override fun getAllConstructors(): Collection<EnumConstructorDescriptor> {
        return constructors
    }

    /**
     * 获取所有成员
     *
     * @return 所有成员（包括继承的）
     */
    override fun getAllMembers(): Collection<DeclarationDescriptor> {
        return (constructors + members).toList()
    }

    /**
     * 接受访问者
     *
     * @param visitor 声明描述符访问者
     * @param data 数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitEnumDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitEnumDescriptor(this, null)
    }

    /**
     * 字符串表示
     *
     * @return 枚举描述符的字符串表示
     */
    override fun toString(): String {
        return buildString {
            append("enum ")
            append(name.asString())

            if (declaredTypeParameters.isNotEmpty()) {
                append("<")
                declaredTypeParameters.joinTo(this, separator = ", ") { it.name.asString() }
                append(">")
            }

            if (hasArguments) {
                append(" (with arguments)")
            }

            if (isNonExhaustive) {
                append(" (non-exhaustive)")
            }
        }
    }

    /**
     * 类型替换
     *
     * 对于泛型枚举，支持类型参数替换。
     * 例如：Option<T> 可以替换为 Option<Int>
     *
     * @param substitutor 类型替换器
     * @return 替换后的分类器描述符
     */
    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        // 如果没有类型参数，返回自身
        if (declaredTypeParameters.isEmpty()) {
            return this
        }
        
        // 检查是否有有效的类型替换
        val substitutedTypeParameters = declaredTypeParameters.mapNotNull { typeParam ->
            val substitutedType = substitutor.safeSubstitute(typeParam.defaultType, Variance.INVARIANT)
            if (substitutedType != typeParam.defaultType) {
                // 创建替换后的类型参数
                // 这里可以扩展为更复杂的实现
                typeParam
            } else {
                null
            }
        }
        
        // 如果有替换，创建新的枚举描述符
        if (substitutedTypeParameters.isNotEmpty()) {
            return EnumDescriptorImpl(
                name = name,
                containingDeclaration = containingDeclaration,
                enumConstructors = enumConstructors,
                enumMembers = enumMembers,
                hasArguments = hasArguments,
                isNonExhaustive = isNonExhaustive,
                source = source,
                enumKind = enumKind,
                modality = modality,
                visibility = visibility,
                declaredTypeParameters = substitutedTypeParameters,
                memberScope = memberScope
            )
        }
        
        return this
    }
}

/**
 * 枚举构造函数描述符实现
 *
 * 表示枚举中的单个case，支持：
 * - 简单构造函数（无关联值）
 * - 函数构造函数（有关联值）
 * - 参数管理
 * - 类型管理
 * - 泛型支持
 *
 * 示例：
 * ```kotlin
 * // 简单构造函数
 * val simpleConstructor = EnumConstructorDescriptorImpl(
 *     name = "Red",
 *     containingEnum = colorEnum,
 *     hasArguments = false
 * )
 *
 * // 函数构造函数（带关联值）
 * val functionConstructor = EnumConstructorDescriptorImpl(
 *     name = "Success",
 *     containingEnum = resultEnum,
 *     hasArguments = true,
 *     argumentTypes = listOf(genericTypeParameter)
 * )
 *
 * // Option 枚举的构造函数
 * val someConstructor = EnumConstructorDescriptorImpl(
 *     name = "Some",
 *     containingEnum = optionEnum,
 *     hasArguments = true,
 *     argumentTypes = listOf(typeParameterT)
 * )
 * ```
 */
class EnumConstructorDescriptorImpl(
    override val name: Name,
    override val containingEnum: EnumDescriptor,
    override val hasArguments: Boolean,
    override val returnType: CangJieType,
    override val source: SourceElement,
    override val argumentTypes: List<CangJieType> = emptyList(),
    override val valueParameters: List<ValueParameterDescriptor> = emptyList(),
    override val constructorType: CangJieType
) : EnumConstructorDescriptor {

    override val kind: CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.DECLARATION

    /**
     * 原始描述符（自身）
     */
    override val original: CallableMemberDescriptor = this
    
    override val overriddenDescriptors: Collection<CallableMemberDescriptor>
        get() = emptyList()

    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor>) {
        // 枚举构造函数不支持重写，所以这里不做任何操作
    }

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        return EnumConstructorDescriptorImpl(
            name = name,
            containingEnum = newOwner as? EnumDescriptor ?: containingEnum,
            hasArguments = hasArguments,
            returnType = returnType,
            source = source,
            argumentTypes = argumentTypes,
            valueParameters = valueParameters,
            constructorType = constructorType
        )
    }

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out CallableMemberDescriptor> {
        return object : CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
            private var newOwner: DeclarationDescriptor = containingEnum
            private var newModality: Modality = Modality.FINAL
            private var newVisibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
            private var newKind: CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.DECLARATION
            private var newTypeParameters: List<TypeParameterDescriptor> = emptyList()

            override fun setOwner(owner: DeclarationDescriptor): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                newOwner = owner
                return this
            }

            override fun setModality(modality: Modality): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                newModality = modality
                return this
            }

            override fun setVisibility(visibility: DescriptorVisibility): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                newVisibility = visibility
                return this
            }

            override fun setKind(kind: CallableMemberDescriptor.Kind): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                newKind = kind
                return this
            }

            override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                newTypeParameters = parameters
                return this
            }

            override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setSubstitution(substitution: TypeSubstitution): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setCopyOverrides(copyOverrides: Boolean): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setName(name: Name): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setOriginal(original: CallableMemberDescriptor?): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setPreserveSourceElement(): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setReturnType(type: CangJieType): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun build(): CallableMemberDescriptor {
                return EnumConstructorDescriptorImpl(
                    name = name,
                    containingEnum = newOwner as? EnumDescriptor ?: containingEnum,
                    hasArguments = hasArguments,
                    returnType = returnType,
                    source = source,
                    argumentTypes = argumentTypes,
                    valueParameters = valueParameters,
                    constructorType = constructorType
                )
            }
        }
    }

    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? {
        return null
    }

    /**
     * 包含声明
     */
    override val containingDeclaration: DeclarationDescriptor = containingEnum

    /**
     * 模态性
     */
    override val modality: Modality = Modality.FINAL

    /**
     * 可见性
     */
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    /**
     * 类型参数
     */
    override val typeParameters: List<TypeParameterDescriptor> = emptyList()
    
    override fun hasStableParameterNames(): Boolean {
        return false
    }

    /**
     * 扩展接收器参数
     */
    override val extensionReceiverParameter: ReceiverParameterDescriptor? = null

    /**
     * 上下文接收器参数
     */
    override val contextReceiverParameters: List<ReceiverParameterDescriptor> = emptyList()

    /**
     * 调度接收器参数
     */
    override val dispatchReceiverParameter: ReceiverParameterDescriptor? = null
    
    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }

    /**
     * 接受访问者
     *
     * @param visitor 声明描述符访问者
     * @param data 数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitEnumConstructorDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitEnumConstructorDescriptor(this, null)
    }

    /**
     * 字符串表示
     *
     * @return 枚举构造函数描述符的字符串表示
     */
    override fun toString(): String {
        return buildString {
            append(name.asString())

            if (hasArguments) {
                append("(")
                argumentTypes.joinTo(this, separator = ", ") { it.toString() }
                append(")")
            }
        }
    }

    /**
     * 类型替换
     *
     * 支持枚举构造函数的类型参数替换。
     *
     * @param substitutor 类型替换器
     * @return 替换后的可调用描述符
     */
    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor? {
        // 如果没有类型参数，返回自身
        if (typeParameters.isEmpty() && argumentTypes.isEmpty()) {
            return this
        }
        
        // 替换参数类型
        val substitutedArgumentTypes = argumentTypes.map { argType ->
            substitutor.safeSubstitute(argType, Variance.INVARIANT)
        }
        
        // 替换返回类型
        val substitutedReturnType = substitutor.safeSubstitute(returnType, Variance.INVARIANT)
        
        // 替换构造函数类型
        val substitutedConstructorType = substitutor.safeSubstitute(constructorType, Variance.INVARIANT)
        
        // 如果有替换，创建新的构造函数描述符
        if (substitutedArgumentTypes != argumentTypes || 
            substitutedReturnType != returnType || 
            substitutedConstructorType != constructorType) {
            return EnumConstructorDescriptorImpl(
                name = name,
                containingEnum = containingEnum,
                hasArguments = hasArguments,
                returnType = substitutedReturnType,
                source = source,
                argumentTypes = substitutedArgumentTypes,
                valueParameters = valueParameters,
                constructorType = substitutedConstructorType
            )
        }
        
        return this
    }
} 