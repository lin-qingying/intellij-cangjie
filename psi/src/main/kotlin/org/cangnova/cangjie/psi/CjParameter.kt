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
import org.cangnova.cangjie.psi.stubs.CangJieParameterStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

/**
 * 参数基础接口
 *
 * 提供参数声明的基本能力，包括可调用声明和 let/var 关键字所有者的功能
 */
interface CjParameterBase : CjCallableDeclaration, CjLetVarKeywordOwner {
    /**
     * 检查参数是否具有 let 或 var 关键字
     *
     * @return 如果参数声明包含 let 或 var 关键字则返回 true
     */
    fun hasLetOrVar(): Boolean

    /**
     * 检查参数是否具有默认值
     *
     * @return 如果参数声明包含默认值则返回 true
     */
    fun hasDefaultValue(): Boolean
}

/**
 * 仓颉语言参数 PSI 元素
 *
 * 表示函数、构造函数、lambda 表达式等可调用结构中的参数声明。
 * 支持多种参数类型：
 * - 普通参数：`param: Type`
 * - 可变参数：`vararg param: Type`
 * - 默认参数：`param: Type = defaultValue`
 * - 命名参数：`param!: Type`
 * - 解构参数：`(a, b): Pair<Int, Int>`
 * - Lambda 参数：`{ param -> ... }`
 * - 循环参数：`for (param in collection)`
 * - 异常捕获参数：`catch (e: Exception)`
 *
 * 该类同时支持基于 Stub 的索引和基于 PSI 树的解析，以优化性能。
 */
class CjParameter : CjNamedDeclarationStub<CangJieParameterStub>, CjParameterBase {
    /**
     * 从 AST 节点构造参数
     *
     * @param node AST 节点
     */
    constructor(node: ASTNode) : super(node)

    /**
     * 从 Stub 索引构造参数
     *
     * @param stub 参数的 Stub 索引数据
     */
    constructor(stub: CangJieParameterStub) : super(stub, CjStubElementTypes.VALUE_PARAMETER)

    /**
     * 接受访问者访问
     *
     * @param visitor PSI 访问者
     * @param data 附加数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitParameter(this, data)
    }

    /**
     * 检查参数是否为可变参数（vararg）
     *
     * 例如：`fun foo(vararg items: String)` 中的 `items` 参数
     *
     * @return 如果参数使用 vararg 修饰符则返回 true
     */
    val isVarArg: Boolean get() {
        return modifierList != null && modifierList!!.hasModifier(CjTokens.VARARG_KEYWORD)
    }

    /**
     * 检查参数是否为循环参数
     *
     * 例如：`for (item in items)` 中的 `item` 参数
     *
     * @return 如果参数的父元素是 for 表达式则返回 true
     */
    val isLoopParameter get() = parent is CjForExpression

    /**
     * 参数的类型引用
     *
     * 例如：`param: String` 中的 `String`
     * 优先从 Stub 索引获取以提升性能
     */
    override val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    /**
     * 设置参数的类型引用
     *
     * @param typeRef 新的类型引用，null 表示移除类型引用
     * @return 设置后的类型引用
     */
    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    /**
     * 检查参数是否为函数类型的参数
     *
     * 例如：`fun foo(lambdaArgument: (functionTypeParameter: T, ...) -> R) { ... }`
     * 中的 `functionTypeParameter` 参数
     *
     * @return 如果此参数是函数类型的参数则返回 true
     */
    fun isFunctionTypeParameter(): Boolean {
        return checkParentOfParentType(CjFunctionType::class.java)
    }

