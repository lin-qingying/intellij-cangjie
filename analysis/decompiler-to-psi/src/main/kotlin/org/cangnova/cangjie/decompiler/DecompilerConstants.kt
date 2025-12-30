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

package org.cangnova.cangjie.decompiler

/**
 * 反编译器常量定义
 *
 * ## 功能说明
 *
 * 此文件包含反编译过程中使用的所有常量，统一管理以避免重复定义。
 *
 * ## 常量分类
 *
 * ### 1. 编译代码占位符
 *
 * 用于表示无法从元数据中恢复的代码：
 * - [COMPILED_CODE]: 通用占位符
 * - [COMPILED_DEFAULT_PARAMETER_VALUE]: 参数默认值占位符
 * - [COMPILED_DEFAULT_INITIALIZER]: 变量初始化表达式占位符
 *
 * ### 2. 反编译注释
 *
 * 用于在反编译文本中标记特殊内容：
 * - [DECOMPILED_CODE_COMMENT]: 方法体占位符注释
 * - [DECOMPILED_COMMENT_FOR_PARAMETER]: 参数默认值注释
 * - [FLEXIBLE_TYPE_COMMENT]: 平台类型注释
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 函数参数默认值
 * fun foo(x: Int = COMPILED_DEFAULT_PARAMETER_VALUE) { ... }
 * // 渲染为：func foo(x: Int /* = compiled code */) { ... }
 *
 * // 变量初始化
 * var count: Int = COMPILED_DEFAULT_INITIALIZER
 * // 渲染为：var count: Int = COMPILED_CODE
 *
 * // 方法体
 * func bar() { DECOMPILED_CODE_COMMENT }
 * // 渲染为：func bar() { /* compiled code */ }
 * ```
 */

/**
 * 通用编译代码占位符
 *
 * 用于表示无法从元数据恢复的代码片段。
 */
internal const val COMPILED_CODE = "/* compiled code */"

/**
 * 函数参数默认值占位符
 *
 * 用于表示函数参数的默认值表达式，因为元数据中不包含默认值的具体代码。
 *
 * ## 使用场景
 *
 * ```kotlin
 * func foo(x: Int = 42, name: String = "default")
 * ```
 *
 * 反编译后：
 * ```kotlin
 * func foo(x: Int = COMPILED_DEFAULT_PARAMETER_VALUE, name: String = COMPILED_DEFAULT_PARAMETER_VALUE)
 * ```
 *
 * ## 渲染格式
 *
 * 在反编译文本中，会被渲染为注释形式：
 * ```kotlin
 * func foo(x!: Int = COMPILED_CODE, name!: String = COMPILED_CODE)
 * ```
 *
 * @see DECOMPILED_COMMENT_FOR_PARAMETER
 */
internal const val COMPILED_DEFAULT_PARAMETER_VALUE = "COMPILED_CODE"

/**
 * 变量初始化表达式占位符
 *
 * 用于表示变量的初始化表达式，因为元数据中不包含初始化代码。
 *
 * ## 使用场景
 *
 * ```kotlin
 * let PI: Float64 = 3.14159
 * var counter: Int = 0
 * ```
 *
 * 反编译后：
 * ```cangjie
 * let PI: Float64 = COMPILED_DEFAULT_INITIALIZER
 * var counter: Int = COMPILED_DEFAULT_INITIALIZER
 * ```
 *
 * ## 渲染格式
 *
 * 在反编译文本中，直接显示为标识符：
 * ```cangjie
 * let PI: Float64 = COMPILED_CODE
 * var counter: Int = COMPILED_CODE
 * ```
 *
 * @see COMPILED_DEFAULT_PARAMETER_VALUE
 */
internal const val COMPILED_DEFAULT_INITIALIZER = "COMPILED_CODE"

/**
 * 方法体占位符注释
 *
 * 用于在反编译文本中表示方法体，因为元数据中不包含方法实现。
 *
 * ## 使用场景
 *
 * 用于所有有方法体的声明：
 * - 普通函数
 * - 构造函数
 * - 属性访问器（getter/setter）
 * - Main 函数
 *
 * ## 渲染格式
 *
 * ```kotlin
 * func foo(): Int { /* compiled code */ }
 *
 * let value: String
 *   get() { /* compiled code */ }
 *   set(v) { /* compiled code */ }
 * ```
 *
 * ## 与抽象方法的区别
 *
 * 抽象方法没有方法体，因此不使用此注释：
 * ```kotlin
 * abstract func bar(): Int  // 无方法体，不使用注释
 * ```
 */
internal const val DECOMPILED_CODE_COMMENT = "/* compiled code */"

/**
 * 参数默认值注释
 *
 * 用于在反编译文本中标记参数具有默认值，但默认值代码无法恢复。
 *
 * ## 渲染格式
 *
 * ```kotlin
 * func foo(x: Int /* = compiled code */, name: String /* = compiled code */)
 * ```
 *
 * ## 与实际默认值的对比
 *
 * | 情况 | 源码 | 反编译 |
 * |------|------|--------|
 * | 有默认值 | `func foo(x: Int = 42)` | `func foo(x: Int /* = compiled code */)` |
 * | 无默认值 | `func foo(x: Int)` | `func foo(x: Int)` |
 *
 * @see COMPILED_DEFAULT_PARAMETER_VALUE
 */
internal const val DECOMPILED_COMMENT_FOR_PARAMETER = "/* = compiled code */"

/**
 * 平台类型注释
 *
 * 用于标记平台相关的灵活类型，这些类型在不同平台上可能有不同的表示。
 *
 * ## 使用场景
 *
 * 主要用于与 Java 或其他平台互操作时的可空性不确定的类型：
 * ```kotlin
 * func getPlatformString(): String /* platform type */
 * ```
 *
 * ## 平台类型特点
 *
 * - **可空性不确定**: 既可以当作可空类型，也可以当作非空类型使用
 * - **平台依赖**: 实际行为取决于平台实现
 * - **安全性提示**: 提醒开发者注意潜在的空指针风险
 *
 * ## 与普通类型的区别
 *
 * | 类型 | 声明 | 说明 |
 * |------|------|------|
 * | 非空类型 | `String` | 保证非空 |
 * | 可空类型 | `String?` | 显式可空 |
 * | 平台类型 | `String /* platform type */` | 可空性未知 |
 */
internal const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"