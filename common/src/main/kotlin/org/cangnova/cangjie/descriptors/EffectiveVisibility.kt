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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.model.TypeCheckerProviderContext
import org.cangnova.cangjie.types.model.TypeConstructorMarker

/**
 * 有效可见性
 *
 * 表示声明在考虑所有影响因素后的实际可见性。与声明的 [Visibility] 不同，
 * 有效可见性会受到容器类型、类型参数、继承关系等因素的限制。
 *
 * ## 核心概念
 * 声明的有效可见性可能比其声明的可见性更严格：
 * ```
 * internal class InternalClass {
 *     public fun method()  // 有效可见性是 internal，受容器类限制
 * }
 *
 * public class PublicClass<T> {
 *     public fun use(param: T)  // 如果 T 是 internal 类型，方法有效可见性变为 internal
 * }
 * ```
 *
 * ## 可见性层级图
 * ```
 *                    Public (最宽松)
 *                /--/   |  \-------------\
 * Protected(Base)       |                 \
 *       |         Protected(Other)        Internal = PackagePrivate
 * Protected(Derived) |                   /     \
 *             |      |                  /    InternalProtected(Base)
 *       ProtectedBound                 /        \
 *                    \                /       /InternalProtected(Derived)
 *                     \InternalProtectedBound/
 *                              |
 *                        PrivateInFile
 *                              |
 *                   PrivateInClass = Local (最严格)
 * ```
 *
 * @property name 有效可见性的名称
 * @property publicApi 是否为公共 API（可被外部模块访问）
 * @property privateApi 是否为私有 API（仅内部访问）
 */
sealed class EffectiveVisibility(val name: String, val publicApi: Boolean = false, val privateApi: Boolean = false) {
    override fun toString() = name

    /**
     * 类内私有
     *
     * 仅在声明所在的类或接口内部可见。这是最严格的有效可见性。
     */
    object PrivateInClass : EffectiveVisibility("private-in-class", privateApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other || Local == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * 局部可见性
     *
     * 用于局部变量和局部声明，有效性等同于 [PrivateInClass]。
     */
    object Local : EffectiveVisibility("local") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other || PrivateInClass == other) Permissiveness.SAME else Permissiveness.LESS

