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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.CangJieTypeParameterStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil

/**
 * 仓颉语言类型参数（泛型参数）PSI 元素
 *
 * 表示泛型声明中的类型参数，例如：
 * - 类泛型：`class Container<T>`
 * - 函数泛型：`func map<T, R>(list: List<T>, transform: (T) -> R)`
 * - 接口泛型：`interface Comparable<T>`
 *
 * 支持类型参数的上界约束：
 * - 单个上界：`class Foo<T: Comparable<T>>`
 * - 多个上界：`class Bar<T> where T: Comparable<T>, T: Serializable`
 *
 * 该类同时支持基于 Stub 的索引和基于 PSI 树的解析，以优化性能。
 */
class CjTypeParameter : CjNamedDeclarationStub<CangJieTypeParameterStub> {
    /**
     * 从 AST 节点构造类型参数
     *
     * @param node AST 节点
     */
    constructor(node: ASTNode) : super(node)

    /**
     * 从 Stub 索引构造类型参数
     *
     * @param stub 类型参数的 Stub 索引数据
     */
    constructor(stub: CangJieTypeParameterStub) : super(stub, CjStubElementTypes.TYPE_PARAMETER)

    /**
     * 接受访问者访问
     *
     * @param visitor PSI 访问者
     * @param data 附加数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitTypeParameter(this, data)
    }

    /**
     * 返回类型参数的字符串表示
     *
     * @return 节点元素类型的字符串形式
     */
    override fun toString(): String {
        return node.elementType.toString()
    }

    /**
     * 设置类型参数的上界约束
     *
     * 用于修改类型参数的约束条件，例如从 `T` 修改为 `T: Comparable<T>`
     *
     * 处理以下情况：
     * 1. 如果当前有上界且新上界为 null：删除冒号和上界约束
     * 2. 如果当前有上界且新上界非 null：替换现有上界
     * 3. 如果当前无上界且新上界非 null：添加冒号和上界约束
     * 4. 如果当前无上界且新上界为 null：无操作
     *
     * @param typeReference 新的上界类型引用，null 表示移除上界约束
     * @return 设置后的类型引用，如果移除了约束则返回 null
     */
    fun setExtendsBound(typeReference: CjTypeReference?): CjTypeReference? {
        val currentExtendsBound = extendsBound
        if (currentExtendsBound != null) {
            if (typeReference == null) {
                // 移除现有的上界约束
                val colon = findChildByType<PsiElement>(CjTokens.COLON)
                colon?.delete()
                currentExtendsBound.delete()
                return null
            }
            // 替换现有的上界约束
            return currentExtendsBound.replace(typeReference) as CjTypeReference
        }

        if (typeReference != null) {
            // 添加新的上界约束
            val colon = addAfter(CjPsiFactory(project).createColon(), nameIdentifier)
            return addAfter(typeReference, colon) as CjTypeReference
        }

        return null
    }

    /**
     * 类型参数的上界约束（单个）
     *
     * 例如：`class Foo<T: Comparable<T>>` 中的 `Comparable<T>`
     * 对于多个上界约束，只返回第一个
     * 优先从 Stub 索引获取以提升性能
     *
     * @return 上界类型引用，如果没有约束则返回 null
     */
    val extendsBound: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    /**
     * 类型参数的所有上界约束
     *
     * 例如：`class Foo<T> where T: Comparable<T>, T: Serializable` 中的约束列表
     * 优先从 Stub 索引获取以提升性能
     *
     * @return 所有上界类型引用的列表，如果没有约束则返回空列表
     */
    val extendsBounds: List<CjTypeReference>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.TYPE_REFERENCE,
        )



    /**
     * 获取类型参数的使用范围
     *
     * 类型参数的作用域仅限于其所属的泛型声明（类、函数、接口等）内部
     * 例如：
     * - `class Foo<T>` 中的 `T` 作用域为整个类 Foo
     * - `func map<T, R>(...)` 中的 `T` 和 `R` 作用域为函数 map
     *
     * @return 类型参数的搜索范围，限定为包含它的泛型声明范围
     */
    override fun getUseScope(): SearchScope {
        val owner = PsiTreeUtil.getParentOfType(
            this,
            CjTypeParameterListOwner::class.java,
        )
        return LocalSearchScope(owner ?: this)
    }
}
