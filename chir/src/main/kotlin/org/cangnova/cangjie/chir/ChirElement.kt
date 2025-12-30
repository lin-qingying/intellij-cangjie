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

package org.cangnova.cangjie.chir

/**
 * CHIR (CangJie Hierarchical Intermediate Representation) 元素的根接口。
 *
 * CHIR 是仓颉语言的分层中间表示层，用于在编译过程中表示抽象语法结构。
 * 所有 CHIR 元素（表达式、声明、注解等）都实现此接口。
 *
 * ## 设计说明
 * - 使用访问者模式 ([ChirVisitor]) 进行遍历和处理
 * - 每个元素都可以关联到源代码位置 ([source])
 * - 支持类型安全的树结构遍历
 *
 * ## 层次结构
 * ```
 * ChirElement (根接口)
 *   ├── ChirExpression (表达式)
 *   ├── ChirDeclaration (声明)
 *   └── ChirAnnotation (注解)
 * ```
 *
 * @see ChirVisitor 访问者模式实现
 * @see ChirExpression 表达式接口
 * @see ChirDeclaration 声明接口
 * @see ChirAnnotation 注解接口
 */
interface ChirElement {

    /**
     * 关联的源代码元素信息。
     *
     * 提供此 CHIR 元素在源代码中的位置信息，用于：
     * - 错误报告和诊断信息定位
     * - IDE 功能（如导航、高亮）
     * - 调试器断点设置
     *
     * @return 源元素信息，如果元素是合成的（编译器生成的）则可能为 null
     */
    val source: CjSourceElement?

    /**
     * 接受访问者访问此元素。
     *
     * 这是访问者模式的标准实现，允许在不修改元素类的情况下添加新的操作。
     * 默认实现将调用委托给 [ChirVisitor.visitElement]。
     *
     * @param R 访问者返回类型
     * @param D 传递给访问者的数据类型
     * @param visitor 访问此元素的访问者
     * @param data 传递给访问者的上下文数据
     * @return 访问者处理此元素后的结果
     *
     * @see ChirVisitor
     */
    fun <R, D> accept(visitor: ChirVisitor<R, D>, data: D): R =
        visitor.visitElement(this, data)
}

