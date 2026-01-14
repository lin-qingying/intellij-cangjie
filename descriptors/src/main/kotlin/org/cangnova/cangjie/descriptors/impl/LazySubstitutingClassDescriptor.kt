/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.getCangJieTypeRefiner
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 懒加载类型替换类描述符
 *
 * 这是实现泛型类型参数化的关键类，通过包装器模式实现类型替换。
 *
 * ## 设计目的
 *
 * 当我们有一个泛型类 `class A<T>` 并使用具体类型参数实例化（如 `A<Int>`, `A<String>`）时，
 * 我们需要一种机制来表示这些不同的类型实例。这个类就是实现这个机制的核心。
 *
 * ## 工作原理
 *
 * ### 包装器模式
 * - **原始描述符** ([original]): 保存对泛型类 `A<T>` 的原始 [ClassDescriptor] 的引用
 * - **类型替换器** ([originalSubstitutor]): 保存类型参数的替换规则（如 T -> Int）
 * - **懒加载**: 实际的替换操作延迟到访问成员时才进行，提高性能
 *
 * ### A<Int> vs A<String> 的类型系统表示
 *
 * 重要概念：`A<Int>` 和 `A<String>` **不是同一个类型**，但它们有以下关系：
 *
 * 1. **共享原始描述符**:
 *    - 两者的 [original] 属性都指向同一个 `ClassDescriptor A`
 *    - 这体现了它们都是从同一个泛型类定义派生的
 *
 * 2. **不同的包装器实例**:
 *    - `A<Int>` 是一个 [LazySubstitutingClassDescriptor] 实例，持有 `ComposableTypeSubstitutor(T -> Int)`
 *    - `A<String>` 是另一个 [LazySubstitutingClassDescriptor] 实例，持有 `ComposableTypeSubstitutor(T -> String)`
 *    - 这两个包装器实例不相等（用 `===` 比较返回 false）
 *
 * 3. **不同的类型表示**:
 *    - 在类型层面（[CangJieType]），它们是完全不同的类型
 *    - 它们有相同的 [typeConstructor]（指向同一个 `ClassDescriptor A`）
 *    - 但有不同的类型参数（[TypeArgument] 列表）
 *
 * 4. **不同的成员类型**:
 *    - 访问成员时，类型参数会被动态替换
 *    - 例如，如果 `A<T>` 有方法 `func foo(): T`
 *    - `A<Int>` 的 `foo()` 返回类型是 `Int`
 *    - `A<String>` 的 `foo()` 返回类型是 `String`
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 假设有泛型类 class A<T> { func foo(): T }
 *
 * val originalDescriptor: ClassDescriptor = ... // A<T> 的原始描述符
 *
 * // 创建 A<Int> 的描述符
 * val intSubstitutor = TypeSubstitutors.create(mapOf(T -> IntType))
 * val aInt = LazySubstitutingClassDescriptor(originalDescriptor, intSubstitutor)
 *
 * // 创建 A<String> 的描述符
 * val stringSubstitutor = TypeSubstitutors.create(mapOf(T -> StringType))
 * val aString = LazySubstitutingClassDescriptor(originalDescriptor, stringSubstitutor)
 *
 * // aInt 和 aString 是不同的描述符
 * assert(aInt !== aString)
 *
 * // 但它们共享同一个原始描述符
 * assert(aInt.original === aString.original)
 * assert(aInt.original === originalDescriptor)
 *
 * // 访问成员时，类型会被替换
 * val fooInAInt = aInt.getMemberScope().getFunction("foo")  // 返回类型: Int
 * val fooInAString = aString.getMemberScope().getFunction("foo")  // 返回类型: String
 * ```
 *
 * ## 性能优化
 *
 * - **懒加载**: [getSubstitutor] 和 [typeConstructor] 使用懒加载模式，只在首次访问时计算
 * - **成员作用域包装**: 通过 [SubstitutingScope] 包装原始作用域，避免预先替换所有成员
 * - **类型构造器缓存**: [typeConstructor] 计算后缓存在 [myTypeConstructor] 中
 *
 * ## 链式替换支持
 *
 * 支持多层类型替换（如 `A<T>` -> `A<B<T>>` -> `A<B<Int>>`）：
 * ```kotlin
 * override fun substitute(substitutor: ComposableTypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
 *     if (substitutor.isEmpty) return this
 *     return LazySubstitutingClassDescriptor(
 *         this,
 *         getSubstitutor().compose(substitutor)
 *     )
 * }
 * ```
 *
 * @param original 原始的泛型类描述符（如 `A<T>`）
 * @param originalSubstitutor 类型参数替换器（如 `T -> Int`）
 *
 * @see ComposableTypeSubstitutor 类型替换器
 * @see SubstitutingScope 替换作用域
 * @see ClassifierDescriptorWithTypeParameters.substitute 替换方法
 */
class LazySubstitutingClassDescriptor(
    override val original: ModuleAwareClassDescriptor,
    private val originalSubstitutor: ComposableTypeSubstitutor
) : ModuleAwareClassDescriptor(), ClassDescriptor {


    private var newSubstitutor: ComposableTypeSubstitutor? = null
    private lateinit var typeConstructorParameters: MutableList<TypeParameterDescriptor>
    private lateinit var myDeclaredTypeParameters: MutableList<TypeParameterDescriptor>
    private var myTypeConstructor: TypeConstructor? = null


    private fun getSubstitutor(): ComposableTypeSubstitutor {
        if (newSubstitutor == null) {
            if (originalSubstitutor.isEmpty) {
                newSubstitutor = originalSubstitutor
            } else {
                val originalTypeParameters = original.typeConstructor.parameters
                typeConstructorParameters = ArrayList(originalTypeParameters.size)

                newSubstitutor = DescriptorSubstitutor.substituteTypeParameters(
                    originalTypeParameters, originalSubstitutor, this, typeConstructorParameters
                )

                myDeclaredTypeParameters =
                    typeConstructorParameters.filter { descriptor: TypeParameterDescriptor ->
                        !descriptor.isCapturedFromOuterDeclaration
                    }.toMutableList()
            }
        }
        return newSubstitutor!!
    }


    override fun getMemberScope(
        substitutor: ComposableTypeSubstitutor,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        val memberScope =
            original.getMemberScope(substitutor, cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }

    override fun getMemberScope(
        typeArguments: List<TypeArgument>,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        val memberScope =
            original.getMemberScope(typeArguments, cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }


    override fun getMemberScope(typeArguments: List<TypeArgument>): MemberScope {
        return getMemberScope(
            typeArguments, DescriptorUtils.getContainingModule(
                this
            ).getCangJieTypeRefiner(

            )
        )

    }


    override fun getMemberScope(substitutor: ComposableTypeSubstitutor): MemberScope {
        return getMemberScope(
            substitutor, DescriptorUtils.getContainingModule(
                this
            ).getCangJieTypeRefiner(

            )
        )

    }

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {

        val memberScope =
            original.getUnsubstitutedMemberScope(cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }


    override val unsubstitutedMemberScope: MemberScope
        get() {
            return getUnsubstitutedMemberScope(
                DescriptorUtils.getContainingModule(
                    original
                ).getCangJieTypeRefiner()
            )
        }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)

    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
        TODO("Not yet implemented")
    }

    override val source: SourceElement = SourceElement.NO_SOURCE

    override val typeConstructor: TypeConstructor
        get() {
            val originalTypeConstructor: TypeConstructor = original.typeConstructor
            if (originalSubstitutor.isEmpty) {
                return originalTypeConstructor
            }

            if (myTypeConstructor == null) {
                val substitutor: ComposableTypeSubstitutor = getSubstitutor()

                val originalSupertypes: Collection<CangJieType> =
                    originalTypeConstructor.supertypes
                val supertypes =
                    ArrayList<CangJieType>(originalSupertypes.size)
                for (supertype in originalSupertypes) {
                    val substituted = substitutor.safeSubstitute(supertype.unwrap())
                    supertypes.add(
                        when (substituted) {
                            is SimpleType -> substituted
                            is FlexibleType -> substituted
                            else -> substituted as CangJieType
                        }
                    )
                }

                myTypeConstructor = ClassTypeConstructorImpl(
                    this,
                    typeConstructorParameters,
                    supertypes,
                    LockBasedStorageManager.NO_LOCKS
                )
            }

            return myTypeConstructor!!
        }
    override val defaultType: SimpleType
        get() {

            val TypeArguments: List<TypeArgument> =
                TypeUtils.getDefaultTypeArguments(
                    typeConstructor.parameters
                )
            return simpleTypeWithNonTrivialMemberScope(
                DefaultTypeAttributeTranslator.toAttributes(annotations, null, null),
                typeConstructor,
                TypeArguments,
                unsubstitutedMemberScope
            )
        }


    override val visibility: DescriptorVisibility
        get() = original.visibility


    override val modality: Modality
        get() = original.modality


    override fun substitute(substitutor: ComposableTypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        if (substitutor.isEmpty) return this
        return LazySubstitutingClassDescriptor(
            this,
            getSubstitutor().compose(substitutor)
        )
    }

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() {

            getSubstitutor()
            return myDeclaredTypeParameters
        }


    override val thisAsReceiverParameter: ReceiverParameterDescriptor
        get() = TODO("Not yet implemented")



    override val staticScope: MemberScope
        get() = original.staticScope

    override val constructors: Collection<ClassConstructorDescriptor>
        get() {

            val originalConstructors: Collection<ClassConstructorDescriptor> =
                (original as ClassDescriptor).constructors
            val result: MutableCollection<ClassConstructorDescriptor> =
                java.util.ArrayList<ClassConstructorDescriptor>(originalConstructors.size)
            for (constructor in originalConstructors) {
                val copy: ClassConstructorDescriptor = constructor.newCopyBuilder()
                    .setOriginal(constructor.original)
                    .setModality(constructor.modality)
                    .setVisibility(constructor.visibility)
                    .setKind(constructor.kind)
                    .setCopyOverrides(false)
                    .build() as ClassConstructorDescriptor
                copy.substitute(getSubstitutor()).let { it?.let { element -> result.add(element) } }
            }
            return result
        }


    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = emptySet()


    override val kind: ClassKind
        get() = (original as ClassDescriptor).kind


    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = (original as ClassDescriptor).unsubstitutedPrimaryConstructor


    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = (original as ClassDescriptor).sealedSubclasses
    override val defaultFunctionTypeForSamInterface: SimpleType?
        get() = substituteSimpleType((original as ClassDescriptor).defaultFunctionTypeForSamInterface)


    private fun substituteSimpleType(type: SimpleType?): SimpleType? {
        if (type == null || originalSubstitutor.isEmpty) return type

        val substitutor: ComposableTypeSubstitutor = getSubstitutor()
        val substitutedType: UnwrappedType = substitutor.safeSubstitute(type.unwrap())

        assert(substitutedType is SimpleType) {
            """
            Substitution for SimpleType should also be a SimpleType, but it is $substitutedType
            Unsubstituted: $type
            """.trimIndent()
        }
        return substitutedType as SimpleType
    }

    override val isDefinitelyNotSamInterface: Boolean
        get() = (original as ClassDescriptor).isDefinitelyNotSamInterface

    override val containingDeclaration: DeclarationDescriptor
        get() = original.containingDeclaration
    override val annotations: Annotations
        get() = original.annotations

    override val name: Name
        get() = original.name

    /**
     * 返回描述符的字符串表示，用于调试
     *
     * 格式: `ClassName<TypeArg1, TypeArg2, ...>`
     *
     * 示例:
     * - `Array<Int>` - 类型参数已替换
     * - `Map<String, List<Int>>` - 嵌套类型参数
     * - `Box<T>` - 未替换的类型参数
     */
    override fun toString(): String {
        return buildString {
            append(name.asString())

            // 获取类型参数
            val typeParams = typeConstructor.parameters
            if (typeParams.isNotEmpty()) {
                append("<")
                typeParams.joinTo(this, ", ") { param ->
                    // 获取替换后的类型
                    val substitutor = getSubstitutor()
                    val substitutedType = substitutor.substitute(param.defaultType )
                    substitutedType?.toString() ?: param.name.asString()
                }
                append(">")
            }
        }
    }
}
