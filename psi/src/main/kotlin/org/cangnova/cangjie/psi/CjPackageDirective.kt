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
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName.Companion.fromString
import org.cangnova.cangjie.name.Name.Companion.identifier
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.psi.stubs.CangJiePackageDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import java.util.*

class CjPackageDirective : CjDeclarationStub<CangJiePackageDirectiveStub> {
    private var qualifiedNameCache: String? = null

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePackageDirectiveStub) : super(stub, CjStubElementTypes.PACKAGE_DIRECTIVE)

    override fun toString(): String {
        return super.toString()
    }

    val packageNameExpression
        get() = CjStubbedPsiUtil.getStubOrPsiChild(
            this,
            CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS,
            CjExpression.ARRAY_FACTORY,
        )


    val packageNames: List<CjSimpleNameExpression>
        get() {
            var nameExpression = this.packageNameExpression ?: return mutableListOf<CjSimpleNameExpression>()

            val packageNames: MutableList<CjSimpleNameExpression> =
                ArrayList<CjSimpleNameExpression>()
            while (nameExpression is CjQualifiedExpression) {
                val selector = nameExpression.selectorExpression
                if (selector is CjSimpleNameExpression) {
                    packageNames.add(selector)
                }

                nameExpression = nameExpression.receiverExpression
            }

            if (nameExpression is CjSimpleNameExpression) {
                packageNames.add(nameExpression)
            }

            packageNames.reverse()

            return packageNames
        }

    val lastReferenceExpression: CjSimpleNameExpression?
        get() {
            val nameExpression = this.packageNameExpression ?: return null

            return nameExpression.getQualifiedElementSelector() as CjSimpleNameExpression?
        }

    val nameIdentifier: PsiElement?
        get() {
            val lastPart = this.lastReferenceExpression
            return lastPart?.identifier
        }

    override fun getName(): String {
        val nameIdentifier = this.nameIdentifier
        return if (nameIdentifier == null) "" else nameIdentifier.text
    }

    override fun navigate(requestFocus: Boolean) {
        super.navigate(requestFocus)
    }

    override fun canNavigateToSource(): Boolean {
        return super.canNavigateToSource()
    }

    override fun canNavigate(): Boolean {
        return super.canNavigate()
    }

    private fun getModifier(tokenType: CjKeywordToken): PsiElement? {
        return findChildByType(tokenType)
    }

    fun hasModifier(tokenType: CjModifierKeywordToken): Boolean {
//        CangJieImportDirectiveItemStub stub = getStub();
//        if (stub != null) {
//            return stub.getModifierVisibility(tokenType);
//        }
        return getModifier(tokenType) != null
    }

    val isMacroPackage: Boolean
        get() = //        try {

            findChildByType<PsiElement>(CjTokens.MACRO_KEYWORD) != null
    //        } catch (Exception e) {
//            return false;
//        }

    val nameAsName: Name
        get() {
            val nameIdentifier = this.nameIdentifier
            return if (nameIdentifier == null) {
                SpecialNames.ROOT_PACKAGE
            } else {
                identifier(
                    nameIdentifier.text,
                )
            }
        }

    val isRoot: Boolean
        get() = getName().isEmpty()

    var fqName: FqName = FqName.ROOT
        get() {
            val qualifiedName = this.qualifiedName
            return if (qualifiedName.isEmpty()) {
                FqName.ROOT
            } else {
                fromString(
                    qualifiedName,
                )
            }
        }
        set(fqName) {
            if (fqName.isRoot) {
                if (!field.isRoot) {
                    replace(
                        Objects.requireNonNull<CjPackageDirective>(
                            CjPsiFactory(project).createFile(
                                "",
                            ).packageDirective,
                        ),
                    )
                }
                return
            }

            val psiFactory = CjPsiFactory(project)
            val newExpression: PsiElement = psiFactory.createExpression(fqName.asString())
            val currentExpression = this.packageNameExpression
            if (currentExpression != null) {
                currentExpression.replace(newExpression)
                return
            }

            val keyword = this.packageKeyword
            if (keyword != null) {
                addAfter(newExpression, keyword)
                addAfter(psiFactory.createWhiteSpace(), keyword)
                return
            }

            replace(psiFactory.createPackageDirective(fqName))
        }

    fun getFqName(nameExpression: CjSimpleNameExpression?): FqName {
        return FqName(getQualifiedNameOf(nameExpression))
    }

    val qualifiedName: String
        get() {
            if (qualifiedNameCache == null) {
                qualifiedNameCache = getQualifiedNameOf(null)
            }

            return qualifiedNameCache!!
        }

    private fun getQualifiedNameOf(nameExpression: CjSimpleNameExpression?): String {
        val builder = StringBuilder()
        for (e in this.packageNames) {
            if (builder.isNotEmpty()) {
                builder.append(".")
            }
            builder.append(e.referencedName)

            if (e === nameExpression) break
        }
        return builder.toString()
    }

    val packageKeyword
        get() = findChildByType<PsiElement>(CjTokens.PACKAGE_KEYWORD)

    override fun subtreeChanged() {
        qualifiedNameCache = null
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPackageDirective(this, data)
    }
}
