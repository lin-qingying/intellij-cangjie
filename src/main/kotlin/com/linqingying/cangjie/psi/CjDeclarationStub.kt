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

import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.psi.psiUtil.findDocComment
import com.linqingying.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.concurrent.atomic.AtomicLong

abstract class CjDeclarationStub<T : StubElement<*>> : CjModifierListOwnerStub<T>, CjDeclaration {
    private val modificationStamp: AtomicLong = AtomicLong()

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(
            this,
            CjExpression::class.java
        )

    override fun subtreeChanged() {
        super.subtreeChanged()
        modificationStamp.getAndIncrement()
    }

    fun getModificationStamp(): Long {
        return modificationStamp.get()
    }

    override val docComment: CDoc?
        get() {
            return findDocComment(this)
        }

    override fun getParent(): PsiElement? {
        val stub = stub
        // we build stubs for local classes/objects too but they have wrong parent
        if (stub != null && !(stub is CangJieTypeStatementStub<*> && (stub as CangJieTypeStatementStub<*>).isLocal())) {
            return stub.parentStub.psi
        }
        return super.getParent()
    }


    override fun getOriginalElement(): PsiElement {
        val navigationPolicy: CangJieDeclarationNavigationPolicy? = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java
        )
        return navigationPolicy?.getOriginalElement(this) ?: this
    }

    override fun getNavigationElement(): PsiElement {
        val navigationPolicy: CangJieDeclarationNavigationPolicy? = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java
        )
        return navigationPolicy?.getNavigationElement(this) ?: this
    }
}
