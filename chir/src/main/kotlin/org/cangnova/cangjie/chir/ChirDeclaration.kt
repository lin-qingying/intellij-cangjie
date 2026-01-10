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

import org.cangnova.cangjie.name.Name

/**
 * 表示 CHIR 中的声明元素。
 *
 * 声明是引入新的命名实体到作用域中的语法结构，包括但不限于：
 * - **类型声明**: 类（class）、接口（interface）、枚举（enum）、类型别名（type alias）
 * - **函数声明**: 顶层函数、成员函数、扩展函数、构造函数
 * - **属性声明**: 顶层属性、成员属性、局部变量、参数
 * - **模块声明**: 包（package）、导入（import）、模块（module）
 *
 * ## 声明与表达式的区别
 * - **声明** ([ChirDeclaration]): 引入命名实体，具有作用域和可见性规则
 * - **表达式** ([ChirExpression]): 可以被求值产生结果
 * - 某些语言结构可能同时是声明和表达式（如 lambda、匿名类）
 *
 * ## 声明的关键特性
 * - **命名** ([name]): 声明的标识符名称
 * - **注解** ([annotations]): 附加的元数据注解
 * - **作用域**: 声明的可见范围（文件级、类级、局部等）
 * - **可见性**: 访问修饰符（public、private、internal 等）
 * - **修饰符**: 其他属性（abstract、final、open 等）
 * - **类型信息**: 声明的类型或签名
 *
 * ## 示例
 * ```kotlin
 * // 类声明
 * @Entity
 * class Person(val name: String, var age: Int) {
 *     // 成员函数声明
 *     fun greet() = "Hello, I'm $name"
 * }
 *
 * // 顶层函数声明
 * @JvmStatic
 * fun main() {
 *     // 局部变量声明
 *     val person = Person("Alice", 30)
 * }
 *
 * // 接口声明
 * interface Drawable {
 *     // 抽象函数声明
 *     fun draw()
 * }
 * ```
 *
 * ## 与语义分析的关系
 * 声明在编译过程中扮演关键角色：
 * - **符号表构建**: 将声明注册到符号表中
 * - **名称解析**: 查找标识符引用对应的声明
 * - **类型检查**: 验证类型约束和签名匹配
 * - **可见性检查**: 验证访问权限
 * - **注解处理**: 处理声明上的注解元数据
 *
 * @see ChirElement 根接口
 * @see ChirExpression 表达式接口
 * @see ChirAnnotationContainer 可被注解的元素容器
 * @see ChirVisitor.visitDeclaration 访问声明的方法
 * @see Name 标识符名称类型
 */
interface ChirDeclaration : ChirElement, ChirAnnotationContainer {
    /**
     * 声明的名称。
     *
     * 标识符名称用于在作用域内唯一标识此声明。
     * 对于大多数声明（类、函数、变量等），名称是必需的；
     * 但某些特殊声明（如匿名类、lambda）可能没有显式名称。
     *
     * ## 名称解析
     * 名称在以下场景中使用：
     * - 符号查找和引用解析
     * - 重载决策（函数签名匹配）
     * - 代码补全和导航
     * - 重构操作（重命名、查找引用）
     *
     * ## 示例
     * ```kotlin
     * class MyClass { ... }     // name = "MyClass"
     * fun calculate() { ... }   // name = "calculate"
     * val counter = 0          // name = "counter"
     * ```
     *
     * @return 声明的标识符名称
     * @see Name 名称的表示类型
     */
    val name: Name

    /**
     * 接受访问者访问此声明。
     *
     * 重写父接口的默认实现，将调用委托给 [ChirVisitor.visitDeclaration]，
     * 以便访问者可以专门处理声明类型的元素。
     *
     * @param R 访问者返回类型
     * @param D 传递给访问者的数据类型
     * @param visitor 访问此声明的访问者
     * @param data 传递给访问者的上下文数据
     * @return 访问者处理此声明后的结果
     *
     * @see ChirVisitor.visitDeclaration
     */
    override fun <R, D> accept(visitor: ChirVisitor<R, D>, data: D): R =
        visitor.visitDeclaration(this, data)
}
