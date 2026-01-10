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

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 构造函数被调用表达式 (Constructor Callee Expression)
 *
 * 表示调用构造函数或注解时的被调用者部分,包含类型引用和构造函数名称。
 * 这是构造函数调用或注解使用的核心组成部分,用于标识被实例化的类型。
 *
 * ## 语法结构
 * ```
 * constructorCalleeExpression ::= typeReference
 * ```
 *
 * ## 使用场景
 *
 * ### 1. 构造函数调用
 * ```cangjie
 * val list = ArrayList<Int>()  // ArrayList<Int> 是 constructorCalleeExpression
 * val person = Person("Alice", 30)  // Person 是 constructorCalleeExpression
 * ```
 *
 * ### 2. 注解使用
 * ```cangjie
 * @Deprecated  // Deprecated 是 constructorCalleeExpression
 * @ForeignName(name: "native_func")  // ForeignName 是 constructorCalleeExpression
 * @CallingConv(convention: CDECL)  // CallingConv 是 constructorCalleeExpression
 * ```
 *
 * ### 3. 带泛型参数的构造
 * ```cangjie
 * val map = HashMap<String, Int>()  // HashMap<String, Int> 是 constructorCalleeExpression
 * val set = HashSet<String>()  // HashSet<String> 是 constructorCalleeExpression
 * ```
 *
 * ## 在调用链中的位置
 *
 * 作为 [CjCallElement] 的 `calleeExpression` 属性:
 * ```
 * CjAnnotation (注解)
 *   └─ calleeExpression: CjConstructorCalleeExpression
 *        └─ typeReference: CjTypeReference
 *             └─ typeElement: CjUserType
 *                  └─ referenceExpression: CjSimpleNameExpression
 * ```
 *
 * 或在构造函数调用表达式中:
 * ```
 * CjCallExpression (构造函数调用)
 *   └─ calleeExpression: CjConstructorCalleeExpression
 *        └─ typeReference: CjTypeReference
 *   └─ valueArgumentList: CjValueArgumentList
 * ```
 *
 * ## 主要属性
 * - [typeReference]: 类型引用,包含被调用的类型信息
 * - [constructorReferenceExpression]: 构造函数引用表达式,从类型引用中提取的名称表达式
 *
 * ## 与其他元素的关系
 * - 被 [CjCallElement] 作为 `calleeExpression` 使用
 * - 在 [CjAnnotation] 中标识注解类型
 * - 在构造函数调用中标识被实例化的类型
 * - 包含 [CjTypeReference] 以指定具体类型
 *
 * ## 示例解析
 *
 * ### 简单构造
 * ```cangjie
 * Person("Alice")
 * ```
 * - typeReference: `Person`
 * - constructorReferenceExpression: `Person`
 *
 * ### 泛型构造
 * ```cangjie
 * ArrayList<Int>()
 * ```
 * - typeReference: `ArrayList<Int>`
 * - constructorReferenceExpression: `ArrayList`
 *
 * ### 注解使用
 * ```cangjie
 * @ForeignName(name: "func")
 * ```
 * - typeReference: `ForeignName`
 * - constructorReferenceExpression: `ForeignName`
 *
 * @see CjCallElement 调用表达式接口
 * @see CjAnnotation 注解,使用此类作为被调用者
 * @see CjTypeReference 类型引用
 * @see CjSimpleNameExpression 简单名称表达式
 */
class CjConstructorCalleeExpression : CjExpressionImplStub<CangJiePlaceHolderStub<CjConstructorCalleeExpression>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjConstructorCalleeExpression>) : super(
        stub,
        CjStubElementTypes.CONSTRUCTOR_CALLEE,
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitConstructorCalleeExpression(this, data)
    }

    @get:IfNotParsed
    val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    @get:IfNotParsed
    val constructorReferenceExpression: CjSimpleNameExpression?
        get() {
            val typeReference = typeReference ?: return null
            val typeElement = typeReference.typeElement as? CjUserType ?: return null
            return typeElement.referenceExpression
        }
}
