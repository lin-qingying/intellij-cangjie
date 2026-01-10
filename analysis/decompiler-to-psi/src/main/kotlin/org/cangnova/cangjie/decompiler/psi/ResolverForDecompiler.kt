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

package org.cangnova.cangjie.decompiler.psi

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 反编译器的符号解析器接口
 *
 * 该接口定义了反编译过程中需要的符号解析功能。当反编译器需要引用其他类型、
 * 解析继承关系或获取类型信息时，会通过此接口查找对应的 Descriptor。
 *
 * ## 核心职责
 *
 * 1. **类型解析**: 根据类标识符查找类或枚举的 Descriptor
 * 2. **包级声明查找**: 获取包中的所有顶层声明
 * 3. **依赖解析**: 支持反编译过程中的符号引用解析
 *
 * ## 使用场景
 *
 * - **反编译类定义**: 需要解析父类、接口、类型参数等
 * - **反编译函数签名**: 需要解析参数类型、返回类型等
 * - **构建 PSI 树**: 需要获取符号的完整信息以生成正确的 PSI 结构
 * - **类型引用**: 在反编译文本中正确显示类型引用
 *
 * ## 工作原理
 *
 * ```
 * 反编译器遇到类型引用
 *   ↓
 * 通过 ClassId 调用 resolveTopLevelClass()
 *   ↓
 * 查找并返回 ClassDescriptor
 *   ↓
 * 使用 Descriptor 信息生成 PSI 节点
 * ```
 *
 * ## 实现要求
 *
 * 实现类需要能够：
 * - 在当前模块和依赖模块中查找符号
 * - 处理内置类型的解析
 * - 处理标准库类型的解析
 * - 返回 null 表示符号未找到（而不是抛出异常）
 *
 * ## 示例
 *
 * ```kotlin
 * val resolver: ResolverForDecompiler = ...
 *
 * // 解析 std.collection.ArrayList 类
 * val classId = ClassId(FqName("std.collection"), Name.identifier("ArrayList"))
 * val descriptor = resolver.resolveTopLevelClass(classId)
 *
 * // 解析包中的所有声明
 * val declarations = resolver.resolveAllDeclarationsInPackage(FqName("std.collection"))
 * ```
 *
 * @see ClassDescriptor
 * @see EnumDescriptor
 * @see ClassId
 */
interface ResolverForDecompiler {
    /**
     * 解析顶层类
     *
     * 根据类标识符查找对应的类描述符。该方法用于在反编译过程中解析类型引用。
     *
     * ## 查找范围
     *
     * - 当前模块的顶层类
     * - 依赖模块中的可见类
     * - 内置类型（如 Int8, Bool 等）
     * - 标准库类型（如 std.collection.ArrayList）
     *
     * ## 使用场景
     *
     * - **继承关系解析**: 查找父类的 Descriptor
     * - **类型参数解析**: 查找泛型参数的边界类型
     * - **成员类型解析**: 查找属性、参数的类型
     * - **注解类解析**: 查找注解类型
     *
     * ## 返回值
     *
     * - 成功: 返回对应的 [ClassDescriptor]
     * - 失败: 返回 null（类不存在或不可访问）
     *
     * ## 示例
     *
     * ```kotlin
     * // 解析 std.collection.ArrayList
     * val classId = ClassId(FqName("std.collection"), Name.identifier("ArrayList"))
     * val descriptor = resolver.resolveTopLevelClass(classId)
     *
     * // 解析内置类型 cangjie.Int8
     * val int8Id = ClassId(FqName("cangjie"), Name.identifier("Int8"))
     * val int8Descriptor = resolver.resolveTopLevelClass(int8Id)
     * ```
     *
     * @param classId 类的标识符，包含包名和类名
     * @return 对应的类描述符，如果未找到则返回 null
     */
    fun resolveTopLevelClass(classId: ClassId): ClassDescriptor?

    /**
     * 解析顶层枚举
     *
     * 根据类标识符查找对应的枚举描述符。与 [resolveTopLevelClass] 类似，
     * 但专门用于枚举类型的解析。
     *
     * ## 为什么需要单独的枚举解析方法？
     *
     * 枚举在仓颉语言中有特殊的语义：
     * - 枚举值是编译时常量
     * - 枚举有特殊的模式匹配语法
     * - 枚举的反编译需要特殊处理（显示所有枚举值）
     *
     * ## 查找范围
     *
     * 与 [resolveTopLevelClass] 相同，但只返回枚举类型。
     *
     * ## 使用场景
     *
     * - **枚举类型引用**: 解析代码中引用的枚举类型
     * - **枚举值解析**: 查找枚举的所有可能值
     * - **模式匹配**: 验证枚举的完整性检查
     *
     * ## 返回值
     *
     * - 成功: 返回对应的 [EnumDescriptor]
     * - 失败: 返回 null（枚举不存在、不可访问，或指定的类不是枚举）
     *
     * ## 示例
     *
     * ```kotlin
     * // 解析自定义枚举 myapp.Status
     * val enumId = ClassId(FqName("myapp"), Name.identifier("Status"))
     * val enumDescriptor = resolver.resolveTopLevelEnum(enumId)
     *
     * // 获取所有枚举值
     * enumDescriptor?.let {
     *     val entries = it.enumEntries
     * }
     * ```
     *
     * @param classId 枚举的标识符，包含包名和枚举名
     * @return 对应的枚举描述符，如果未找到或不是枚举则返回 null
     */
    fun resolveTopLevelEnum(classId: ClassId): EnumDescriptor?

    /**
     * 解析包中的所有声明
     *
     * 获取指定包下的所有顶层声明，包括类、枚举、函数、变量、类型别名等。
     * 该方法用于在反编译过程中获取包的完整内容。
     *
     * ## 返回的声明类型
     *
     * 返回列表可能包含：
     * - [ClassDescriptor] - 类声明
     * - [EnumDescriptor] - 枚举声明
     * - [FunctionDescriptor] - 顶层函数
     * - [PropertyDescriptor] - 顶层属性/变量
     * - [TypeAliasDescriptor] - 类型别名
     *
     * ## 使用场景
     *
     * - **包级导入**: 解析 `import std.collection.*` 这样的通配符导入
     * - **包浏览**: 在 IDE 中显示包的所有成员
     * - **代码补全**: 提供包中所有可用符号的建议
     * - **反编译包**: 生成整个包的反编译文本
     *
     * ## 可见性过滤
     *
     * 实现类应该只返回对调用者可见的声明：
     * - public 声明总是可见
     * - internal 声明在同一模块内可见
     * - private 声明不应被返回
     *
     * ## 性能考虑
     *
     * 该方法可能需要遍历多个模块，因此可能较慢。
     * 实现类应该考虑使用缓存来优化性能。
     *
     * ## 示例
     *
     * ```kotlin
     * // 获取 std.collection 包中的所有声明
     * val declarations = resolver.resolveAllDeclarationsInPackage(FqName("std.collection"))
     *
     * // 过滤出所有类
     * val classes = declarations.filterIsInstance<ClassDescriptor>()
     *
     * // 过滤出所有顶层函数
     * val functions = declarations.filterIsInstance<FunctionDescriptor>()
     * ```
     *
     * @param packageFqName 包的完全限定名（例如 "std.collection"）
     * @return 包中所有可见的顶层声明，如果包不存在则返回空列表
     */
    fun resolveAllDeclarationsInPackage(packageFqName: FqName): List<DeclarationDescriptor>
}
