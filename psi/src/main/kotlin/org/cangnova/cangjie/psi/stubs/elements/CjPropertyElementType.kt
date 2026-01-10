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

package org.cangnova.cangjie.psi.stubs.elements
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJiePropertyStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.psi.psiUtil.isExtensionDeclaration

class CjPropertyElementType(debugName: String) : CjStubElementType<CangJiePropertyStub, CjProperty>(
    debugName,
    CjProperty::class.java,
    CangJiePropertyStub::class.java,
) {
    override fun serialize(stub: CangJiePropertyStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.getFqName()?.asString())
        dataStream.writeBoolean(stub.isExtension())

        dataStream.writeBoolean(stub.hasReturnTypeRef())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJiePropertyStub {
        val name = dataStream.readName()
        val fqNameAsString = dataStream.readName()
        val fqName: FqName? = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null
        val hasReceiverTypeRef = dataStream.readBoolean()

        val hasReturnTypeRef = dataStream.readBoolean()

        return CangJiePropertyStubImpl(
            parentStub,
            name,
            fqName,
            hasReceiverTypeRef,
            hasReturnTypeRef,
        )
    }

    override fun createStub(psi: CjProperty, parentStub: StubElement<out PsiElement>?): CangJiePropertyStub {
        return CangJiePropertyStubImpl(
            parentStub,
            StringRef.fromString(psi.name),
            psi.safeFqNameForLazyResolve(),
            psi.isExtensionDeclaration()   ,
            psi.typeReference != null,
        )
    }

    override fun indexStub(stub: CangJiePropertyStub, sink: IndexSink) {
        getInstance().indexProperty(stub, sink)
    }

    override fun createPsi(stub: CangJiePropertyStub): CjProperty {
        return CjProperty(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjProperty {
        return CjProperty(node)
    }
}
