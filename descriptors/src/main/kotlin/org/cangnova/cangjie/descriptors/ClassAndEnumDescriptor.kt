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

import org.cangnova.cangjie.types.SimpleType

/**
 * 类和枚举描述符的公共接口
 *
 * 该接口提取了 [ClassDescriptor] 和 [EnumDescriptor] 的共同特性，
 * 用于统一处理类和枚举类型的场景。
 *
 * ## 设计目标
 *
 * 1. **统一类型处理**：在类型解析、类型检查等场景中，可以统一处理类和枚举
 * 2. **代码复用**：避免为类和枚举编写重复的处理逻辑
 * 3. **类型安全**：通过接口约束，确保只有类和枚举能够使用相关功能
 *
 * ## 共同特性
 *
 * 类和枚举都具有以下特性：
 * - **类型能力**：可以作为类型使用（通过 [ClassifierDescriptorWithTypeParameters]）
 * - **继承能力**：可以继承接口或被继承（通过 [InheritableDescriptor]）
 * - **作用域**：包含成员声明（通过 [HasScopeDescriptor]）
 * - **泛型支持**：支持类型参数（通过 [ClassifierDescriptorWithTypeParameters]）
 * - **可见性控制**：具有访问修饰符（通过 [DeclarationDescriptorWithVisibility]）
 * - **类型构造器**：可以构造类型实例（通过 [ClassifierDescriptorWithTypeConstructor]）
 *
 * ## 使用场景
 *
 * ```kotlin
 * // 类型解析
 * fun resolveTypeForClassOrEnum(
 *     descriptor: ClassAndEnumDescriptor,
 *     context: TypeResolutionContext
 * ): CangJieType {
 *     val typeConstructor = descriptor.typeConstructor
 *     val parameters = descriptor.declaredTypeParameters
 *     // ... 统一的类型解析逻辑
 * }
 *
 * // 成员解析
 * fun resolveMember(descriptor: ClassAndEnumDescriptor, name: Name): DeclarationDescriptor? {
 *     return descriptor.unsubstitutedMemberScope.findMember(name)
 * }
 * ```
 *
 * ## 实现类
 *
 * - [ClassDescriptor]：类、接口、结构体等类型声明
 * - [EnumDescriptor]：枚举类型声明
 *
 * @see ClassDescriptor
 * @see EnumDescriptor
 * @see ClassifierDescriptorWithTypeParameters
 * @see InheritableDescriptor
 */
interface ClassAndEnumDescriptor :
    DeclarationDescriptor,
    ClassifierDescriptorWithTypeParameters,
    InheritableDescriptor,
    HasScopeDescriptor,
    ClassOrPackageFragmentDescriptor,
    DeclarationDescriptorWithVisibility,
    ClassifierDescriptorWithTypeConstructor {
    /**
     * 类的 this 接收者参数描述符，用于表示类的接收者类型。
     */
    val thisAsReceiverParameter: ReceiverParameterDescriptor

    /**
     * 包含该类或枚举的声明描述符
     *
     * 可能是：
     * - [PackageFragmentDescriptor]：顶层声明
     * - [ClassDescriptor]：嵌套类/枚举
     * - [FunctionDescriptor]：局部类/枚举
     */
    override val containingDeclaration: DeclarationDescriptor

    /**
     * 获取类或枚举的默认类型
     *
     * 对于泛型类/枚举，返回带有类型参数的类型，例如 `List<T>` 或 `Result<T>`。
     * 对于非泛型类/枚举，返回简单类型，例如 `String` 或 `Color`。
     *
     * @return 默认类型
     */
    override val defaultType: SimpleType

    /**
     * 类或枚举的模态性（Modality）
     *
     * - [Modality.FINAL]：不可继承（默认）
     * - [Modality.OPEN]：可以被继承
     * - [Modality.ABSTRACT]：抽象类，必须被继承
     * - [Modality.SEALED]：密封类，只能在同一文件中继承
     */
    override val modality: Modality

    /**
     * 类或枚举的可见性
     *
     * - [DescriptorVisibilities.PUBLIC]：公开可见
     * - [DescriptorVisibilities.PRIVATE]：仅当前文件可见
     * - [DescriptorVisibilities.PROTECTED]：子类可见
     * - [DescriptorVisibilities.INTERNAL]：模块内可见
     */
    override val visibility: DescriptorVisibility

    /**
     * 声明的类型参数列表
     *
     * 例如：
     * - `class Box<T>` 的类型参数为 `[T]`
     * - `enum Result<T, E>` 的类型参数为 `[T, E]`
     * - `class String` 的类型参数为空列表
     *
     * @return 类型参数列表
     */
    override val declaredTypeParameters: List<TypeParameterDescriptor>

    /**
     * 原始的类或枚举描述符
     *
     * 在类型替换场景中，指向未替换的原始描述符。
     * 例如，`List<String>` 的 original 指向 `List<T>`。
     */
    override val original: ClassAndEnumDescriptor

    /**
     * 类或枚举的类别
     *
     * @return 类别枚举值
     */
    override val kind: ClassKind
}