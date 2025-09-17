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


import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeSubstitution

val ScopedDescriptor.modality
    get() = when (this) {
        is ClassDescriptor -> this.modality
        is EnumDescriptor -> this.modality

        else -> Modality.FINAL
    }

/**
 * 具有作用域的描述符接口。
 *
 * 表示那些具有成员作用域的声明，能够包含其他声明作为成员。
 * 主要用于类、接口、枚举等可以包含成员的类型声明。
 */
interface ScopedDescriptor : ClassOrPackageFragmentDescriptor {
    /**
     * 表示此分类器的种类，例如 class、interface、enum 等。
     */
    val kind: ClassKind

    /**
     * 根据指定的类型实参列表返回对应的成员作用域。
     *
     * @param typeArguments 类型实参列表
     * @return 返回计算后的成员作用域
     */
    fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope

    /**
     * 根据指定的类型替换规则返回对应的成员作用域。
     *
     * @param typeSubstitution 类型替换映射
     * @return 返回计算后的成员作用域
     */
    fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope

    /**
     * 未进行类型替换时的成员作用域（原始作用域）。
     */
    val unsubstitutedMemberScope: MemberScope

    /**
     * 实例级成员作用域，默认返回空作用域。
     */
    val instanceScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 静态成员作用域（如伴生对象或静态成员的作用域）。
     */
    val staticScope: MemberScope


}

/**
 * 表示类（Class）的描述符，提供类的作用域、构造函数、修饰符、可见性等元信息。
 */
interface ClassDescriptor : ScopedDescriptor, ClassifierDescriptorWithTypeParameters,
    ClassOrPackageFragmentDescriptor {
    /**
     * 根据指定的类型实参列表返回对应的成员作用域。
     *
     * @param typeArguments 类型实参列表
     * @return 返回计算后的成员作用域
     */
    override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope

    /**
     * 根据指定的类型替换规则返回对应的成员作用域。
     *
     * @param typeSubstitution 类型替换映射
     * @return 返回计算后的成员作用域
     */
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope

    /**
     * 类的 this 接收者参数描述符，用于表示类的接收者类型。
     */
    val thisAsReceiverParameter: ReceiverParameterDescriptor

    /**
     * 类的上下文接收者列表（如上下文接收者参数）。
     */
    val contextReceivers: List<ReceiverParameterDescriptor>

    /**
     * 未进行类型替换时的成员作用域（原始作用域）。
     */
    override val unsubstitutedMemberScope: MemberScope

//    val unsubstitutedInnerClassesScope: MemberScope

    /**
     * 实例级成员作用域，默认返回空作用域。
     */
    override val instanceScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 静态成员作用域。
     */
    override val staticScope: MemberScope


    /**
     * 类的所有构造函数集合。
     */
    val constructors: Collection<ClassConstructorDescriptor>


    /**
     * 在类结尾处执行的构造函数集合（项目特定语义）。
     */
    val endConstructors: Collection<ClassConstructorDescriptor>


    /**
     * 包含该类的声明描述符。
     */
    override val containingDeclaration: DeclarationDescriptor

    /**
     * 获取类的默认类型；对于泛型类返回 A<T> 形式的类型。
     */
    override val defaultType: SimpleType

    /**
     * 此描述符对应的类的种类。
     */
    override val kind: ClassKind


    /**
     * 类的调度性（如 final、open 等）。
     */
    override val modality: Modality

    /**
     * 类的可见性（如 public、private 等）。
     */
    override val visibility: DescriptorVisibility


    /**
     * 未替换的主构造函数（如果有）。
     */
    val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?

    /**
     * 如果当前类是 inner 类，则该列表可能与 typeConstructor.parameters 不同；
     * typeConstructor.parameters 可能包含从外部声明捕获的类型参数。
     *
     * @return 返回当前类实际声明的类型参数列表
     */


    override val declaredTypeParameters: List<TypeParameterDescriptor>

    /**
     * 如果这是一个 sealed 类，则返回其直接子类；否则返回空集合。
     */

    val sealedSubclasses: Collection<ClassDescriptor>

    /**
     * 原始的分类符描述符（用于引用未替换的原始描述符）。
     */
    override val original: ClassifierDescriptor

    /**
     * 对于 SAM 接口的备选函数类型（如无法通过更优方式获取时使用）。
     */
    val defaultFunctionTypeForSamInterface: SimpleType?

    /**
     * 快速判定该类是否肯定不是 SAM 接口。可能在某些情况下返回 false，但只有在确定不是 SAM 时才返回 true。
     */
    val isDefinitelyNotSamInterface: Boolean


    /**
     * 判断类是否包含 const 构造函数。
     *
     * @return 如果存在 const 构造函数则返回 true，否则返回 false
     */
    fun hasConstConstructor(): Boolean {
        val constructors =
            this.constructors
        for (constructor in constructors) {
            if (constructor.isConst) {
                return true
            }
        }


        return false
    }
}
