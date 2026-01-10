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

package org.cangnova.cangjie.chir

/**
 * 表示一个可以被注解的 CHIR 元素容器。
 *
 * 该接口为所有支持注解的 CHIR 元素提供统一的注解访问能力。
 * 在仓颉语言中，许多语言结构都可以被注解修饰，包括：
 * - 声明（类、函数、属性等）
 * - 类型引用
 * - 表达式（在某些情况下）
 *
 * ## 注解的用途
 * - **元数据**: 为代码添加额外的描述信息
 * - **编译时处理**: 触发注解处理器生成代码或执行验证
 * - **运行时反射**: 通过反射 API 在运行时访问注解信息
 * - **工具支持**: IDE、构建工具、框架使用注解提供特定功能
 *
 * ## 示例
 * ```kotlin
 * @Deprecated("Use newFunction instead")
 * @JvmName("oldFunc")
 * fun oldFunction() { ... }
 * ```
 *
 * @see ChirAnnotation 注解的完整表示
 * @see ChirDeclaration 声明接口，继承此接口以支持注解
 */
interface ChirAnnotationContainer {
    /**
     * 该元素上的注解列表。
     *
     * 按照在源代码中声明的顺序返回所有注解。
     * 如果元素没有注解，则返回空列表。
     *
     * ## 注意事项
     * - 列表按源码顺序排列
     * - 包括所有保留级别（SOURCE、BINARY、RUNTIME）的注解
     * - 不包括从父类或接口继承的注解
     *
     * @return 注解列表，如果没有注解则为空列表
     */
    val annotations: List<ChirAnnotation>
}

/**
 * 表示一个 CHIR 注解。
 *
 * 注解是附加在程序元素上的元数据，用于提供额外的信息或触发特定的处理逻辑。
 * 注解由注解类型和可选的参数列表组成。
 *
 * ## 注解结构
 * ```kotlin
 * @AnnotationType(arg1 = value1, arg2 = value2, ...)
 * ```
 *
 * ## 组成部分
 * - [annotationType]: 注解的类型标识
 * - [arguments]: 注解的参数列表（可能为空）
 *
 * ## 示例
 * ```kotlin
 * // 无参数注解
 * @Override
 * fun toString(): String { ... }
 *
 * // 带参数注解
 * @Deprecated(message = "Use new API", level = DeprecationLevel.ERROR)
 * class OldClass { ... }
 *
 * // 数组参数
 * @SuppressWarnings(["unchecked", "rawtypes"])
 * val list = ArrayList()
 *
 * // 嵌套注解
 * @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
 * @Retention(AnnotationRetention.RUNTIME)
 * annotation class MyAnnotation
 * ```
 *
 * ## 使用场景
 * - **标记**: 无参数注解，作为标记使用（如 @Override、@Deprecated）
 * - **配置**: 带参数注解，提供配置信息（如 @JvmName、@JvmStatic）
 * - **验证**: 编译时检查和验证（如 @NonNull、@Range）
 * - **生成**: 触发代码生成（如 @Entity、@Serializable）
 *
 * @see ChirAnnotationType 注解类型信息
 * @see ChirAnnotationArgument 注解参数
 * @see ChirAnnotationValue 注解参数值
 * @see ChirAnnotationContainer 可被注解的元素容器
 */
interface ChirAnnotation : ChirElement {
    /**
     * 注解的类型信息。
     *
     * 标识这是哪个注解类的使用，包含注解的完全限定名。
     *
     * @return 注解类型
     * @see ChirAnnotationType
     */
    val annotationType: ChirAnnotationType

    /**
     * 注解的参数列表。
     *
     * 包含所有显式提供的参数值。未提供的参数将使用默认值（如果有）。
     * 参数可以是命名参数或位置参数。
     *
     * ## 参数类型
     * - 命名参数: `@Annotation(name = value)`
     * - 位置参数: `@Annotation(value)` (通常用于单参数注解)
     *
     * @return 参数列表，如果没有参数则为空列表
     * @see ChirAnnotationArgument
     */
    val arguments: List<ChirAnnotationArgument>

