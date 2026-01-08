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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider
import org.cangnova.cangjie.types.CangJieType

/**
 * 合成作用域接口
 *
 * 合成作用域用于提供编译器自动生成的成员,这些成员在源代码中不存在,但在语义分析时需要被识别。
 * 这是一种扩展语言功能的机制,无需修改源代码就能为类型添加额外的功能。
 *
 * ## 合成成员的类型
 *
 * 1. **合成扩展属性**: 为类型自动添加的扩展属性
 * 2. **合成成员函数**: 为类型自动添加的成员函数
 * 3. **合成静态函数**: 为类型自动添加的静态函数
 * 4. **合成构造器**: 为类型自动生成的构造函数
 *
 * ## 使用场景
 *
 * - 函数式接口(SAM)的自动转换构造器
 * - 数据类的自动生成方法(如 toString, equals, hashCode)
 * - 枚举类的自动生成方法(如 values, valueOf)
 * - 编译器内置的类型转换和操作符重载
 *
 * ## 实现说明
 *
 * 具体的合成作用域实现需要实现此接口的所有方法。对于不需要提供某种合成成员的实现,
 * 可以继承 [Default] 类,它提供了所有方法的空实现。
 *
 * ## 示例
 *
 * ```cangjie
 * // 函数式接口
 * interface Comparator<T> {
 *     func compare(a: T, b: T): Int64
 * }
 *
 * // 编译器自动合成一个构造器,允许 lambda 转换:
 * let cmp = Comparator<Int64> { a, b -> a - b }
 * ```
 *
 * @see SyntheticScopes 合成作用域的集合接口
 * @see FunInterfaceConstructorsScopeProvider 函数式接口构造器提供者
 */
interface SyntheticScope {

    /**
     * 获取指定名称的合成扩展属性
     *
     * 为给定的接收者类型查找具有指定名称的合成扩展属性。
     *
     * @param receiverTypes 接收者类型集合
     * @param name 属性名称
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 找到的合成扩展属性描述符集合
     */
    fun getSyntheticExtensionProperties(
        receiverTypes: Collection<CangJieType>,
        name: Name,
        location: LookupLocation
    ): Collection<PropertyDescriptor>

    /**
     * 获取指定名称的合成成员函数
     *
     * 为给定的接收者类型查找具有指定名称的合成成员函数。
     *
     * @param receiverTypes 接收者类型集合
     * @param name 函数名称
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 找到的合成成员函数描述符集合
     */
    fun getSyntheticMemberFunctions(
        receiverTypes: Collection<CangJieType>,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor>

    /**
     * 获取合成静态函数
     *
     * 基于已贡献的函数,生成额外的合成静态函数。
     *
     * @param contributedFunctions 已存在的函数描述符集合
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 生成的合成静态函数描述符集合
     */
    fun getSyntheticStaticFunctions(
        contributedFunctions: Collection<FunctionDescriptor>,
        location: LookupLocation
    ): Collection<FunctionDescriptor>

    /**
     * 获取分类器的合成构造器
     *
     * 为给定的分类器(类、接口等)生成合成构造器。
     * 例如,为函数式接口生成 SAM 转换构造器。
     *
     * @param contributedClassifier 分类器描述符
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 生成的合成构造器描述符集合
     */
    fun getSyntheticConstructors(
        contributedClassifier: ClassifierDescriptor,
        location: LookupLocation
    ): Collection<FunctionDescriptor>

    /**
     * 获取所有合成扩展属性
     *
     * 为给定的接收者类型查找所有合成扩展属性(不限制名称)。
     *
     * @param receiverTypes 接收者类型集合
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 找到的所有合成扩展属性描述符集合
     */
    fun getSyntheticExtensionProperties(
        receiverTypes: Collection<CangJieType>,
        location: LookupLocation
    ): Collection<PropertyDescriptor>

    /**
     * 获取所有合成成员函数
     *
     * 为给定的接收者类型查找所有合成成员函数(不限制名称)。
     *
     * @param receiverTypes 接收者类型集合
     * @return 找到的所有合成成员函数描述符集合
     */
    fun getSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>): Collection<FunctionDescriptor>

