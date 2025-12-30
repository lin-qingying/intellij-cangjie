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

package org.cangnova.cangjie.resolve.qualified

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeArgumentList
import org.cangnova.cangjie.psi.CjTypeProjection

/**
 * 限定符部分基类
 *
 * 表示限定名称路径中的一个组成部分，包含名称、类型参数和查找位置信息。
 * 子类 `ExpressionQualifierPart` 还会关联具体的 PSI 表达式节点。
 *
 * 用途：
 * - 在解析限定表达式（如 `a.b.c`）时，每个部分（`a`, `b`, `c`）都表示为一个 QualifierPart
 * - 支持泛型参数（如 `Foo<T>` 中的 `<T>`）
 * - 提供查找位置信息用于缓存和性能优化
 *
 * @param name 此部分的名称
 * @param typeArguments 此部分的类型参数列表（如果有）
 * @param location 查找位置，用于缓存和调试
 */
open class QualifierPart(
    val name: Name,
    val typeArguments: CjTypeArgumentList? = null,
    val location: LookupLocation = NoLookupLocation.FOR_DEFAULT_IMPORTS
) {
    /**
     * 关联的 PSI 表达式节点
     *
     * 基类返回 null，子类 `ExpressionQualifierPart` 会覆盖此属性返回实际的表达式。
     */
    open val expression: CjSimpleNameExpression? get() = null

    /** 解构第一个组件：名称 */
    operator fun component1() = name

    /** 解构第二个组件：表达式 */
    open operator fun component2() = expression

    /** 解构第三个组件：类型参数 */
    operator fun component3() = typeArguments
}

/**
 * 表达式限定符部分
 *
 * 表示限定表达式中的一个组成部分，关联了具体的 PSI 表达式节点。
 * 例如在 `a.b<T>.c` 中，`a`、`b<T>`、`c` 各为一个 `ExpressionQualifierPart`。
 *
 * 用途：
 * - 在解析限定表达式时，将 PSI 节点转换为解析器可处理的结构
 * - 支持错误报告（可以精确定位到源码位置）
 * - 支持 IDE 功能（如导航、重构）
 *
 * @param name 名称
 * @param expression PSI 表达式节点
 * @param typeArguments 类型参数列表
 */
class ExpressionQualifierPart(
    name: Name,
    override val expression: CjSimpleNameExpression,
    typeArguments: CjTypeArgumentList? = null
) : QualifierPart(name, typeArguments, CangJieLookupLocation(expression)) {

    /**
     * 便利构造器：从表达式自动提取名称
     *
     * @param expression PSI 表达式节点
     */
    constructor(expression: CjSimpleNameExpression) : this(expression.referencedNameAsName, expression)

    override fun component2() = expression
}

/**
 * 类型限定符解析结果
 *
 * 保存类型解析过程中的中间结果，包括解析路径和最终的类型描述符。
 *
 * 使用场景：
 * - 解析用户类型引用（如 `com.example.Foo<T>`）
 * - 将 PSI 中的类型节点解析为类型系统的描述符
 * - 提取泛型参数用于类型检查
 *
 * @param qualifierParts 解析路径中的各个部分（如 `a.b.C` 会被拆分为三个部分）
 * @param classifierDescriptor 最终解析到的类型描述符（类、接口、类型别名等），解析失败时为 null
 */
data class TypeQualifierResolutionResult(
    val qualifierParts: List<ExpressionQualifierPart>,
    val classifierDescriptor: ClassifierDescriptor? = null
) {
    /**
     * 获取所有类型投影（泛型参数）
     *
     * 扁平化展开路径中所有部分的类型参数。
     * 例如：`a.B<T>.C<U>` 会提取出 `[T, U]`。
     */
    val allProjections: List<CjTypeProjection>
        get() = qualifierParts.flatMap { it.typeArguments?.arguments.orEmpty() }
}

/**
 * 限定表达式解析结果
 *
 * 用于确定限定表达式中哪部分是限定符（类或包），哪部分是成员名。
 *
 * 使用场景：
 * - 解析调用表达式（如 `a.b.c.foo()`），确定 `a.b.c` 是限定符，`foo` 是成员
 * - 区分包访问和成员访问
 * - 为后续的成员解析提供上下文
 *
 * 示例：
 * ```
 * a.b.c.foo  -> QualifiedExpressionResolveResult(
 *                   classOrPackage = 包 a.b.c,
 *                   memberName = foo
 *               )
 * ```
 *
 * @param classOrPackage 解析到的类或包描述符
 * @param memberName 剩余的成员名（如果有）
 */
data class QualifiedExpressionResolveResult(
    val classOrPackage: DeclarationDescriptor?,
    val memberName: Name?
) {
    companion object {
        /** 表示解析失败的单例常量 */
        val UNRESOLVED = QualifiedExpressionResolveResult(null, null)
    }
}
