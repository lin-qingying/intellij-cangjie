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

import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.psiUtil.StubUtils
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieTypeAliasStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjTypeAliasElementType(debugName: String) :
    CjStubElementType<CangJieTypeAliasStub, CjTypeAlias>(
        debugName,
        CjTypeAlias::class.java,
        CangJieTypeAliasStub::class.java,
    ) {
    override fun serialize(stub: CangJieTypeAliasStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.getFqName()?.asString())
        StubUtils.serializeClassId(dataStream, stub.getClassId())
    }

    override fun indexStub(stub: CangJieTypeAliasStub, sink: IndexSink) {
        StubIndexService.getInstance().indexTypeAlias(stub, sink)
    }
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieTypeAliasStub {
        val name = dataStream.readName()
        val fqName = dataStream.readName()
        val classId = StubUtils.deserializeClassId(dataStream)

        return CangJieTypeAliasStubImpl(parentStub, name, fqName, classId)
    }

    override fun createStub(psi: CjTypeAlias, parentStub: StubElement<*>): CangJieTypeAliasStub {
        val name = StringRef.fromString(psi.name)
        val fqName = StringRef.fromString(psi.safeFqNameForLazyResolve()?.asString())
        val classId = StubUtils.createNestedClassId(parentStub, psi)

        return CangJieTypeAliasStubImpl(parentStub, name, fqName, classId)
    }
}
