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
package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeSubstitution

/**
 * 枚举描述符接口
 *
 * 基于编译器中的EnumDecl实现，提供枚举类型的完整描述。
 * 枚举在仓颉语言中是一种特殊的类型，支持：
 * - 枚举构造函数（cases）
 * - 枚举成员函数
 * - 继承接口
 * - 泛型支持
 * - 非穷尽性枚举（带...）
 *
 * 示例：
 * ```cangjie
 * enum Color {
 *     Red,
 *     Green,
 *     Blue
 * }
 *
 * enum Result<T> {
 *     Success(T),
 *     Error(String)
 * }
 *
 * enum NonExhaustive {
 *     Case1,
 *     Case2,
 *     ...  // 非穷尽性枚举
 * }
 * ```
 */
interface EnumDescriptor :ClassifierDescriptorWithKind, ClassifierDescriptorWithTypeParameters, ClassOrPackageFragmentDescriptor {

    /**
     * 获取枚举的成员作用域
     *
     * @param typeArguments 类型参数列表
     * @return 成员作用域
     */
    override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope

    /**
     * 获取枚举的成员作用域（使用类型替换）
     *
     * @param typeSubstitution 类型替换
     * @return 成员作用域
     */
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope

    /**
     * 未替换的成员作用域
     */
    override  val unsubstitutedMemberScope: MemberScope

    val isOptionType: Boolean get() = false


    /**
     * 实例作用域
     */
    override  val instanceScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 静态作用域
     */
    override val staticScope: MemberScope

    /**
     * 枚举构造函数列表
     *
     * 枚举构造函数可以是：
     * - VarDecl（无关联值的case）
     * - FuncDecl（有关联值的case）
     */
    val constructors: Collection<EnumConstructorDescriptor>


    /**
     * 是否有关联值
     *
     * 如果枚举case有关联值（如 `Success(T)`），则为true
     */
    val hasArguments: Boolean

    /**
     * 是否为非穷尽性枚举
     *
     * 如果枚举包含 `...` 标记，则为true
     */
    val isNonExhaustive: Boolean

    /**
     * 是否包含省略号
     *
     * 与isNonExhaustive相同，保持与编译器API一致
     */
    val hasEllipsis: Boolean
        get() = isNonExhaustive


    /**
     * 包含声明
     */
    override val containingDeclaration: DeclarationDescriptor

    /**
     * 默认类型
     */
    override val defaultType: SimpleType
    override val kind: ClassKind
        get() = ClassKind.ENUM
    /**
     * 枚举类型
     */
    val enumKind: EnumKind get() = if (isNonExhaustive) EnumKind.NON_EXHAUSTIVE else EnumKind.ENUM


    /**
     * 模态性
     */
    override val modality: Modality

    /**
     * 可见性
     */
    override val visibility: DescriptorVisibility

    /**
     * 声明的类型参数
     */
    override val declaredTypeParameters: List<TypeParameterDescriptor>


    /**
     * 获取所有枚举构造函数
     *
     * @return 所有构造函数（包括继承的）
     */
    fun getAllConstructors(): Collection<EnumConstructorDescriptor>

    /**
     * 检查是否为非穷尽性枚举
     *
     * @return true如果是非穷尽性枚举
     */
    fun isNonExhaustiveEnum(): Boolean = isNonExhaustive
}

/**
 * 枚举构造函数描述符
 *
 * 表示枚举中的单个case，可以是：
 * - 简单case：`Red`
 * - 带关联值的case：`Success(T)`
 */
interface EnumConstructorDescriptor : CallableMemberDescriptor {

    /**
     * 所属的枚举
     */
    override val containingDeclaration: EnumDescriptor

    /**
     * 构造函数名称
     */
    override val name: Name



    /**
     * 是否有关联值
     */
    val hasArguments: Boolean get() = valueParameters.isEmpty()

    /**
     * 构造函数参数
     */
    override val valueParameters: List<ValueParameterDescriptor>

    /**
     * 返回类型（通常是枚举本身）
     */
    override val returnType: CangJieType?


    /**
     * 是否为简单构造函数（无关联值）
     */
    val isSimpleConstructor: Boolean
        get() = !hasArguments

    /**
     * 是否为函数构造函数（有关联值）
     */
    val isFunctionConstructor: Boolean
        get() = hasArguments





    interface CopyBuilder<D : EnumConstructorDescriptor> : CallableMemberDescriptor.CopyBuilder<D> {
        override fun setOwner(owner: DeclarationDescriptor): CopyBuilder<D>

        override fun setModality(modality: Modality): CopyBuilder<D>

        override fun setVisibility(visibility: DescriptorVisibility): CopyBuilder<D>

        override fun setKind(kind: CallableMemberDescriptor.Kind): CopyBuilder<D>

        override fun setCopyOverrides(copyOverrides: Boolean): CopyBuilder<D>

        override fun setName(name: Name): CopyBuilder<D>

        fun setValueParameters(parameters: List<ValueParameterDescriptor>): CopyBuilder<D>

        override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<D>

        override fun setReturnType(type: CangJieType): CopyBuilder<D>

        fun setContextReceiverParameters(contextReceiverParameters: List<ReceiverParameterDescriptor>): CopyBuilder<D>

        fun setExtensionReceiverParameter(extensionReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        override fun setOriginal(original: CallableMemberDescriptor?): CopyBuilder<D>

        fun setSignatureChange(): CopyBuilder<D>

        override fun setPreserveSourceElement(): CopyBuilder<D>

        fun setDropOriginalInContainingParts(): CopyBuilder<D>



        fun setAdditionalAnnotations(additionalAnnotations: Annotations): CopyBuilder<D>

        override fun setSubstitution(substitution: TypeSubstitution): CopyBuilder<D>

        fun <V> putUserData(userDataKey: CallableDescriptor.UserDataKey<V>, value: V): CopyBuilder<D>

        override fun build(): D?
    }

}

/**
 * 枚举类型枚举
 */
enum class EnumKind {
    /** 普通枚举 */
    ENUM,

    /** 非穷尽性枚举 */
    NON_EXHAUSTIVE,

    /** Option类型枚举 */
    OPTION
} 