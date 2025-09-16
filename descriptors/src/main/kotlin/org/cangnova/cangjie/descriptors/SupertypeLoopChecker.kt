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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor

/**
 * 超级类型循环检查器接口，用于检测和处理类型继承关系中的循环依赖。
 */
interface SupertypeLoopChecker {
    /**
     * 在类型的超级类型中查找循环依赖并断开循环，返回处理后的超级类型集合。
     *
     * @param currentTypeConstructor 当前类型的构造器
     * @param superTypes 当前的超级类型集合
     * @param neighbors 获取类型的邻居（即直接超级类型）的函数
     * @param reportLoop 报告循环的回调函数
     * @return 处理后的超级类型集合
     */
    fun findLoopsInSupertypesAndDisconnect(
        currentTypeConstructor: TypeConstructor,
        superTypes: Collection<CangJieType>,
        neighbors: (TypeConstructor) -> Iterable<CangJieType>,
        reportLoop: (CangJieType) -> Unit
    ): Collection<CangJieType>

    /**
     * 空实现的超级类型循环检查器，不对超级类型做任何处理。
     */
    object EMPTY : SupertypeLoopChecker {
        /**
         * 直接返回传入的超级类型集合，不进行任何处理。
         */
        override fun findLoopsInSupertypesAndDisconnect(
            currentTypeConstructor: TypeConstructor,
            superTypes: Collection<CangJieType>,
            neighbors: (TypeConstructor) -> Iterable<CangJieType>,
            reportLoop: (CangJieType) -> Unit
        ): Collection<CangJieType> = superTypes
    }
}
