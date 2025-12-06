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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeConstructor
import org.cangnova.cangjie.descriptors.isFinalClass
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.storage.StorageManager

/**
 * 抽象类类型构造器
 *
 * 为类类型提供类型构造器的抽象实现。类型构造器负责：
 * - 管理类型参数
 * - 定义父类型
 * - 提供类型相等性判断
 * - 提供类型的最终性（finality）判断
 *
 * 这是所有类类型构造器的基类，提供了通用的实现逻辑。
 *
 * @param storageManager 存储管理器，用于延迟计算和缓存
 *
 * @see AbstractTypeConstructor 父类，提供基础的类型构造器实现
 * @see TypeConstructor 类型构造器接口
 */
abstract class AbstractClassTypeConstructor(
    storageManager: StorageManager
) : AbstractTypeConstructor(storageManager), TypeConstructor {

    /**
     * 声明描述符
     *
     * 返回与此类型构造器关联的类描述符。
     * 这个描述符包含了类的所有信息，如名称、成员、修饰符等。
     *
     * @return 带类型构造器的类描述符
     */
    abstract override val declarationDescriptor: ClassifierDescriptorWithTypeConstructor

    /**
     * 检查是否为相同的类
     *
     * 通过比较完全限定名（Fully Qualified Name）来判断两个类是否相同。
     * 这种比较方式可以跨越不同的描述符实例，只要它们代表同一个类。
     *
     * 实现逻辑：
     * 1. 检查 classifier 是否为 ClassifierDescriptorWithTypeConstructor 类型
     * 2. 比较两个类的完全限定名是否相等
     *
     * @param classifier 要比较的类描述符
     * @return 如果代表同一个类则返回 true，否则返回 false
     *
     * @see areFqNamesEqual 完全限定名比较函数
     */
    override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
        return classifier is ClassifierDescriptorWithTypeConstructor &&
                areFqNamesEqual(declarationDescriptor, classifier)
    }

    /**
     * 判断类型是否为最终类型（final）
     *
     * 最终类型不能被继承。这个属性通过检查类描述符的修饰符来确定。
     *
     * 使用场景：
     * - 类型推断：最终类型可以提供更精确的类型信息
     * - 优化：编译器可以对最终类型进行特殊优化
     * - 类型检查：某些操作只能在最终类型上执行
     *
     * @return 如果类是 final 的则返回 true，否则返回 false
     *
     * @see isFinalClass 类描述符的扩展属性，用于判断是否为最终类
     */
    override val isFinal: Boolean
        get() {
            val descriptor = declarationDescriptor
            return descriptor.isFinalClass
        }

    /**
     * 内置类型系统
     *
     * 提供对仓颉语言内置类型的访问，如：
     * - 基本类型：Int, Float, String, Bool 等
     * - 容器类型：Array, List, Map 等
     * - 特殊类型：Any, Nothing, Unit 等
     *
     * 这个属性从类描述符中获取，确保所有类型使用同一个内置类型系统实例。
     *
     * @return 仓颉内置类型系统
     *
     * @see CangJieBuiltIns 仓颉内置类型系统
     */
    override val builtIns: CangJieBuiltIns
        get() = declarationDescriptor.builtIns
}