        override fun toVisibility(): Visibility = Visibilities.Local
    }

    /**
     * 文件内私有
     *
     * 在声明所在的文件内可见。比类内私有更宽松，但仍然是私有 API。
     */
    object PrivateInFile : EffectiveVisibility("private-in-file", privateApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                this -> Permissiveness.SAME
                PrivateInClass, Local -> Permissiveness.MORE
                else -> Permissiveness.LESS
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * 公共可见性
     *
     * 在任何地方都可见，这是最宽松的有效可见性。
     */
    object Public : EffectiveVisibility("public", publicApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            if (this == other) Permissiveness.SAME else Permissiveness.MORE

        override fun toVisibility(): Visibility = Visibilities.Public
    }

    /**
     * Internal 或包私有的抽象基类
     *
     * 处理包级别和模块级别的可见性：
     * - Internal: 当前包及子包可见
     * - PackagePrivate: Java 风格的包私有（用于互操作）
     */
    abstract class InternalOrPackage protected constructor(internal: Boolean) : EffectiveVisibility(
        if (internal) "internal" else "public/*package*/"
    ) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalProtected -> Permissiveness.MORE
                is InternalOrPackage -> Permissiveness.SAME
                ProtectedBound, is Protected -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound, is InternalOrPackage, is InternalProtected -> other
                is Protected -> InternalProtected(other.containerTypeConstructor)
                ProtectedBound -> InternalProtectedBound
            }
    }

    /**
     * Internal 可见性
     *
     * 在当前包及其子包内可见。
     */
    object Internal : InternalOrPackage(true) {
        override fun toVisibility(): Visibility = Visibilities.Internal
    }

    /**
     * 包私有可见性
     *
     * Java 风格的包私有（默认可见性），用于 Java 互操作。
     */
    object PackagePrivate : InternalOrPackage(false) {
        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * Protected 可见性
     *
     * 在模块内可见。当与继承相关时，还需要考虑容器类型的子类型关系。
     *
     * @property containerTypeConstructor 容器类型构造器，用于判断继承关系中的可见性
     */
    class Protected(val containerTypeConstructor: TypeConstructorMarker?) : EffectiveVisibility("protected", publicApi = true) {

        override fun equals(other: Any?) = (other is Protected && containerTypeConstructor == other.containerTypeConstructor)

        override fun hashCode() = containerTypeConstructor?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerTypeConstructor ?: '?'})"

        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> Permissiveness.MORE
                is Protected -> containerRelation(containerTypeConstructor, other.containerTypeConstructor, typeCheckerContextProvider)
                is InternalProtected -> when (containerRelation(
                    containerTypeConstructor,
                    other.containerTypeConstructor,
                    typeCheckerContextProvider
                )) {
                    // Protected 永远不会比 internal & protected 更严格
                    Permissiveness.SAME, Permissiveness.MORE -> Permissiveness.MORE
                    Permissiveness.UNKNOWN, Permissiveness.LESS -> Permissiveness.UNKNOWN
                }
                is InternalOrPackage -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is Protected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.SAME, Permissiveness.LESS -> this
                    Permissiveness.MORE -> other
                    Permissiveness.UNKNOWN -> ProtectedBound
                }
                is InternalProtected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.MORE -> other
                    else -> InternalProtectedBound
                }
                is InternalOrPackage -> InternalProtected(containerTypeConstructor)
            }

        override fun toVisibility(): Visibility = Visibilities.Protected
    }

    /**
     * Protected 下界
     *
     * 当无法确定具体的 protected 继承关系时使用的下界。
     * 表示"在不同类中的 protected"，是所有 protected 可见性的最严格形式。
     */
    object ProtectedBound : EffectiveVisibility("protected (in different classes)", publicApi = true) {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is Protected -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                ProtectedBound -> Permissiveness.SAME
                is InternalOrPackage, is InternalProtected -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public, is Protected -> this
                PrivateInClass, PrivateInFile, Local, ProtectedBound, InternalProtectedBound -> other
                is InternalOrPackage, is InternalProtected -> InternalProtectedBound
            }

        override fun toVisibility(): Visibility = Visibilities.Protected
    }

    /**
     * Internal 与 Protected 的交集
     *
     * 同时受 internal（包及子包）和 protected（模块内）限制的可见性。
     * 这种可见性比单独的 internal 或 protected 都更严格。
     *
     * @property containerTypeConstructor 容器类型构造器，用于处理继承关系
     */
    class InternalProtected(
        val containerTypeConstructor: TypeConstructorMarker?
    ) : EffectiveVisibility("internal & protected", publicApi = false) {

        override fun equals(other: Any?) = (other is InternalProtected && containerTypeConstructor == other.containerTypeConstructor)

        override fun hashCode() = containerTypeConstructor?.hashCode() ?: 0

        override fun toString() = "${super.toString()} (in ${containerTypeConstructor ?: '?'})"

        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> Permissiveness.MORE
                is InternalProtected -> containerRelation(
                    containerTypeConstructor,
                    other.containerTypeConstructor,
                    typeCheckerContextProvider
                )
                is Protected -> when (containerRelation(
                    containerTypeConstructor,
                    other.containerTypeConstructor,
                    typeCheckerContextProvider
                )) {
                    // Internal & protected 永远不会比单独的 protected 更宽松
                    Permissiveness.SAME, Permissiveness.LESS -> Permissiveness.LESS
                    Permissiveness.UNKNOWN, Permissiveness.MORE -> Permissiveness.UNKNOWN
                }
                ProtectedBound -> Permissiveness.UNKNOWN
            }

        override fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
            when (other) {
                Public, is InternalOrPackage -> this
                PrivateInClass, PrivateInFile, Local, InternalProtectedBound -> other
                is Protected, is InternalProtected -> when (relation(other, typeCheckerContextProvider)) {
                    Permissiveness.SAME, Permissiveness.LESS -> this
                    Permissiveness.MORE -> other
                    Permissiveness.UNKNOWN -> InternalProtectedBound
                }
                ProtectedBound -> InternalProtectedBound
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * Internal 与 Protected 下界的交集
     *
     * 当 internal 和 protected 的容器关系无法确定时使用的下界。
     * 这是 internal 和 protected 组合中最严格的形式。
     */
    object InternalProtectedBound : EffectiveVisibility("internal & protected (in different classes)") {
        override fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness =
            when (other) {
                Public, is Protected, is InternalProtected, ProtectedBound, is InternalOrPackage -> Permissiveness.LESS
                PrivateInClass, PrivateInFile, Local -> Permissiveness.MORE
                InternalProtectedBound -> Permissiveness.SAME
            }

        override fun toVisibility(): Visibility = Visibilities.Private
    }

    /**
     * 可见性宽松程度
     *
     * 用于比较两个有效可见性的宽松程度。
     */
    enum class Permissiveness {
        /** 当前可见性更严格（可访问范围更小） */
        LESS,

        /** 两个可见性相同 */
        SAME,

        /** 当前可见性更宽松（可访问范围更大） */
        MORE,

        /** 无法比较（例如 internal 与 protected 在某些情况下） */
        UNKNOWN
    }

    /**
     * 比较与另一个有效可见性的关系
     *
     * 确定当前有效可见性相对于另一个有效可见性的宽松程度。
     * 用于类型检查时判断可见性是否兼容。
     *
     * @param other 要比较的另一个有效可见性
     * @param typeCheckerContextProvider 类型检查上下文提供者，用于判断类型关系
     * @return 宽松程度枚举值
     */
    abstract fun relation(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): Permissiveness

    /**
     * 转换为声明可见性
     *
     * 将有效可见性转换回对应的声明级别可见性。
     * 用于生成错误消息或建议修复。
     *
     * @return 对应的声明可见性
     */
    abstract fun toVisibility(): Visibility

    /**
     * 计算下界
     *
     * 计算当前有效可见性与另一个有效可见性的最严格形式（下界）。
     * 当声明受多个因素限制时，其有效可见性是所有限制的下界。
     *
     * 例如：
     * ```
     * internal class C {
     *     protected fun method()  // 有效可见性是 InternalProtected
     * }
     * ```
     *
     * @param other 另一个有效可见性
     * @param typeCheckerContextProvider 类型检查上下文提供者
     * @return 两个可见性的下界（更严格的那个）
     */
    open fun lowerBound(other: EffectiveVisibility, typeCheckerContextProvider: TypeCheckerProviderContext): EffectiveVisibility =
        when (relation(other, typeCheckerContextProvider)) {
            Permissiveness.SAME, Permissiveness.LESS -> this
            Permissiveness.MORE -> other
            Permissiveness.UNKNOWN -> PrivateInClass
        }
}