    /**
     * 参数类型声明的冒号标记
     *
     * 例如：`param: Type` 中的 `:`
     */
    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    /**
     * 检查参数是否为命名参数
     *
     * 命名参数使用感叹号标记：`param!: Type`
     * 优先从 Stub 索引获取以提升性能
     *
     * @return 如果参数使用命名参数标记则返回 true
     */
    val isNamed: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isNamed()
            }
            return findChildByType<PsiElement>(CjTokens.EXCL) != null
        }

    /**
     * 默认值声明的等号标记
     *
     * 例如：`param: Type = defaultValue` 中的 `=`
     */
    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)

    /**
     * 检查参数是否有默认值
     *
     * 例如：`param: Type = defaultValue`
     * 优先从 Stub 索引获取以提升性能
     *
     * @return 如果参数声明包含默认值则返回 true
     */
    override fun hasDefaultValue(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasDefaultValue()
        }
        return defaultValue != null
    }

    /**
     * 检查参数是否为 Lambda 表达式的参数
     *
     * 例如：
     * ```
     * lambdaConsumer { lambdaParameter ->
     *     ...
     * }
     * ```
     *
     * @return 如果此参数是 lambda 的参数则返回 true
     */
    val isLambdaParameter: Boolean
        get() = checkParentOfParentType(CjFunctionLiteral::class.java)

    /**
     * 参数的默认值表达式
     *
     * 例如：`param: Type = defaultValue` 中的 `defaultValue`
     * 注意：
     * - 优先从 Stub 索引检查是否有默认值
     * - 对于已编译的文件，默认值不可用
     * - 默认值表达式位于等号标记之后
     *
     * @return 默认值表达式，如果没有默认值则返回 null
     */
    val defaultValue: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasDefaultValue()) {
                    return null
                }

                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val equalsToken = equalsToken
            return if (equalsToken != null) {
                PsiTreeUtil.getNextSiblingOfType(
                    equalsToken,
                    CjExpression::class.java,
                )
            } else {
                null
            }
        }

    /**
     * 检查参数是否为可变的（使用 var 声明）
     *
     * 例如：主构造函数参数 `class Foo(var x: Int)` 中的 `x`
     * 优先从 Stub 索引获取以提升性能
     *
     * @return 如果参数使用 var 关键字则返回 true
     */
    val isMutable: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isMutable()
            }

            return findChildByType<PsiElement?>(CjTokens.VAR_KEYWORD) != null
        }

    /**
     * 检查参数是否具有 let 或 var 关键字
     *
     * 通常用于主构造函数参数声明属性
     * 例如：`class Foo(let x: Int, var y: String)`
     * 优先从 Stub 索引获取以提升性能
     *
     * @return 如果参数声明包含 let 或 var 关键字则返回 true
     */
    override fun hasLetOrVar(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasLetOrVar()
        }
        return letOrVarKeyword != null
    }

    /**
     * 参数的 let 或 var 关键字元素
     *
     * 例如：`class Foo(let x: Int)` 中的 `let`
     * 优先从 Stub 索引检查是否存在
     *
     * @return let/const/var 关键字元素，如果没有则返回 null
     */
    override val letOrVarKeyword: PsiElement?
        get() {
            val stub = stub
            if (stub != null && !stub.hasLetOrVar()) {
                return null
            }
            return findChildByType(LET_VAR_TOKEN_SET)
        }

    /**
     * 获取参数在导航树中的展示形式
     *
     * @return 参数的展示信息，由 ItemPresentationProviders 提供
     */
    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    /**
     * 辅助方法：检查父元素的父元素是否为指定类型
     *
     * 用于判断参数所在的上下文，例如是否在 lambda、函数类型或 catch 子句中
     *
     * @param klass 要检查的父父元素类型
     * @return 如果父父元素是指定类型则返回 true
     */
    private fun <T : PsiElement> checkParentOfParentType(klass: Class<T>): Boolean {
        val parent = parent ?: return false
        return klass.isInstance(parent.parent)
    }

    /**
     * 检查参数是否为异常捕获参数
     *
     * 例如：`catch (e: Exception)` 中的 `e` 参数
     *
     * @return 如果参数位于 catch 子句中则返回 true
     */
    val isCatchParameter: Boolean
        get() = checkParentOfParentType(CjCatchClause::class.java)

    // 以下属性为 CjCallableDeclaration 接口要求，但参数本身不具备这些特性
    // 因此统一返回空值或空列表


    /** 参数列表（参数本身不包含参数列表） */
    override val valueParameterList: CjParameterList? = null

    /** 值参数列表（参数本身不包含参数） */
    override val valueParameters: List<CjParameter> = emptyList()


    /** 类型参数列表（参数不支持泛型） */
    override val typeParameterList: CjTypeParameterList? = null

    /** 类型约束列表（参数不支持） */
    override val typeConstraintList: CjTypeConstraintList? = null

    /** 类型约束列表（参数不支持） */
    override val typeConstraints: List<CjTypeConstraint> = emptyList()

    /** 类型参数列表（参数不支持泛型） */
    override val typeParameters: List<CjTypeParameter> = emptyList()

    /**
     * 获取参数所属的函数或声明体
     *
     * 通过参数列表找到包含此参数的函数、构造函数或 lambda 表达式
     * 优先通过 Stub 索引获取以提升性能
     *
     * @return 所属的函数声明，如果不在函数中则返回 null
     */
    val ownerFunction: CjDeclarationWithBody?
        get() {
            val parent = parentByStub as? CjParameterList ?: return null
            return parent.ownerFunction
        }

    /**
     * 获取参数的使用范围
     *
     * 参数的作用域取决于其声明位置：
     * - 普通函数参数：作用域为整个函数体
     * - 主构造函数参数（有 let/var）：作用域为整个类（属性）
     * - 主构造函数参数（无 let/var）：作用域仅限构造函数初始化代码
     * - Lambda 参数：作用域为 lambda 体
     * - 循环参数：作用域为循环体
     * - catch 参数：作用域为 catch 块
     *
     * @return 参数的搜索范围
     */
    override fun getUseScope(): SearchScope {
        var owner: CjExpression? = ownerFunction
        if (owner is CjPrimaryConstructor) {
            // 如果是带 let/var 的主构造函数参数，它是一个属性，使用全局作用域
            if (hasLetOrVar()) return super.getUseScope()
            // 否则作用域仅限于类的初始化代码
            owner = owner.getContainingTypeStatement()
        }
        if (owner == null) {
            owner = PsiTreeUtil.getParentOfType(this, CjExpression::class.java)
        }
        return LocalSearchScope(owner ?: this)
    }

    companion object {
        /**
         * let/var 关键字的 Token 集合
         *
         * 包含 LET_KEYWORD、CONST_KEYWORD 和 VAR_KEYWORD，用于识别参数的可变性声明
         */
        val LET_VAR_TOKEN_SET: TokenSet =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD)
    }
}
