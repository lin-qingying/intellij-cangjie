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

package org.cangnova.cangjie.chir

/**
 * 表示 CHIR 中的表达式元素。
 *
 * 表达式是可以被求值并产生结果的语法结构，包括但不限于：
 * - 字面量表达式（数字、字符串、布尔值等）
 * - 变量引用和成员访问
 * - 函数调用和运算符表达式
 * - Lambda 表达式和闭包
 * - 控制流表达式（if、when、try-catch 等）
 *
 * ## 设计原则
 * - 所有表达式都有类型（通过类型推导或显式声明）
 * - 表达式可以嵌套形成复合表达式树
 * - 表达式求值可能产生副作用（如函数调用、赋值等）
 *
 * ## 示例
 * ```kotlin
 * // 简单表达式
 * 42                           // 字面量表达式
 * x + y                        // 二元运算表达式
 * foo(a, b)                    // 函数调用表达式
 *
 * // 复合表达式
 * if (x > 0) x else -x         // 条件表达式
 * { a, b -> a + b }            // Lambda 表达式
 * ```
 *
 * @see ChirElement 根接口
 * @see ChirDeclaration 声明接口
 * @see ChirVisitor.visitExpression 访问表达式的方法
 */
interface ChirExpression : ChirElement {

    /**
     * 接受访问者访问此表达式。
     *
     * 重写父接口的默认实现，将调用委托给 [ChirVisitor.visitExpression]，
     * 以便访问者可以专门处理表达式类型的元素。
     *
     * @param R 访问者返回类型
     * @param D 传递给访问者的数据类型
     * @param visitor 访问此表达式的访问者
     * @param data 传递给访问者的上下文数据
     * @return 访问者处理此表达式后的结果
     *
     * @see ChirVisitor.visitExpression
     */
    override fun <R, D> accept(visitor: ChirVisitor<R, D>, data: D): R =
        visitor.visitExpression(this, data)
}