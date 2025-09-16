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
package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeProjection

/**
 * 类型检查过程的回调接口。
 *
 * 实现此接口以定制类型检查期间遇到不同情况时的处理策略：例如两个类型是否应视为相等、
 * 类型构造器是否相等、子类型关系如何判定以及是否接受捕获的类型投影。
 *
 * 所有方法返回 Boolean：返回 true 表示继续类型检查流程，返回 false 表示检查失败并中止。
 */
interface TypeCheckingProcedureCallbacks {
    /**
     * 断言两个类型相等。
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param typeCheckingProcedure 当前类型检查过程对象，可用于递归检查或提供上下文信息
     * @return true 表示认为相等并继续；false 表示不相等并中止检查
     */
    fun assertEqualTypes(a: CangJieType, b: CangJieType, typeCheckingProcedure: TypeCheckingProcedure): Boolean

    /**
     * 断言两个类型构造器相等（通常用于忽略类型参数时的比较）。
     *
     * @param a 第一个类型构造器
     * @param b 第二个类型构造器
     * @return true 表示构造器相等并继续；false 表示不相等并中止检查
     */
    fun assertEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean

    /**
     * 断言 subtype 是否为 supertype 的子类型。
     *
     * @param subtype 子类型
     * @param supertype 父类型
     * @param typeCheckingProcedure 当前类型检查过程对象，用于在需要时触发进一步检查
     * @return true 表示接受该子类型关系并继续；false 表示不接受并中止检查
     */
    fun assertSubtype(
        subtype: CangJieType,
        supertype: CangJieType,
        typeCheckingProcedure: TypeCheckingProcedure
    ): Boolean

    /**
     * 当遇到需要捕获（capture）的类型投影时调用此回调（例如通配符或星投影的处理）。
     *
     * @param type 要捕获的类型
     * @param typeProjection 与之对应的类型投影信息
     * @return true 表示接受捕获并继续；false 表示拒绝并中止检查
     */
    fun capture(type: CangJieType, typeProjection: TypeProjection): Boolean

    /**
     * 当找不到与子类型对应的父类型时调用（例如在超类型搜索失败时��。
     *
     * @param subtype 原始子类型
     * @param supertype 期望的父类型
     * @return true 表示忽略该缺失并继续其他检查；false 表示将此视为失败并中止检查
     */
    fun noCorrespondingSupertype(subtype: CangJieType, supertype: CangJieType): Boolean
}