    /**
     * 接受访问者访问此注解。
     *
     * 重写父接口的默认实现，将调用委托给 [ChirVisitor.visitAnnotation]，
     * 以便访问者可以专门处理注解类型的元素。
     *
     * @param R 访问者返回类型
     * @param D 传递给访问者的数据类型
     * @param visitor 访问此注解的访问者
     * @param data 传递给访问者的上下文数据
     * @return 访问者处理此注解后的结果
     *
     * @see ChirVisitor.visitAnnotation
     */
    override fun <R, D> accept(visitor: ChirVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)
}

/**
 * 注解类型信息。
 *
 * 表示注解的类型标识，用于区分不同的注解类。
 * 通过完全限定名唯一标识一个注解类型。
 *
 * ## 示例
 * ```kotlin
 * @kotlin.Deprecated  // qualifiedName = "kotlin.Deprecated"
 * @org.example.Custom // qualifiedName = "org.example.Custom"
 * ```
 *
 * @see ChirAnnotation 使用此类型的注解
 */
interface ChirAnnotationType {
    /**
     * 注解的完全限定名。
     *
     * 包含包名和类名的完整路径，用于唯一标识注解类型。
     * 格式: `package.name.AnnotationName`
     *
     * ## 用途
     * - 注解查找和匹配
     * - 注解处理器的类型判断
     * - 代码生成和分析
     *
     * @return 完全限定名，例如 "kotlin.Deprecated"、"javax.annotation.Nullable"
     */
    val qualifiedName: String
}

/**
 * 注解参数。
 *
 * 表示注解中的一个参数，包括参数名称和参数值。
 * 参数可以是命名参数（有名称）或位置参数（名称为 null）。
 *
 * ## 参数形式
 * - **命名参数**: `name = value` - 显式指定参数名
 * - **位置参数**: `value` - 省略参数名，通常用于单参数注解或 value 参数
 *
 * ## 示例
 * ```kotlin
 * @Deprecated(message = "Old API")        // 命名参数: name = "message"
 * @Suppress("UNCHECKED_CAST")             // 位置参数: name = null (隐式为 "value")
 * @Range(from = 0, to = 100)              // 多个命名参数
 * ```
 *
 * @see ChirAnnotation 包含此参数的注解
 * @see ChirAnnotationValue 参数值的表示
 */
interface ChirAnnotationArgument {
    /**
     * 参数名称（可选）。
     *
     * 对于命名参数，返回显式的参数名。
     * 对于位置参数（通常是单参数注解的 value 参数），可能为 null。
     *
     * ## 示例
     * ```kotlin
     * @Deprecated(message = "...")  // name = "message"
     * @Suppress("...")               // name = null (隐式为 "value")
     * ```
     *
     * @return 参数名称，位置参数时为 null
     */
    val name: String?

    /**
     * 参数值。
     *
     * 注解参数的值，可以是常量、数组、枚举、类引用或嵌套注解。
     *
     * @return 参数值
     * @see ChirAnnotationValue 参数值的各种类型
     */
    val value: ChirAnnotationValue
}

/**
 * 注解参数值的类型层次结构。
 *
 * 这是一个密封接口，定义了注解参数可能的值类型。
 * 注解参数值必须是编译时常量，支持以下类型：
 *
 * - [ConstantValue]: 基本类型和字符串常量
 * - [ArrayValue]: 数组（包括其他注解值的数组）
 * - [EnumValue]: 枚举常量引用
 * - [ClassValue]: 类型引用（KClass）
 * - [AnnotationValue]: 嵌套注解
 *
 * ## 设计说明
 * 使用密封接口确保类型安全，通过 when 表达式可以穷尽所有可能的值类型。
 *
 * ## 示例
 * ```kotlin
 * when (value) {
 *     is ConstantValue -> // 处理常量
 *     is ArrayValue -> // 处理数组
 *     is EnumValue -> // 处理枚举
 *     is ClassValue -> // 处理类引用
 *     is AnnotationValue -> // 处理嵌套注解
 * }
 * ```
 *
 * @see ChirAnnotationArgument.value 使用此类型的地方
 */
