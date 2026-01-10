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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import com.intellij.util.ArrayFactory

/**
 * 仓颉语言中声明 (Declaration) 的基础接口
 *
 * 声明是仓颉语言中定义程序结构的基本单元,包括类、函数、变量、常量等。
 * 该接口继承自 [CjExpression] 和 [CjModifierListOwner],表示声明既可以作为表达式使用,
 * 又可以拥有修饰符(如 public、private 等)。
 *
 * 实现该接口的典型 PSI 元素包括:
 * - 类声明 (CjClass)
 * - 函数声明 (CjFunction)
 * - 属性声明 (CjProperty)
 * - 参数声明 (CjParameter)
 *
 * @see CjNamedDeclaration 具有名称的声明
 * @see CjModifierListOwner 可拥有修饰符的元素
 * @see CjExpression 表达式基础接口
 */
interface CjDeclaration : CjExpression, CjModifierListOwner {
    /**
     * 获取该声明的文档注释
     *
     * @return 文档注释对象,如果不存在则返回 null
     */
    val docComment: CDoc?

    /**
     * 获取该声明关联的表达式
     *
     * 例如,变量声明的初始化表达式、函数的返回表达式等。
     *
     * @return 关联的表达式,如果不存在则返回 null
     */
    val expression: CjExpression?

    companion object {
        /**
         * 空声明数组常量
         */
        val EMPTY_ARRAY: Array<CjDeclaration?> = arrayOfNulls(0)

        /**
         * 声明数组工厂,用于创建指定大小的声明数组
         */
        val ARRAY_FACTORY: ArrayFactory<CjDeclaration> =
            ArrayFactory { count: Int -> if (count == 0) EMPTY_ARRAY else arrayOfNulls(count) }
    }
}
