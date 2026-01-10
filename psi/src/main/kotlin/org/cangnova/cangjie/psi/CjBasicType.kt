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

import org.cangnova.cangjie.psi.stubs.CangJieBasicTypeStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

/**
 * 仓颉基本类型 PSI 元素
 *
 * 表示仓颉语言中的基本类型，如 Int、String、Bool 等。
 * 基本类型是语言内置的基础数据类型，不需要导入即可使用。
 *
 * 支持的基本类型包括：
 * - 整数类型：Int8, Int16, Int32, Int64, IntNative, UInt8, UInt16, UInt32, UInt64, UIntNative
 * - 浮点类型：Float16, Float32, Float64
 * - 字符类型：Rune
 * - 布尔类型：Bool
 * - 单位类型：Unit
 * - 其他类型：Nothing, Any 等
 *
 * 此类支持基于 Stub 的索引，提供快速的类型解析能力。
 *
 * 示例：
 * ```
 * let x: Int = 42        // Int 是基本类型
 * let z: Bool = true     // Bool 是基本类型
 * ```
 */
class CjBasicType : CjElementImplStub<CangJieBasicTypeStub>, CjTypeElement {

    /**
     * 从 AST 节点创建基本类型元素
     *
     * @param node AST 节点
     */
    constructor(node: ASTNode) : super(node)

    /**
     * 从 Stub 创建基本类型元素
     *
     * @param stub 基本类型的 Stub
     */
    constructor(stub: CangJieBasicTypeStub) : super(stub, CjStubElementTypes.BASIC_TYPE)

    /**
     * 返回元素类型的字符串表示
     *
     * @return 元素类型名称
     */
    override fun toString(): String {
        return elementType.toString()
    }

    /**
     * 接受访问者模式的访问
     *
     * @param visitor PSI 访问者
     * @param data 访问数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitBasicType(this, data)
    }

    /**
     * 获取基本类型的名称
     *
     * 优先从 Stub 中获取（性能更好），如果 Stub 不可用则从文本中获取。
     *
     * @return 类型名称，如 "Int"、"Bool" 等
     */
    override fun getName(): String {
        return stub?.basicType ?: text
    }

    /**
     * 类型参数列表
     *
     * 基本类型不支持类型参数，始终返回空列表。
     * 注意：虽然某些泛型容器类型（如 Array<T>）看起来像基本类型，
     * 但它们实际上是类类型，不是这里的基本类型。
     *
     * @return 空列表
     */
    val typeArguments: List<CjTypeProjection>
        get() = emptyList()

    /**
     * 类型参数作为类型引用的列表
     *
     * 基本类型不支持类型参数，始终返回空列表。
     *
     * @return 空列表
     */
    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() = emptyList()
}
