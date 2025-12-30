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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService.Companion.getReferencesFromProviders
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementType
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement

open class CjElementImplStub<T : StubElement<*>> :
    StubBasedPsiElementBase<T>, CjElement, StubBasedPsiElement<T> {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(stub: T, nodeType: IStubElementType<*, *>, node: ASTNode?) : super(stub, nodeType, node)

    constructor(node: ASTNode) : super(node)

    override fun getPsiOrParent(): CjElement {
        return this
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun getReference(): PsiReference? {
        val references = references
        return if ((references.isNotEmpty())) references[0] else null
    }

    override fun getReferences(): Array<PsiReference> {
        if (this is CjBasicType) return emptyArray()


        return getReferencesFromProviders(this)
    }

    override fun getContainingCjFile(): CjFile {
        val file = containingFile
        if (file !is CjFile) {
            var fileString = ""
            if (file.isValid) {
                try {
                    fileString = " " + file.text
                } catch (_: Exception) {
                }
            }

            // getNode() will fail if getContainingFile() returns not PsiFileImpl instance
            val nodeString = (if (file is PsiFileImpl) (" node = $node") else "")

            throw IllegalStateException(
                "CjElement not inside CjFile: " +
                    file + fileString + " of type " + file.javaClass +
                    " for element " + this + " of type " + this.javaClass + nodeString,
            )
        }
        return file
    }

    override fun getLanguage(): Language {
        return CangJieLanguage
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        var child = firstChild
        while (child != null) {
            if (child is CjElement) {
                child.accept(visitor, data)
            }
            child = child.nextSibling
        }
    }

    fun <PsiT : CjElementImplStub<*>, StubT : StubElement<*>> getStubOrPsiChildrenAsList(
        elementType: CjStubElementType<StubT, PsiT>,
    ): List<PsiT> {
        return listOf(*getStubOrPsiChildren(elementType, elementType.arrayFactory))
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCjElement(this, data)
    }
}
