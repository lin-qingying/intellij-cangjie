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
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName.Companion.topLevel
import org.cangnova.cangjie.name.Name.Companion.identifier

import org.cangnova.cangjie.psi.CjImportDirective.Companion.fqNameFromExpression
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

class CjImportDirective : CjDeclarationStub<CangJieImportDirectiveStub> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieImportDirectiveStub) : super(
        stub,
        CjStubElementTypes.IMPORT_DIRECTIVE,
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitImportDirective(this, data)
    }

    /**
     * 获取所有导入项
     *
     * 新实现：直接获取 IMPORT_ITEM 类型的子元素
     */
    val importItems: List<CjImportItem>
        get() = findChildrenByClass(CjImportItem::class.java).toList()




    fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

    /**
     * 获取导入的引用表达式（用于同包多项导入的基础路径）
     * 例如: import a.b.{c, d} -> 返回 a.b
     */
    @get:IfNotParsed
    val importedReference: CjExpression?
        get() {
            val references =
                getStubOrPsiChildren(
                    CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS,
                    CjExpression.ARRAY_FACTORY,
                )
            if (references.isNotEmpty()) {
                return references[0]
            }
            return null
        }

    override fun hasModifier(modifier: CjKeywordToken): Boolean {
        return getModifier(modifier) != null
    }

    companion object {
        fun fqNameFromExpression(expression: CjExpression?): FqName? {
            if (expression == null) {
                return null
            }

            when (expression) {
                is CjDotQualifiedExpression -> {
                    val parentFqn = fqNameFromExpression(expression.receiverExpression)
                    val child =
                        nameFromExpression(expression.selectorExpression) ?: return parentFqn
                    if (parentFqn != null) {
                        return parentFqn.child(child)
                    }
                    return null
                }

                is CjSynthesisQualifiedExpression -> {
                    return fqNameFromExpression(expression.selectorExpression)?.let {
                        fqNameFromExpression(expression.receiverExpression)?.child(
                            it,
                        )
                    }
                }

                is CjSimpleNameExpression -> {
                    return topLevel(expression.referencedNameAsName)
                }

                else -> {
                    throw IllegalArgumentException("Can't construct fqn for: " + expression.javaClass)
                }
            }
        }

        private fun nameFromExpression(expression: CjExpression?): Name? {
            if (expression == null) {
                return null
            }

            if (expression is CjSimpleNameExpression) {
                return expression.referencedNameAsName
            } else {
                throw IllegalArgumentException("Can't construct name for: " + expression.javaClass)
            }
        }
    }
}


/**
 * 新的导入项类（轻量级，不使用 Stub）
 *
 * 用于表示单个导入项，例如:
 * - import a.b       -> 一个 CjImportItem
 * - import {a.b, c.d} -> 两个 CjImportItem
 * - import a.{b, c}  -> 两个 CjImportItem
 */
class CjImportItem(node: ASTNode) : CjElementImpl(node), CjImportInfo {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitImportItem(this, data)
    }

    @Volatile
    private var _importedFqName: FqName? = null

    override fun subtreeChanged() {
        super.subtreeChanged()
        _importedFqName = null
    }

    /**
     * 获取导入的引用表达式
     */
    val importedReference: CjExpression?
        get() = findChildByType(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS)

    /**
     * 获取导入的完全限定名
     *
     * 对于同包多项导入 (import a.{b, c})，需要组合基础路径和项路径
     */
    override val importedFqName: FqName?
        get() {
            _importedFqName?.let { return it }

            val reference = importedReference
            val fqName = if (reference != null) {
                // 项自身有表达式，直接从表达式构建 FqName
                CjImportDirective.fqNameFromExpression(reference)
            } else {
                // 项没有表达式，可能是通配符导入
                null
            }

            // 检查父级 ImportDirective 是否有基础路径
            val directive = getStrictParentOfType<CjImportDirective>()
            val basePath = directive?.importedReference?.let {
                CjImportDirective.fqNameFromExpression(it)
            }

            // 组合基础路径和项路径
            val finalFqName = when {
                basePath != null && fqName != null -> {
                    // 有基础路径，组合: a + b.C = a.b.C
                    basePath.child(fqName)
                }
                fqName != null -> {
                    // 只有项路径，直接使用
                    fqName
                }
                basePath != null -> {
                    // 只有基础路径（通配符导入: a.*）
                    basePath
                }
                else -> null
            }

            _importedFqName = finalFqName
            return finalFqName
        }

    /**
     * 是否为通配符导入 (a.b.*)
     */
    override val isAllUnder: Boolean
        get() = node.findChildByType(CjTokens.MUL) != null

    /**
     * 获取别名
     */
    val alias: CjImportAlias?
        get() = findChildByClass(CjImportAlias::class.java)

    /**
     * 获取别名名称
     */
    override val aliasName: String?
        get() = alias?.name

    /**
     * 导入内容
     */
    override val importContent: CjImportInfo.ImportContent?
        get() {
            val reference = importedReference ?: return null
            return CjImportInfo.ImportContent.ExpressionBased(reference)
        }

    /**
     * 导入的名称
     */
    override val importedName: Name?
        get() = importedFqName?.shortName()

    /**
     * 是否为有效导入（无语法错误）
     */
    val isValidImport: Boolean
        get() = !PsiTreeUtil.hasErrorElements(this)

    /**
     * 获取父导入指令
     */
    val importDirective: CjImportDirective
        get() = getStrictParentOfType<CjImportDirective>()
            ?: throw IllegalStateException("CjImportItem must have a parent CjImportDirective")

    /**
     * 转换为 ImportPath（兼容性方法）
     */
    val importPath: ImportPath?
        get() {
            val importFqn = importedFqName ?: return null
            val identifier = if (aliasName != null) {
                identifier(aliasName!!)
            } else {
                null
            }

            return ImportPath(importFqn, isAllUnder, identifier)
        }
}



data class CangJieImportField(
    override val isAllUnder: Boolean,
    override val importContent: CjImportInfo.ImportContent?,
    override val importedFqName: FqName?,
    override val aliasName: String?
) : CjImportInfo

