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

package org.cangnova.cangjie.descriptors.extend

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.types.*

/**
 * 扩展描述符的抽象基类
 *
 * 提供扩展描述符的通用实现，包括：
 * - 扩展 ID 的生成（遵循编译器的 name mangling 策略）
 * - 成员作用域的类型替换
 * - this 接收者参数的创建
 *
 * 扩展 ID 格式（对应编译器 ASTMangler.cpp::MangleExtendDecl）：
 * ```
 * packageName:MangledExtendedType<:MangledInterface1&MangledInterface2&...[@GMangledConstraints]
 * ```
 */
abstract class AbstractExtendDescriptor(
    protected val storageManager: StorageManager,
) : ExtendDescriptor {

    private val _thisAsReceiverParameter: NotNullLazyValue<ReceiverParameterDescriptor> =
        storageManager.createLazyValue {
            LazyExtendReceiverParameterDescriptor(this@AbstractExtendDescriptor)
        }

    override val thisAsReceiverParameter: ReceiverParameterDescriptor
        get() = _thisAsReceiverParameter.invoke()

    // ==================== Extend ID 生成 ====================

    /**
     * 扩展的唯一标识符（延迟计算）
     *
     * 遵循编译器的 name mangling 策略（ASTMangler.cpp::MangleExtendDecl），格式：
     * ```
     * packageName:MangledExtendedType<:MangledInterface1&MangledInterface2&...[@GMangledConstraints]
     * ```
     *
     * 在 Descriptor 层实现（而非 PSI 层）的优势：
     * 1. 使用规范化的类型表示（FQN + 类型参数）而非文本
     * 2. 正确处理导入别名（Comparable vs Cmp）
     * 3. 包含泛型约束的 mangled 形式
     * 4. 排序确保一致性
     */
    private val _extendId: NotNullLazyValue<String> = storageManager.createLazyValue {
        buildExtendId()
    }

    override val extendId: String
        get() = _extendId.invoke()

    /**
     * 构建扩展的唯一标识符
     *
     * 对应编译器实现: ASTMangler.cpp::MangleExtendDecl
     *
     * 格式: `packageName:MangledExtendedType<:MangledInterface1&MangledInterface2&...[@GMangledConstraints]`
     */
    protected open fun buildExtendId(): String {
        val parts = mutableListOf<String>()

        // 1. 包名前缀
        val packageFqName = (containingDeclaration as? PackageFragmentDescriptor)?.fqName?.asString() ?: ""
        parts.add(packageFqName)
        parts.add(":")

        // 2. 被扩展类型的 mangled 形式
        parts.add(mangleType(extendType))

        // 3. 分隔符（对应编译器的 MANGLE_LT_COLON_PREFIX）
        parts.add("<:")

        // 4. 排序的接口列表（用 & 连接）
        val mangledInterfaces = superTypes
            .map { mangleType(it) }
            .sorted()  // 按 mangled 字符串排序，确保一致性
        parts.add(mangledInterfaces.joinToString("&"))

        // 5. 泛型约束（如果有）
        val constraintsMangled = mangleGenericConstraints()
        if (constraintsMangled.isNotEmpty()) {
            parts.add(constraintsMangled)
        }

        return parts.joinToString("")
    }

    /**
     * Mangle 类型
     *
     * 将类型转换为规范化的字符串表示，格式：
     * - 简单类型: `packageName.TypeName`
     * - 泛型类型: `packageName.TypeName<Arg1,Arg2>`
     * - 类型参数: `TypeParam[index]`
     *
     * 示例：
     * - `std.collection.Array<Int>` → `std.collection.Array<std.builtin.Int>`
     * - `std.ops.Comparable<T>` → `std.ops.Comparable<TypeParam0>`
     *
     * @param type 要 mangle 的类型
     * @return mangled 字符串
     */
    protected fun mangleType(type: CangJieType): String {
        return when (type) {
            is SimpleType -> {
                val constructor = type.constructor
                val declarationDescriptor = constructor.declarationDescriptor

                when (declarationDescriptor) {
                    // 类型参数：使用索引表示
                    is TypeParameterDescriptor -> {
                        val index = declaredTypeParameters.indexOf(declarationDescriptor)
                        if (index >= 0) {
                            "TypeParam$index"
                        } else {
                            // 外层类型参数，使用名称
                            "TypeParam[${declarationDescriptor.name}]"
                        }
                    }

                    // 类、接口、结构体等：使用 FQN
                    is ClassDescriptor -> {
                        val fqName = declarationDescriptor.fqNameUnsafe.asString()
                        if (type.arguments.isEmpty()) {
                            fqName
                        } else {
                            // 泛型类型：递归 mangle 类型参数
                            val args = type.arguments.joinToString(",") { arg ->
                                when (arg) {
                                    is TypeArgumentImpl -> mangleType(arg.type)
                                    else -> arg.toString()
                                }
                            }
                            "$fqName<$args>"
                        }
                    }

                    // 类型别名：解析后的实际类型
                    is TypeAliasDescriptor -> {
                        val expandedType = declarationDescriptor.expandedType
                        mangleType(expandedType)
                    }

                    else -> {
                        // 回退：使用完整字符串表示
                        type.toString()
                    }
                }
            }

            // 其他类型（如 FlexibleType）：使用字符串表示
            else -> type.toString()
        }
    }

    /**
     * Mangle 泛型约束
     *
     * 对应编译器实现: ASTMangler.cpp::MangleGenericConstraints
     *
     * 格式: `@G<TypeParam>:<Bound1>:<Bound2>...`
     *
     * 示例：
     * ```
     * where T <: Comparable<T> & Hashable
     * →  @GTypeParam0:std.ops.Comparable<TypeParam0>:std.ops.Hashable
     * ```
     */
    protected fun mangleGenericConstraints(): String {
        if (declaredTypeParameters.isEmpty()) {
            return ""
        }

        val builder = StringBuilder()

        // 收集所有约束并排序
        data class Constraint(
            val param: TypeParameterDescriptor,
            val bound: CangJieType,
            val paramMangled: String,
            val boundMangled: String
        )

        val constraints = declaredTypeParameters.flatMap { param ->
            param.upperBounds.map { bound ->
                Constraint(
                    param = param,
                    bound = bound,
                    paramMangled = mangleType(param.defaultType),
                    boundMangled = mangleType(bound)
                )
            }
        }.sortedWith(compareBy({ it.paramMangled }, { it.boundMangled }))

        // 按类型参数分组
        val groupedByParam = constraints.groupBy { it.param }

        for ((param, paramConstraints) in groupedByParam) {
            builder.append("@G")  // MANGLE_GEXTEND_PREFIX
            builder.append(mangleType(param.defaultType))

            // 排序 upper bounds
            val sortedBounds = paramConstraints
                .map { it.boundMangled }
                .sorted()

            for (bound in sortedBounds) {
                builder.append(":")
                builder.append(bound)
            }
        }

        return builder.toString()
    }

    override fun <R, D> accept(
        visitor: DeclarationDescriptorVisitor<R, D>,
        data: D
    ): R? {
        return visitor.visitExtendDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
        visitor.visitExtendDescriptor(this, null)
    }


    override val extendTypeConstructor: TypeConstructor
        get() = extendType.constructor


    override fun getMemberScope(
        typeArguments: List<TypeArgument>,

        ): MemberScope {
        assert(typeArguments.size == declaredTypeParameters.size) {
            "Illegal number of type arguments: expected ${declaredTypeParameters.size} but was ${typeArguments.size} for   ${declaredTypeParameters}"
        }
        if (typeArguments.isEmpty()) return unsubstitutedMemberScope

        val substitutorMap = declaredTypeParameters.zip(typeArguments).associate { (param, arg) ->
            param.typeConstructor to arg.type.unwrap()
        }
        val substitutor = ComposableTypeSubstitutor.create(substitutorMap)

        return SubstitutingScope(unsubstitutedMemberScope, substitutor)
    }

    override fun getMemberScope(
        substitutor: ComposableTypeSubstitutor,

        ): MemberScope {
        if (substitutor.isEmpty) return unsubstitutedMemberScope

        return SubstitutingScope(unsubstitutedMemberScope, substitutor)
    }

    override val staticScope: MemberScope
        get() = MemberScope.Empty

    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.PUBLIC
    override val original: ExtendDescriptor
        get() = this

}

class ExtendDescriptorImpl(
    override val extendType: CangJieType,
    override val superTypes: List<CangJieType>,
    override val declaredTypeParameters: List<TypeParameterDescriptor>,
    override val extendId: String,
    override val containingDeclaration: DeclarationDescriptor,
    storageManager: StorageManager,

    ) : AbstractExtendDescriptor(

    storageManager
) {
    override val unsubstitutedMemberScope: MemberScope
        get() = TODO("Not yet implemented")
    override val modality: Modality
        get() = TODO("Not yet implemented")



}