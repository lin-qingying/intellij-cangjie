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

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.psi.stubs.CangJieBindingPatternStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieBindingPatternStubImpl
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 绑定模式 ElementType
 *
 * 用于创建、序列化和反序列化 CjBindingPattern 的 Stub
 */
class CjBindingPatternElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieBindingPatternStub, CjBindingPattern>(
        debugName,
        CjBindingPattern::class.java,
        CangJieBindingPatternStub::class.java,
    ) {

    override fun createStub(psi: CjBindingPattern, parentStub: StubElement<*>?): CangJieBindingPatternStub {
        val name = psi.name
        // 计算 fqName：顶层变量的绑定模式才有 fqName
        val fqName = computeFqName(psi, name)
        return CangJieBindingPatternStubImpl(
            parentStub,
            StringRef.fromString(name),
            fqName,
        )
    }

    private fun computeFqName(psi: CjBindingPattern, name: String?): FqName? {
        if (name == null) return null
        val variable = psi.variable ?: return null
        if (!variable.isTopLevel) return null
        val file = variable.containingFile as? CjFile ?: return null
        return file.packageFqName.child(org.cangnova.cangjie.name.Name.identifier(name))
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieBindingPatternStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
        val fqName = stub.fqName
        dataStream.writeBoolean(fqName != null)
        if (fqName != null) {
            dataStream.writeName(fqName.asString())
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieBindingPatternStub {
        val name = dataStream.readName()
        val hasFqName = dataStream.readBoolean()
        val fqName = if (hasFqName) {
            val fqNameStr = dataStream.readNameString()
            fqNameStr?.let { FqName(it) }
        } else {
            null
        }
        return CangJieBindingPatternStubImpl(parentStub, name, fqName)
    }

    override fun indexStub(stub: CangJieBindingPatternStub, sink: IndexSink) {
        // TODO: 添加绑定模式索引
    }
}