sealed interface ChirAnnotationValue {
    /**
     * 常量值（字符串、数字、布尔等）。
     *
     * 表示注解参数中的基本类型常量值，包括：
     * - 数值类型: Byte, Short, Int, Long, Float, Double
     * - 布尔类型: Boolean
     * - 字符类型: Char
     * - 字符串类型: String
     *
     * ## 示例
     * ```kotlin
     * @Range(from = 0, to = 100)         // ConstantValue(0), ConstantValue(100)
     * @Deprecated(message = "Old API")   // ConstantValue("Old API")
     * @JvmField                           // 无参数
     * ```
     *
     * @property value 常量值，类型为 Any?（可能为 null 用于特殊情况）
     */
    data class ConstantValue(val value: Any?) : ChirAnnotationValue

    /**
     * 数组值。
     *
     * 表示注解参数中的数组，数组元素本身也是 [ChirAnnotationValue]，
     * 允许嵌套结构（如数组中包含注解）。
     *
     * ## 示例
     * ```kotlin
     * @SuppressWarnings(["unchecked", "rawtypes"])
     * // ArrayValue(values = [ConstantValue("unchecked"), ConstantValue("rawtypes")])
     *
     * @Target([AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
     * // ArrayValue(values = [EnumValue(...), EnumValue(...)])
     * ```
     *
     * @property values 数组元素列表
     */
    data class ArrayValue(val values: List<ChirAnnotationValue>) : ChirAnnotationValue

    /**
     * 枚举值。
     *
     * 表示注解参数中的枚举常量引用。
     * 包含枚举类型的完全限定名和枚举项名称。
     *
     * ## 示例
     * ```kotlin
     * @Retention(AnnotationRetention.RUNTIME)
     * // EnumValue(enumType = "kotlin.annotation.AnnotationRetention", enumEntry = "RUNTIME")
     *
     * @Target(AnnotationTarget.CLASS)
     * // EnumValue(enumType = "kotlin.annotation.AnnotationTarget", enumEntry = "CLASS")
     * ```
     *
     * @property enumType 枚举类型的完全限定名
     * @property enumEntry 枚举项的名称
     */
    data class EnumValue(val enumType: String, val enumEntry: String) : ChirAnnotationValue

    /**
     * 类引用值。
     *
     * 表示注解参数中的类型引用（KClass）。
     * 通常用于指定类型信息，例如异常类、序列化器类等。
     *
     * ## 示例
     * ```kotlin
     * @Throws(IOException::class)
     * // ClassValue(classType = "java.io.IOException")
     *
     * @Serializable(with = CustomSerializer::class)
     * // ClassValue(classType = "com.example.CustomSerializer")
     * ```
     *
     * @property classType 类型的完全限定名
     */
    data class ClassValue(val classType: String) : ChirAnnotationValue

    /**
     * 嵌套注解。
     *
     * 表示注解参数中的嵌套注解。
     * 允许在注解中使用其他注解作为参数值，形成复杂的注解结构。
     *
     * ## 示例
     * ```kotlin
     * @Component(
     *     value = "userService",
     *     scope = @Scope(value = "singleton")
     * )
     * // 外层注解包含一个 AnnotationValue，其中包含 @Scope 注解
     *
     * @RequestMapping(
     *     method = [RequestMethod.GET],
     *     headers = [@Header(name = "Content-Type", value = "application/json")]
     * )
     * // 数组中包含嵌套注解
     * ```
     *
     * @property annotation 嵌套的注解
     */
    data class AnnotationValue(val annotation: ChirAnnotation) : ChirAnnotationValue
}