    /**
     * 获取所有合成静态函数
     *
     * 基于给定的声明描述符,生成所有合成静态函数。
     *
     * @param functionDescriptors 声明描述符集合
     * @return 生成的所有合成静态函数描述符集合
     */
    fun getSyntheticStaticFunctions(functionDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor>

    /**
     * 获取所有合成构造器
     *
     * 为给定的分类器描述符集合生成所有合成构造器。
     *
     * @param classifierDescriptors 分类器描述符集合
     * @return 生成的所有合成构造器描述符集合
     */
    fun getSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor>

    /**
     * 获取单个构造器的合成版本
     *
     * 为给定的构造器生成对应的合成构造器。
     * 如果不需要合成版本,返回 null。
     *
     * @param constructor 原始构造器描述符
     * @return 合成的构造器描述符,如果不需要合成则返回 null
     */
    fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor?

    /**
     * 默认的合成作用域实现
     *
     * 这是一个便利的基类,为 [SyntheticScope] 接口的所有方法提供空实现。
     * 具体的合成作用域可以继承这个类,只重写需要提供合成成员的方法。
     *
     * 所有方法默认返回空集合或 null,表示不提供任何合成成员。
     *
     * ## 使用示例
     *
     * ```kotlin
     * class MyCustomSyntheticScope : SyntheticScope.Default() {
     *     // 只重写需要的方法
     *     override fun getSyntheticMemberFunctions(
     *         receiverTypes: Collection<CangJieType>,
     *         name: Name,
     *         location: LookupLocation
     *     ): Collection<FunctionDescriptor> {
     *         // 提供自定义的合成函数
     *         return listOf(createSyntheticFunction())
     *     }
     * }
     * ```
     */
    open class Default : SyntheticScope {

        override fun getSyntheticExtensionProperties(
            receiverTypes: Collection<CangJieType>,
            name: Name,
            location: LookupLocation
        ): Collection<PropertyDescriptor> {
            return emptyList()
        }

        override fun getSyntheticMemberFunctions(
            receiverTypes: Collection<CangJieType>,
            name: Name,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticStaticFunctions(
            contributedFunctions: Collection<FunctionDescriptor>,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticConstructors(
            contributedClassifier: ClassifierDescriptor,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticExtensionProperties(
            receiverTypes: Collection<CangJieType>,
            location: LookupLocation
        ): Collection<PropertyDescriptor> {
            return emptyList()
        }

        override fun getSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticStaticFunctions(functionDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? {
            return null
        }
    }
}

/**
 * 合成作用域集合接口
 *
 * 这个接口代表一个合成作用域的集合,允许多个 [SyntheticScope] 实现共同工作。
 * 每个作用域提供器可以贡献不同类型的合成成员。
 *
 * ## 设计模式
 *
 * 这是一个组合模式的应用,允许多个合成作用域提供器协同工作:
 * - 每个 [SyntheticScope] 负责一类合成成员(如函数式接口构造器、数据类方法等)
 * - [SyntheticScopes] 聚合所有提供器,提供统一的访问接口
 * - 扩展函数用于收集所有提供器的结果
 *
 * ## 默认实现
 *
 * 使用 [@DefaultImplementation] 注解指定了默认实现为 [FunInterfaceConstructorsScopeProvider],
 * 它提供函数式接口的 SAM 转换构造器。
 *
 * ## 使用方式
 *
 * ```kotlin
 * // 获取合成构造器
 * val constructors = syntheticScopes.collectSyntheticConstructors(
 *     classifier,
 *     location
 * )
 *
 * // 获取合成成员函数
 * val functions = syntheticScopes.collectSyntheticMemberFunctions(
 *     receiverTypes,
 *     name,
 *     location
 * )
 * ```
 *
 * @property scopes 合成作用域提供器的集合
 * @see SyntheticScope 单个合成作用域的接口
 * @see FunInterfaceConstructorsScopeProvider 函数式接口构造器提供者
 */
@DefaultImplementation(impl = FunInterfaceConstructorsScopeProvider::class)
interface SyntheticScopes {
    val scopes: Collection<SyntheticScope>

    /**
     * 空的合成作用域集合
     *
     * 这是一个单例对象,表示不包含任何合成作用域的空集合。
     * 用于在不需要合成成员的上下文中使用。
     */
    object Empty : SyntheticScopes {
        override val scopes: Collection<SyntheticScope> = emptyList()

    }
}

/**
 * 收集指定名称的合成扩展属性
 *
 * 遍历所有合成作用域,收集为给定接收者类型提供的指定名称的合成扩展属性。
 *
 * @param receiverTypes 接收者类型集合
 * @param name 属性名称
 * @param location 查找位置,用于增量编译的依赖跟踪
 * @return 所有合成作用域提供的扩展属性描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticExtensionProperties(
    receiverTypes: Collection<CangJieType>,
    name: Name,
    location: LookupLocation
) = scopes.flatMap { it.getSyntheticExtensionProperties(receiverTypes, name, location) }

/**
 * 收集分类器的合成构造器
 *
 * 遍历所有合成作用域,收集为给定分类器提供的合成构造器。
 * 例如,为函数式接口生成 SAM 转换构造器。
 *
 * @param contributedClassifier 分类器描述符(类、接口等)
 * @param location 查找位置,用于增量编译的依赖跟踪
 * @return 所有合成作用域提供的构造器描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticConstructors(
    contributedClassifier: ClassifierDescriptor,
    location: LookupLocation
) = scopes.flatMap { it.getSyntheticConstructors(contributedClassifier, location) }

/**
 * 收集单个构造器的合成版本
 *
 * 遍历所有合成作用域,为给定的构造器收集对应的合成版本。
 *
 * @param constructor 原始构造器描述符
 * @return 所有合成作用域提供的合成构造器描述符集合(过滤掉 null 值)
 */
fun SyntheticScopes.collectSyntheticConstructors(constructor: ConstructorDescriptor) =
    scopes.mapNotNull { it.getSyntheticConstructor(constructor) }

/**
 * 收集合成静态函数
 *
 * 遍历所有合成作用域,基于已贡献的函数收集合成静态函数。
 *
 * @param contributedFunctions 已存在的函数描述符集合
 * @param location 查找位置,用于增量编译的依赖跟踪
 * @return 所有合成作用域提供的静态函数描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticStaticFunctions(
    contributedFunctions: Collection<FunctionDescriptor>,
    location: LookupLocation
) = scopes.flatMap {
    it.getSyntheticStaticFunctions(contributedFunctions, location)
}

/**
 * 收集指定名称的合成成员函数
 *
 * 遍历所有合成作用域,收集为给定接收者类型提供的指定名称的合成成员函数。
 *
 * @param receiverTypes 接收者类型集合
 * @param name 函数名称
 * @param location 查找位置,用于增量编译的依赖跟踪
 * @return 所有合成作用域提供的成员函数描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticMemberFunctions(
    receiverTypes: Collection<CangJieType>,
    name: Name,
    location: LookupLocation
) = scopes.flatMap { it.getSyntheticMemberFunctions(receiverTypes, name, location) }

/**
 * 收集所有合成静态函数
 *
 * 遍历所有合成作用域,基于给定的声明描述符收集所有合成静态函数。
 *
 * @param functionDescriptors 声明描述符集合
 * @return 所有合成作用域提供的静态函数描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticStaticFunctions(functionDescriptors: Collection<DeclarationDescriptor>) =
    scopes.flatMap { it.getSyntheticStaticFunctions(functionDescriptors) }

/**
 * 收集所有合成构造器
 *
 * 遍历所有合成作用域,为给定的分类器描述符集合收集所有合成构造器。
 *
 * @param classifierDescriptors 分类器描述符集合
 * @return 所有合成作用域提供的构造器描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>) =
    scopes.flatMap { it.getSyntheticConstructors(classifierDescriptors) }

/**
 * 收集所有合成扩展属性
 *
 * 遍历所有合成作用域,收集为给定接收者类型提供的所有合成扩展属性(不限制名称)。
 *
 * @param receiverTypes 接收者类型集合
 * @param location 查找位置,用于增量编译的依赖跟踪
 * @return 所有合成作用域提供的扩展属性描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticExtensionProperties(
    receiverTypes: Collection<CangJieType>,
    location: LookupLocation
) = scopes.flatMap { it.getSyntheticExtensionProperties(receiverTypes, location) }

/**
 * 收集所有合成成员函数
 *
 * 遍历所有合成作用域,收集为给定接收者类型提供的所有合成成员函数(不限制名称)。
 *
 * @param receiverTypes 接收者类型集合
 * @return 所有合成作用域提供的成员函数描述符的扁平化集合
 */
fun SyntheticScopes.collectSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>) =
    scopes.flatMap { it.getSyntheticMemberFunctions(receiverTypes) }

