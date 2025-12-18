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
package org.cangnova.cangjie

/**
 * 只读标记注解
 *
 * 该注解用于标记函数、属性、字段、参数或局部变量为只读的，
 * 表明它们不应该被修改。
 *
 * 使用场景：
 * - 标记不应修改的数据结构
 * - 文档化只读约束
 * - 配合静态分析工具检查只读约束
 *
 * 适用目标：
 * - 函数：表明函数不修改外部状态
 * - 属性 getter/setter：标记只读属性
 * - 字段：标记不应修改的字段
 * - 参数：标记不应修改的参数
 * - 局部变量：标记不应修改的局部变量
 *
 * 示例：
 * ```kotlin
 * @ReadOnly
 * fun process(data: List<String>) {
 *     // 不修改 data
 * }
 *
 * @ReadOnly
 * val config: Config
 *     get() = ...
 *
 * fun compute(@ReadOnly input: Data) {
 *     // 不修改 input
 * }
 * ```
 *
 * 注意：这是一个文档化注解，编译器不会强制执行只读约束。
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE
)
annotation class ReadOnly 
