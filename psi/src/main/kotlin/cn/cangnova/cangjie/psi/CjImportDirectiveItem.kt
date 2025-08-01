/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.psi
import cn.cangnova.cangjie.name.*

import cn.cangnova.cangjie.lexer.CjKeywordToken
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.*
import cn.cangnova.cangjie.name.FqName.Companion.topLevel
import cn.cangnova.cangjie.name.Name.Companion.identifier

import cn.cangnova.cangjie.psi.CjImportDirective.Companion.fqNameFromExpression
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import cn.cangnova.cangjie.psi.stubs.CangJieImportDirectiveItemStub
import cn.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import cn.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

class CjImportDirective : CjDeclarationStub<CangJieImportDirectiveStub> {
    @Deprecated("")
    constructor(node: ASTNode) : super(node)

    @Deprecated("")
    constructor(stub: CangJieImportDirectiveStub) : super(
        stub,
        CjStubElementTypes.IMPORT_DIRECTIVE,
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitImportDirective(this, data)
    }

    /**
     * 对于  {a.b,b.b} 形式
     */
    val items: List<CjImportDirectiveItem>
        get() {
            val items = getStubOrPsiChildren(
                CjStubElementTypes.IMPORT_DIRECTIVE_ITEM,
                CjImportDirectiveItem.ARRAY_FACTORY,
            ).mapNotNull {
                it
            }

            val a = items.flatMap {
                it.items.ifEmpty {
                    listOf(it)
                }
            }

            return a
        }

    fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

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

@Deprecated("Use CjImportDirective")
class CjMultiImportDirective(node: ASTNode) : CjElementImpl(node)

// import
//       item*
//           item*
class CjImportDirectiveItem : CjDeclarationStub<CangJieImportDirectiveItemStub>, CjImportInfo {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitImportDirectiveItem(this, data)
    }

    override fun subtreeChanged() {
        super.subtreeChanged()
        _importedFqName = null
    }

    //    是否为多导入但是并非 *
    private val isMultiUnder: Boolean
        get() {
            //    判断父节点是否为 CjImportDirective
            return parent is CjImportDirective && parentImportedReference != null
        }

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieImportDirectiveItemStub) : super(stub, CjStubElementTypes.IMPORT_DIRECTIVE_ITEM)

    companion object {
        @JvmStatic
        val EMPTY_ARRAY = arrayOf<CjImportDirectiveItem>()

        @JvmStatic
        val ARRAY_FACTORY =
            ArrayFactory { count: Int ->
                if (count == 0) EMPTY_ARRAY else arrayOfNulls<CjImportDirectiveItem>(count)
            }
    }

    /**
     * 对于  a.{a,b} 形式
     */
    val items: List<CjImportDirectiveItem> =
        getStubOrPsiChildren(CjStubElementTypes.IMPORT_DIRECTIVE_ITEM, ARRAY_FACTORY)
            .mapNotNull { it }

    private val parentImportedReference: CjExpression?
        get() {
            return (parent as? CjImportDirective)?.importedReference
        }

    @get:IfNotParsed
    val importedReference: CjExpression?
        get() {
            val references =
                getStubOrPsiChildren(
                    CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS,
                    CjExpression.ARRAY_FACTORY,
                ).firstOrNull()

            if (isMultiUnder) {
                return CjSynthesisQualifiedExpression(
                    parentImportedReference!!,
                    references,
                )
            }

            return references
        }

    val alias: CjImportAlias?
        get() = getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS)

    override val aliasName: String?
        get() {
            val alias = alias
            return alias?.name
        }

    val importDirective: CjImportDirective
        get() = getStrictParentOfType<CjImportDirective>() ?: throw IllegalStateException()

    @Volatile
    private var _importedFqName: FqName? = null

    override val isAllUnder: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isAllUnder()
            }
            return node.findChildByType(CjTokens.MUL) != null
        }

    override val importContent: CjImportInfo.ImportContent?
        get() {
            val reference = importedReference ?: return null
            return CjImportInfo.ImportContent.ExpressionBased(reference)
        }

    @get:IfNotParsed
    override val importedFqName: FqName?
        get() {

            val stub = stub
            if (stub != null) {
                return stub.getImportedFqName()
            }

            var importedFqName = this._importedFqName
            if (importedFqName != null) return importedFqName
            val importedReference = importedReference ?: return null

            importedFqName =
                fqNameFromExpression(importedReference)

            this._importedFqName = importedFqName
            return importedFqName
        }
    @get:IfNotParsed
    val importPath: ImportPath?
        get() {
            val importFqn = _importedFqName ?: return null

            var alias: Name? = null
            val aliasName = aliasName
            if (aliasName != null) {
                alias = identifier(aliasName)
            }

            return ImportPath(importFqn, isAllUnder, alias)
        }
    val isValidImport: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isValid()
            }
            return !PsiTreeUtil.hasErrorElements(this)
        }
}
