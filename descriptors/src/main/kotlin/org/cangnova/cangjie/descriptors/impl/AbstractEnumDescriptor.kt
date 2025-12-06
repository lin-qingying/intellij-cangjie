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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.DeclarationDescriptorVisitor
import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.getCangJieTypeRefiner
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieTypeFactory.computeExpandedType
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeConstructorSubstitution
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeRefinement
import org.cangnova.cangjie.types.TypeSubstitution
import org.cangnova.cangjie.types.TypeSubstitutor
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.makeUnsubstitutedType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 抽象枚举描述符基类
 *
 * 提供枚举描述符的通用实现，子类可以根据具体需求进行定制。
 * 支持：
 * - 枚举构造函数管理
 * - 枚举成员函数管理
 * - 类型参数支持
 * - 成员作用域管理
 * - 非穷尽性枚举支持
 * - 类型替换支持
 *
 * 该抽象类定义了枚举的核心行为和状态，子类可以：
 * - 自定义成员作用域创建策略
 * - 定制类型替换逻辑
 * - 扩展构造函数和成员管理
 * - 实现特殊的枚举类型（如Option）
 */
abstract class AbstractEnumDescriptor(
    protected val storageManager: StorageManager,
    override val name: Name,


    ) : ModuleAwareEnumDescriptor(), EnumDescriptor {

    /**
     * 原始描述符（自身）
     */
    override val original: EnumDescriptor = this

    /**
     * 默认类型（懒加载）
     */
    override val defaultType: SimpleType by lazy {
        _defaultType()
    }

    
    protected val _defaultType: NotNullLazyValue<SimpleType> = storageManager.createLazyValue {
        makeUnsubstitutedType(
            this, unsubstitutedMemberScope,

            object : (CangJieTypeRefiner) -> SimpleType? {
                override fun invoke(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType? {
                    val descriptor = cangjieTypeRefiner.refineDescriptor(this@AbstractEnumDescriptor)
                    // If we've refined descriptor
                    if (descriptor == null) return _defaultType.invoke()

                    if (descriptor is TypeAliasDescriptor) {
                        return descriptor.computeExpandedType(
                            TypeUtils.getDefaultTypeProjections(descriptor.typeConstructor.parameters)
                        )
                    }


                    return descriptor.defaultType
                }

            }


        )
    }

    
    override val unsubstitutedMemberScope: MemberScope
        get() = getUnsubstitutedMemberScope(DescriptorUtils.getContainingModule(this).getCangJieTypeRefiner())

    abstract override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope


    /**
     * 是否为Option类型
     */
    override val isOptionType: Boolean
        get() = name.asString() == "Option"

    /**
     * 静态作用域（默认为空，子类可重写）
     */
    override val staticScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 实例作用域（默认为空，子类可重写）
     */
    override val instanceScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 抽象属性 - 枚举构造函数列表
     * 子类必须实现此属性
     */
    abstract override val constructors: Collection<EnumConstructorDescriptor>


    /**
     * 获取成员作用域（带类型参数）
     *
     * 对于泛型枚举（如 Option<T>），需要根据类型参数创建相应的成员作用域。
     * 子类可重写以提供自定义的成员作用域创建逻辑。
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
     * 子类可重写以提供自定义的类型替换逻辑。
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
        return createSubstitutedMemberScope(typeSubstitution)
    }

    /**
     * 创建替换后的成员作用域
     * 子类可重写以提供自定义的类型替换成员作用域创建逻辑
     *
     * @param typeSubstitution 类型替换
     * @return 替换后的成员作用域
     */
    protected open fun createSubstitutedMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        // 默认实现：返回未替换的成员作用域
        // 子类可以扩展为更复杂的实现，比如替换构造函数和成员函数的类型
        return unsubstitutedMemberScope
    }

    /**
     * 获取所有枚举构造函数
     *
     * 默认实现返回所有构造函数，子类可重写以包含继承的构造函数
     *
     * @return 所有构造函数（包括继承的）
     */
    override fun getAllConstructors(): Collection<EnumConstructorDescriptor> {
        return constructors
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
     * 类型替换
     *
     * 对于泛型枚举，支持类型参数替换。
     * 子类可重写以提供自定义的类型替换逻辑。
     *
     * @param substitutor 类型替换器
     * @return 替换后的分类器描述符
     */
    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        if (substitutor.isEmpty) {
            return this
        }
        return LazySubstitutingEnumDescriptor(this, substitutor)

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

            append(" [")
            append(enumKind.name.lowercase())
            append("]")
        }
    }

    override fun getMemberScope(
        typeSubstitution: TypeSubstitution,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        if (typeSubstitution.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner)

        val substitutor = TypeSubstitutor.create(typeSubstitution)
        return SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor)

    }

    override val kind: ClassKind = ClassKind.ENUM

    override fun getMemberScope(
        typeArguments: List<TypeProjection>,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        assert(typeArguments.size == typeConstructor.parameters.size) {
            "Illegal number of type arguments: expected ${typeConstructor.parameters.size} but was ${typeArguments.size} for $typeConstructor ${typeConstructor.parameters}"
        }
        if (typeArguments.isEmpty()) return getUnsubstitutedMemberScope(cangjieTypeRefiner)

        val substitutor = TypeConstructorSubstitution.create(typeConstructor, typeArguments).buildSubstitutor()
        return SubstitutingScope(getUnsubstitutedMemberScope(cangjieTypeRefiner), substitutor)
    }
}
