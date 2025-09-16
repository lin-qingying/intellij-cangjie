/*
 * Copyright 2024 LinQingYing. and contributors.
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

import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

/**
 * 表示描述符可见性的抽象基类
 */
abstract class DescriptorVisibility protected constructor(){

    /**
     * 获取可见性的委托对象
     * @return 返回Visibility对象
     */
    abstract val delegate: Visibility

    /**
     * 获取可见性的名称
     * @return 返回可见性名称
     */
    val name: String
        get() = delegate.name

    /**
     * 判断是否为公共API
     * @return 如果是公共API则返回true，否则返回false
     */
    val isPublicAPI: Boolean
        get() = delegate.isPublicAPI

    /**
     * 获取外部显示名称（用于诊断）
     * @return 返回外部显示名称
     */
    abstract val externalDisplayName: String

    /**
     * 获取内部显示名称
     * @return 返回内部显示名称
     */
    abstract val internalDisplayName: String
    /**
     * 如果需要在导入时检查此可见性且不导入具有此可见性的不可访问声明，则返回true
     * 提示：如果可以基于文件级别检查此可见性，则返回true
     * 示例：
     * 对于PROTECTED返回false，因为类的受保护成员可以导入到其容器的子类中使用
     * 因此当我们查看导入时，无法判断其在此文件中的合法性
     * 对于INTERNAL返回true，因为内部声明要么在文件中全部可见，要么全部不可见
     * 对于PRIVATE返回true，因导入私有成员无意义：除非它们无需导入即可访问
     */
    abstract fun mustCheckInImports(): Boolean
    abstract fun normalize(): DescriptorVisibility
    /**
     * 判断是否可见
     * @param receiver 接收器值，用于特殊接收器值的被调用方可访问性判断
     *                 值为null表示当前调用中没有接收器
     *                 如需忽略接收器进行基本检查（如在调用范围外检查），应使用Visibilities.ALWAYS_SUITABLE_RECEIVER
     *                 如需判断可见性是否接受任意接收器，应使用Visibilities.IRRELEVANT_RECEIVER
     *                 注意：当前Visibilities.IRRELEVANT_RECEIVER与null具有相同效果
     *                 实现时必须注意这些特殊值
     */
    abstract fun isVisible(
        receiver: ReceiverValue?,
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        useSpecialRulesForPrivateSealedConstructors: Boolean
    ): Boolean
    /**
     * @return 比较结果，null表示未知
     */
    fun compareTo(visibility: DescriptorVisibility): Int? {
        return delegate.compareTo(visibility.delegate)
    }
}

/**
 * 表示通过委托实现的描述符可见性类
 * @param delegate 可见性委托对象
 */
abstract class DelegatedDescriptorVisibility(override val delegate: Visibility) : DescriptorVisibility() {
    override fun mustCheckInImports(): Boolean {
        return delegate.mustCheckInImports()
    }
     // internal representation for descriptors
    override val internalDisplayName: String
        get() = delegate.internalDisplayName

//    // external representation for diagnostics
    override val externalDisplayName: String
        get() = delegate.externalDisplayName
//
    override fun normalize(): DescriptorVisibility = DescriptorVisibilities.toDescriptorVisibility(delegate.normalize())

    /**
     * 返回可见性的字符串表示
     * @return 返回可见性的字符串
     */
    final override fun toString(): String = delegate.toString()

}
