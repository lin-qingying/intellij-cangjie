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
 * CHIR 元素树的访问者基类。
 *
 * 实现了经典的访问者（Visitor）设计模式，允许在不修改 CHIR 元素类的情况下，
 * 定义新的操作。访问者可以遍历整个 CHIR 树，对不同类型的元素执行不同的处理逻辑。
 *
 * ## 设计模式说明
 * 访问者模式将数据结构（CHIR 元素）与操作（访问者）分离，具有以下优点：
 * - **开放-封闭原则**: 添加新操作无需修改元素类
 * - **单一职责**: 相关操作集中在一个访问者类中
 * - **类型安全**: 通过泛型参数保证类型安全
 *
 * ## 类型参数
 * @param R 访问操作的返回类型。可以是任何类型，例如：
 *   - `Unit`: 执行副作用操作（如代码生成、验证）
 *   - `Boolean`: 谓词判断（如查找、过滤）
 *   - `List<T>`: 收集结果（如提取所有函数调用）
 * @param D 访问过程中传递的上下文数据类型。常见用途：
 *   - 累积状态（如符号表、作用域链）
 *   - 配置选项（如格式化参数）
 *   - 访问控制（如访问深度、停止标志）
 *
 * ## 使用示例
 * ```kotlin
 * // 示例 1: 统计表达式数量
 * class ExpressionCounter : ChirVisitor<Int, Unit>() {
 *     override fun visitElement(element: ChirElement, data: Unit) = 0
 *     override fun visitExpression(expression: ChirExpression, data: Unit) = 1
 * }
 *
 * // 示例 2: 收集所有声明名称
 * class DeclarationCollector : ChirVisitor<List<String>, MutableList<String>>() {
 *     override fun visitElement(element: ChirElement, data: MutableList<String>) = data
 *     override fun visitDeclaration(declaration: ChirDeclaration, data: MutableList<String>): List<String> {
 *         data.add(declaration.name)
 *         return data
 *     }
 * }
 *
 * // 示例 3: 查找特定模式
 * class PatternFinder : ChirVisitor<Boolean, String>() {
 *     override fun visitElement(element: ChirElement, data: String) = false
 *     override fun visitExpression(expression: ChirExpression, pattern: String): Boolean {
 *         return expression.toString().contains(pattern)
 *     }
 * }
 * ```
 *
 * ## 实现访问者的步骤
 * 1. 继承 [ChirVisitor] 并指定泛型参数
 * 2. 实现 [visitElement] 作为默认处理逻辑
 * 3. 根据需要重写特定类型的 visit 方法（[visitExpression]、[visitDeclaration] 等）
 * 4. 如需递归遍历，在 visit 方法中调用子元素的 accept 方法
 *
 * @see ChirElement.accept CHIR 元素接受访问者的方法
 * @see ChirExpression 表达式接口
 * @see ChirDeclaration 声明接口
 * @see ChirAnnotation 注解接口
 */
abstract class ChirVisitor<out R, in D> {
    /**
     * 访问任意 CHIR 元素的通用方法。
     *
     * 这是访问者的**抽象基础方法**，所有元素类型都会最终调用此方法
     * （除非在子类型的 visit 方法中被覆盖）。
     *
     * ## 实现建议
     * - 提供默认的处理逻辑或回退行为
     * - 对于递归遍历，在此方法中访问子元素
     * - 对于不关心的元素类型，返回中性值（如 Unit、空列表等）
     *
     * @param element 要访问的 CHIR 元素
     * @param data 传递给访问者的上下文数据
     * @return 访问结果，具体含义由子类定义
     */
    abstract fun visitElement(element: ChirElement, data: D): R

    /**
     * 访问表达式元素。
     *
     * 当访问 [ChirExpression] 类型的元素时会调用此方法。
     * 默认实现委托给 [visitElement]，子类可以重写以提供特定的表达式处理逻辑。
     *
     * ## 适用场景
     * - 表达式求值或类型推导
     * - 代码优化（如常量折叠、死代码消除）
     * - 代码生成（如 IR 转换）
     * - 模式匹配和查找
     *
     * @param expression 要访问的表达式
     * @param data 传递给访问者的上下文数据
     * @return 访问结果
     */
    open fun visitExpression(expression: ChirExpression, data: D): R =
        visitElement(expression, data)

    /**
     * 访问声明元素。
     *
     * 当访问 [ChirDeclaration] 类型的元素时会调用此方法。
     * 默认实现委托给 [visitElement]，子类可以重写以提供特定的声明处理逻辑。
     *
     * ## 适用场景
     * - 符号表构建和作用域分析
     * - 签名提取和 API 文档生成
     * - 依赖分析和模块化检查
     * - 重构工具（如重命名、提取方法）
     *
     * @param declaration 要访问的声明
     * @param data 传递给访问者的上下文数据
     * @return 访问结果
     */
    open fun visitDeclaration(declaration: ChirDeclaration, data: D): R =
        visitElement(declaration, data)

    /**
     * 访问注解元素。
     *
     * 当访问 [ChirAnnotation] 类型的元素时会调用此方法。
     * 默认实现委托给 [visitElement]，子类可以重写以提供特定的注解处理逻辑。
     *
     * ## 适用场景
     * - 注解处理器和代码生成（如序列化、依赖注入）
     * - 编译时验证（如检查注解参数的有效性）
     * - 文档生成（如提取 @Deprecated 注解）
     * - 运行时反射准备
     *
     * @param annotation 要访问的注解
     * @param data 传递给访问者的上下文数据
     * @return 访问结果
     */
    open fun visitAnnotation(annotation: ChirAnnotation, data: D): R =
        visitElement(annotation, data)
}