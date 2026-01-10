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

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * 局部命名声明接口
 *
 * 表示定义在函数、代码块等局部作用域内的命名声明,例如局部变量、局部函数等。
 * 局部声明不具有完全限定名(Fully Qualified Name),因此不继承 [CjNamedDeclaration]。
 *
 * @see CjNamedDeclaration 顶层命名声明
 */
interface CjLocalNamedDeclaration : CjDeclaration

/**
 * 仓颉语言中命名声明的基础接口
 *
 * 命名声明是具有标识符名称的声明,可以被引用和查找。
 * 该接口整合了多个相关接口:
 * - [CjDeclaration]: 声明基础接口
 * - [PsiNameIdentifierOwner]: IntelliJ 平台的命名标识符持有者接口,支持重命名等操作
 * - [CjStatementExpression]: 语句表达式接口
 * - [CjNamed]: 具有名称的元素接口
 *
 * 实现该接口的典型 PSI 元素包括:
 * - 类声明 (CjClass)
 * - 函数声明 (CjFunction)
 * - 属性声明 (CjProperty)
 * - 类型别名声明 (CjTypeAlias)
 *
 * @see CjLocalNamedDeclaration 局部命名声明
 * @see PsiNameIdentifierOwner IntelliJ 平台命名元素接口
 */
interface CjNamedDeclaration :
    CjDeclaration,
    PsiNameIdentifierOwner,
    CjStatementExpression,
    CjNamed {
    /**
     * 获取安全的名称对象
     *
     * 与 [nameAsName] 不同,该属性保证返回非空的 [Name] 对象。
     * 如果声明没有名称,会返回一个特殊的占位符名称。
     *
     * @return 名称对象(非 null)
     */
    val nameAsSafeName: Name

    /**
     * 获取完全限定名 (Fully Qualified Name)
     *
     * 完全限定名包含了从根包到该声明的完整路径,例如 `std.collection.ArrayList`。
     * 局部声明(如函数内的变量)没有完全限定名,会返回 null。
     *
     * @return 完全限定名,如果不存在则返回 null
     */
    val fqName: FqName?
}
