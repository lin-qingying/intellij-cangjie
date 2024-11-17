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

package com.linqingying.cangjie.psi

import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.FqName.Companion.topLevel
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.Name.Companion.identifier
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.elements.CjTokenSets
import com.linqingying.cangjie.resolve.ImportPath
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.concurrent.Volatile

class CjImportDirective : CjDeclarationStub<CangJieImportDirectiveStub>, CjImportInfo {
    @Volatile
    private var _importedFqName: FqName? = null

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieImportDirectiveStub) : super(stub, CjStubElementTypes.IMPORT_DIRECTIVE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitImportDirective(this, data)
    }

    @get:IfNotParsed
    val importedReference: CjExpression?
        get() {
            val references =
                getStubOrPsiChildren(
                    CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS,
                    CjExpression.ARRAY_FACTORY
                )
            if (references.isNotEmpty()) {
                return references[0]
            }
            return null
        }

    val alias: CjImportAlias?
        get() = getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS)

    override val aliasName: String?
        get() {
            val alias = alias
            return alias?.name
        }

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
            val parentStub = this.parent

            val stub = stub
            if (stub != null) {
                return stub.getImportedFqName()
            }

            var importedFqName = this._importedFqName
            if (importedFqName != null) return importedFqName
            val importedReference = importedReference ?: return null
            // in case it's not parsed


            importedFqName = fqNameFromExpression(importedReference)

            if (parentStub is CjMultiImportDirective) {
                val name = parentStub.fqName
                if (name != null) {
                    importedFqName = name.child(importedFqName!!)
                }
            }

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

    override fun subtreeChanged() {
        super.subtreeChanged()
        _importedFqName = null
    }

    fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

    override fun hasModifier(modifier: CjKeywordToken): Boolean {
//        CangJieImportDirectiveStub stub = getStub();
//        if (stub != null) {
//            return stub.getModifierVisibility(tokenType);
//        }
        return getModifier(modifier) != null
    }

    override val modifierVisibility : DescriptorVisibility get()    {
        val stub = stub
        if (stub != null) {
            return stub.getModifierVisibility()
        }

        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE
        if (hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL
        if (hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED
        if (hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC
        return DescriptorVisibilities.PRIVATE
        //        return resolveVisibilityFromModifiers(this, DescriptorVisibilities.PRIVATE);
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

                is CjSimpleNameExpression -> {
                    return topLevel(expression.getReferencedNameAsName())
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
                return expression.getReferencedNameAsName()
            } else {
                throw IllegalArgumentException("Can't construct name for: " + expression.javaClass)
            }
        }
    }
}
