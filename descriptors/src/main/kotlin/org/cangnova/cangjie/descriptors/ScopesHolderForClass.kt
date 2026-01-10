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

import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 类作用域持有者 (Class Scopes Holder)
 *
 * 这是一个用于管理类描述符成员作用域的缓存管理类。该类的主要作用包括：
 *
 * 1. **作用域缓存管理**: 为每个类描述符缓存其成员作用域，避免重复创建
 * 2. **类型细化支持**: 支持类型细化器(TypeRefiner)对作用域的动态调整
 * 3. **递归类型保护**: 防止递归类型定义时出现无限循环问题
 * 4. **性能优化**: 通过延迟加载和智能缓存提升编译器性能
 *
 * 使用场景：
 * - 编译器需要解析类的成员时
 * - 类型检查过程中需要访问类的作用域时
 * - 处理泛型类和继承关系时
 *
 * @param T 继承自MemberScope的作用域类型，表示具体的成员作用域实现
 */
class ScopesHolderForClass<T : MemberScope> private constructor(
    /**
     * 类描述符 - 描述当前类的所有信息，包括名称、父类、成员等
     */
    private val classDescriptor: ClassifierDescriptor,

    /**
     * 存储管理器 - 负责管理延迟计算值的生命周期和缓存
     */
    storageManager: StorageManager,

    /**
     * 作用域工厂函数 - 用于根据类型细化器创建具体的作用域实例
     * 参数: CangJieTypeRefiner - 类型细化器
     * 返回: T - 具体的作用域实例
     */
    private val scopeFactory: (CangJieTypeRefiner) -> T,

    /**
     * 所有者模块的类型细化器 - 用于处理当前类所属模块的类型细化
     */
    private val cangjieTypeRefinerForOwnerModule: CangJieTypeRefiner
) {

    /**
     * 所有者模块的作用域 - 延迟创建
     *
     * 使用延迟加载模式，只有在实际需要时才创建作用域实例。
     * 这样可以避免在类加载时就创建所有作用域，提高性能。
     */
    private val scopeForOwnerModule by storageManager.createLazyValue {
        scopeFactory(cangjieTypeRefinerForOwnerModule)
    }

    /**
     * 获取指定类型细化器对应的作用域
     *
     * 这是该类的核心方法，实现了智能的作用域获取策略：
     *
     * 1. **模块级别检查**: 首先检查当前模块是否需要类型细化
     *    - 如果不需要，直接返回所有者模块的缓存作用域
     *
     * 2. **类型构造器级别检查**: 检查当前类的类型构造器是否需要细化
     *    - 如果不需要，同样返回所有者模块的缓存作用域
     *
     * 3. **创建细化作用域**: 如果需要细化，则创建或获取专门的细化作用域
     *
     * **递归类型保护机制**:
     * 这种分层检查设计可以有效防止递归类型定义导致的无限循环问题，例如：
     * ```kotlin
     * interface A<T : A<T>>  // 自引用泛型
     * interface B : B<T>     // 直接自继承
     * ```
     *
     * 在处理这类递归类型时，如果不进行提前检查，会导致：
     * 1. 计算类B的默认类型
     * 2. 调用isRefinementNeededForTypeConstructor检查
     * 3. 查询B的父类型，发现A<B>
     * 4. 再次请求类B的默认类型
     * 5. 形成无限递归
     *
     * 通过提前的模块和类型构造器检查，可以在递归发生前就返回合适的作用域。
     *
     * @param cangjieTypeRefiner 用于类型细化的类型细化器实例
     * @return T 对应的作用域实例，可能是缓存的所有者模块作用域，也可能是新创建的细化作用域
     */
    
    fun getScope(cangjieTypeRefiner: CangJieTypeRefiner): T {
        // 第一层保护：模块级别检查
        // 如果当前模块不需要进行类型细化，直接返回所有者模块的作用域
        // 这可以避免不必要的细化操作，提高性能
        if (!cangjieTypeRefiner.isRefinementNeededForModule(classDescriptor.module)) {
            return scopeForOwnerModule
        }

        // 第二层保护：类型构造器级别检查
        // 如果当前类的类型构造器不需要细化，返回所有者模块的作用域
        // 这是防止递归类型问题的关键检查点
        if (!cangjieTypeRefiner.isRefinementNeededForTypeConstructor(classDescriptor.typeConstructor)) {
            return scopeForOwnerModule
        }

        // 需要进行类型细化：获取或创建针对该类的细化作用域
        // 使用类型细化器的缓存机制，避免重复创建相同的细化作用域
        return cangjieTypeRefiner.getOrPutScopeForClass(classDescriptor) {
            scopeFactory(cangjieTypeRefiner)
        }
    }

    companion object {
        /**
         * 创建ScopesHolderForClass实例的工厂方法
         *
         * 使用工厂模式创建实例，提供了更好的封装性和灵活性。
         *
         * @param T 作用域类型参数，必须继承自MemberScope
         * @param classDescriptor 要管理作用域的类描述符
         * @param storageManager 存储管理器，用于管理延迟值的生命周期
         * @param cangjieTypeRefinerForOwnerModule 所有者模块的类型细化器
         * @param scopeFactory 作用域创建工厂函数
         * @return ScopesHolderForClass<T> 新创建的作用域持有者实例
         *
         * 使用示例：
         * ```kotlin
         * val scopeHolder = ScopesHolderForClass.create(
         *     classDescriptor = myClassDescriptor,
         *     storageManager = storageManager,
         *     cangjieTypeRefinerForOwnerModule = typeRefiner,
         *     scopeFactory = { refiner -> MyMemberScope(refiner) }
         * )
         * ```
         */
        @JvmStatic
        fun <T : MemberScope> create(
            classDescriptor: ClassifierDescriptor,
            storageManager: StorageManager,
            cangjieTypeRefinerForOwnerModule: CangJieTypeRefiner,
            scopeFactory: (CangJieTypeRefiner) -> T
        ): ScopesHolderForClass<T> {
            return ScopesHolderForClass(
                classDescriptor,
                storageManager,
                scopeFactory,
                cangjieTypeRefinerForOwnerModule
            )
        }
    }
}