/**
 * 类型关系
 *
 * 描述类型在可见性检查中的角色。用于生成更精确的错误消息。
 *
 * @property description 关系的描述文本
 */
enum class RelationToType(val description: String) {
    /** 类型构造器本身 */
    CONSTRUCTOR(""),

    /** 包含声明的容器 */
    CONTAINER(" containing declaration"),

    /** 类型参数 */
    ARGUMENT(" argument"),

    /** 类型参数的包含声明 */
    ARGUMENT_CONTAINER(" argument containing declaration");

    /**
     * 获取容器关系
     *
     * 将当前关系转换为对应的容器关系。
     * 用于递归检查嵌套类型的可见性。
     *
     * @return 对应的容器关系
     */
    fun containerRelation() = when (this) {
        CONSTRUCTOR, CONTAINER -> CONTAINER
        ARGUMENT, ARGUMENT_CONTAINER -> ARGUMENT_CONTAINER
    }

    override fun toString() = description
}

/**
 * 比较容器的类型关系
 *
 * 判断两个类型构造器之间的子类型关系，用于确定 protected 可见性的宽松程度。
 * 如果第一个类型是第二个类型的子类，则 protected 可见性更严格。
 *
 * @param first 第一个类型构造器
 * @param second 第二个类型构造器
 * @param typeCheckerContextProvider 类型检查上下文提供者
 * @return 宽松程度：SAME（相同类型）、LESS（第一个是子类）、MORE（第二个是子类）、UNKNOWN（无关系）
 */
internal fun containerRelation(
    first: TypeConstructorMarker?,
    second: TypeConstructorMarker?,
    typeCheckerContextProvider: TypeCheckerProviderContext
): EffectiveVisibility.Permissiveness {
    return when {
        first == null || second == null -> EffectiveVisibility.Permissiveness.UNKNOWN
        first == second -> EffectiveVisibility.Permissiveness.SAME
        AbstractTypeChecker.isSubtypeOfClass(typeCheckerContextProvider.createTypeCheckerContext(), first, second) ->
            EffectiveVisibility.Permissiveness.LESS
        AbstractTypeChecker.isSubtypeOfClass(typeCheckerContextProvider.createTypeCheckerContext(), second, first) ->
            EffectiveVisibility.Permissiveness.MORE
        else -> EffectiveVisibility.Permissiveness.UNKNOWN
    }
}

/**
 * 创建类型检查器状态
 *
 * 为类型检查创建配置好的状态对象。
 *
 * @return 配置好的类型检查器状态
 */
private fun TypeCheckerProviderContext.createTypeCheckerContext(): TypeCheckerState = newTypeCheckerState(
    errorTypesEqualToAnything = false,
    stubTypesEqualToAnything = true
)