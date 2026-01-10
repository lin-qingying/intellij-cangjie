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

package org.cangnova.cangjie.container


/**
 * 平台特定扩展标记接口
 *
 * 用于标识那些在通用解析设施(如类型解析或反序列化)中需要,但具有平台特定实现的组件。
 *
 * 平台特定扩展在容器中必须恰好存在一个实例(因此通常在对应的接口中提供
 * 无操作的 DEFAULT/EMPTY 实现作为常见模式)。
 *
 * 在多平台模块中,此类组件需要特殊处理。具体来说,如果提供了多个相同类型的组件,
 * 这并不是非法状态;相反,我们必须根据具体情况仔细解决冲突。
 * 另请参阅 [PlatformExtensionsClashResolver]。
 *
 * @param S 扩展自身的类型参数
 */
interface PlatformSpecificExtension<S : PlatformSpecificExtension<S>>

/**
 * 平台扩展冲突解决器
 *
 * 允许指定当容器中存在两个或多个 [applicableTo] 类的注册时,
 * 应该使用哪个 [PlatformSpecificExtension]。
 *
 * [PlatformExtensionsClashResolver] 应该通过 [useClashResolver] 扩展注册到容器中。
 *
 * **注意**: 对于最常见的"一个或多个默认实现 vs. 零个或一个非默认实现"的情况,
 * 您不需要此机制。只需使用 [DefaultImplementation],默认实例将自动被区分(参见相关文档)。
 * 仅在需要更复杂逻辑的情况下使用 [PlatformExtensionsClashResolver]。
 *
 * **示例**: [org.cangnova.cangjie.resolve.IdentifierChecker]。它用于平台无关的代码中,
 * 用于解析和检查标识符的正确性。每个平台都有自己的标识符正确性规则。
 * 在多平台模块中,我们不能只选择一个 IdentifierChecker;相反,我们必须提供一个
 * "组合"的 IdentifierChecker,它将启动每个平台的检查。
 *
 * @param E 平台特定扩展的类型
 * @property applicableTo 此解决器适用的类
 */
abstract class PlatformExtensionsClashResolver<E : PlatformSpecificExtension<E>>(val applicableTo: Class<E>) {
    /**
     * 解决扩展冲突
     *
     * 当存在多个相同类型的扩展时,选择最终使用的扩展。
     *
     * @param extensions 发生冲突的扩展列表
     * @return 选中的扩展实例
     */
    abstract fun resolveExtensionsClash(extensions: List<E>): E

    /**
     * 回退到默认值策略
     *
     * 无论存在多少个扩展,始终返回预定义的默认值。
     *
     * @param E 平台特定扩展的类型
     * @property defaultValue 默认扩展实例
     * @param applicableTo 此解决器适用的类
     */
    class FallbackToDefault<E : PlatformSpecificExtension<E>>(
        private val defaultValue: E,
        applicableTo: Class<E>
    ) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E = defaultValue
    }

    /**
     * 首个优先策略
     *
     * 选择扩展列表中的第一个扩展。
     *
     * @param E 平台特定扩展的类型
     * @param applicableTo 此解决器适用的类
     */
    class FirstWins<E : PlatformSpecificExtension<E>>(applicableTo: Class<E>) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E = extensions.first()
    }
}

