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
 * 注解容器 (Annotation Container)
 *
 * 表示修饰符列表中的注解分组容器,可以包含一个或多个连续的注解条目。
 * 这是一个内部组织结构,用于在 PSI 树中对多个注解进行分组管理。
 *
 * ## 语法结构
 * ```
 * annotation ::= annotationEntry+
 * ```
 *
 * ## 与 CjAnnotation 的关系
 * - **CjAnnotations**: 注解容器,是 PSI 树中的组织节点,可包含多个注解条目
 * - **CjAnnotation**: 单个注解使用,是实际的注解应用
 *
 * ## PSI 树结构示例
 * ```
 * CjModifierList
 *   └─ CjAnnotations (容器)
 *       ├─ CjAnnotation (@Deprecated)
 *       ├─ CjAnnotation (@Frozen)
 *       └─ CjAnnotation (@ConstSafe)
 * ```
 *
 * 对应的源代码:
 * ```cangjie
 * @Deprecated
 * @Frozen
 * @ConstSafe
 * func oldMethod() {}
 * ```
 *
 * ## 主要用途
 * 1. **分组管理**: 将连续的注解条目组织在一起
 * 2. **统一操作**: 提供对多个注解的批量操作接口
 * 3. **删除管理**: [removeEntry] 方法处理注解删除的边界情况
 *    - 如果容器中有多个注解条目,只删除指定的条目
 *    - 如果容器中只有一个注解条目,删除整个容器
 *
 * ## 主要属性和方法
 * - [entries]: 获取容器中所有的注解条目列表
 * - [removeEntry]: 删除指定的注解条目,处理边界情况
 *
 * ## 使用注意
 * 在大多数情况下,直接使用 [CjAnnotated.annotationEntries] 访问注解即可,
 * 无需直接操作 CjAnnotations 容器。容器主要用于内部 PSI 树管理。
 *
 * @see CjAnnotation 单个注解条目
 * @see CjAnnotated 可被注解修饰的元素接口
 * @see CjModifierList 修饰符列表
 * @see CjAnnotationsContainer 注解容器接口
 */
class CjAnnotations : CjElementImplStub<CangJiePlaceHolderStub<CjAnnotations>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjAnnotations>) : super(stub, CjStubElementTypes.ANNOTATION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitAnnotation(this, data)
    }

    val entries: List<CjAnnotation>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.ANNOTATION)

    fun removeEntry(entry: CjAnnotation) {
        if (entries.size > 1) {
            entry.delete()
        } else {
            delete()
        }
    }
}
