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

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lang.CangJieFileType.Companion.INSTANCE
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.psiUtil.astReplace
import com.linqingying.cangjie.psi.psiUtil.containingTypeStatement
import com.linqingying.cangjie.psi.psiUtil.quoteIfNeeded
import com.linqingying.cangjie.psi.stubs.CangJieStubWithFqName
import com.linqingying.cangjie.utils.OperatorNameConventions.asOperatorName
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls


abstract class CjNamedDeclarationStub<T : CangJieStubWithFqName<*>> : CjDeclarationStub<T>, CjNamedDeclaration {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    override fun getName(): String? {
        return runReadAction {
            val stub = stub
            if (stub != null) {
                return@runReadAction stub.name
            }

            val identifier = nameIdentifier
            if (identifier != null) {

                val text = identifier.text
//                val text = if(identifier is CjOperationName){
//                    identifier.name
//
//                }else{
//                    identifier.text
//                }
                return@runReadAction if (text != null) CjPsiUtil.unquoteIdentifier(text) else null
            }

            return@runReadAction null
        }
    }

    override val nameAsName: Name?
        get() {
            val name = name
            return if (name != null) name.asOperatorName() else null
        }

    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(CjTokens.IDENTIFIER) ?: findChildByType(CjNodeTypes.OPERATION_NAME)
    }

    val operatorName: CjOperationName?
        get() = findChildByType(CjNodeTypes.OPERATION_NAME)


    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String): PsiElement? {
        val identifier = nameIdentifier ?: return null


        val newIdentifier = CjPsiFactory(project).createNameIdentifierIfPossible(name.quoteIfNeeded())
        if (newIdentifier != null) {
            identifier.astReplace(newIdentifier)
        } else {
            identifier.delete()
        }
        return this
    }

    override fun getTextOffset(): Int {
        val identifier = nameIdentifier
        return identifier?.textRange?.startOffset ?: textRange.startOffset
    }

    override fun getUseScope(): SearchScope {
        val enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration(this, false)
        if (enclosingBlock != null) {
//            PsiElement enclosingParent = enclosingBlock.getParent();


//            if (enclosingParent instanceof CjContainerNode) {
//                enclosingParent = enclosingParent.getParent();
//            }


            return LocalSearchScope(enclosingBlock)
        }


        //        PsiElement parent = getParent();
//        PsiElement grandParent = parent != null ? parent.getParent() : null;
        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) {
            val containingClass: CjElement? = PsiTreeUtil.getParentOfType(
                this,
                CjTypeStatement::class.java
            )

            if (containingClass != null) {
                return LocalSearchScope(containingClass)
            }
            val file = getContainingCjFile()
            if (this is CjTypeStatement) {
                val project = getProject()
                val searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(
                    GlobalSearchScope.allScope(project),
                    INSTANCE
                )

                val fileScope: SearchScope = GlobalSearchScope.fileScope(file)

                val scope: SearchScope = GlobalSearchScope.notScope(searchScope)
                return fileScope.union(scope)
            } else {
                return LocalSearchScope(file)
            }
        }

        var scope = super.getUseScope()

        val cjTypeStatement = this.containingTypeStatement
        if (cjTypeStatement != null) {
            scope = scope.intersectWith(cjTypeStatement.useScope)
        }

        return scope
    }

    override val fqName: FqName?
        get() {
            val stub = getStub()
            if (stub != null) {
                return stub.getFqName()
            }
            return CjNamedDeclarationUtil.getFQName(this)
        }
}
